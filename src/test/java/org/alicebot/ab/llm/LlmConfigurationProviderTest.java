package org.alicebot.ab.llm;

import com.sun.net.httpserver.HttpServer;
import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.AlicebotContext;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Category;
import org.alicebot.ab.Chat;
import org.alicebot.ab.exception.InternalServerException;
import org.alicebot.ab.model.Param;
import org.alicebot.ab.model.Report;
import org.alicebot.ab.model.block.AliceBotLlmModelMapper;
import org.alicebot.ab.model.block.LlmType;
import org.alicebot.ab.utils.AlicebotContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class LlmConfigurationProviderTest {

    @AfterEach
    public void tearDown() {
        AlicebotContext.setProvider(null);
    }

    @Test
    public void testLlmProviderConfigConstructorsAndImmutability() {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("region", "eu-west-1");

        LlmProviderConfig config = new LlmProviderConfig("https://api.example.com", "secret", 20, mutable);
        mutable.put("region", "mutated");

        assertEquals("https://api.example.com", config.apiUrl());
        assertEquals("secret", config.apiKey());
        assertEquals(20, config.maxHistory());
        assertEquals("eu-west-1", config.getExtraProperty("region"));
        assertEquals("fallback", config.getExtraProperty("missing", "fallback"));
        assertThrows(UnsupportedOperationException.class, () -> config.extraProperties().put("k", "v"));

        LlmProviderConfig ollamaConfig = new LlmProviderConfig("http://localhost:11434/api/chat", 10);
        assertNull(ollamaConfig.apiKey());
        assertTrue(ollamaConfig.extraProperties().isEmpty());
    }

    @Test
    public void testLlmServiceValidationRejectsMissingConfig() {
        assertThrows(InternalServerException.class, () -> LLMService.chatGpt("{}", null, null));
        assertThrows(InternalServerException.class, () -> LLMService.chatGpt("{}", new LlmProviderConfig("https://api.openai.com", null, 15), null));
        assertThrows(InternalServerException.class, () -> LLMService.chatGemini("{}", new LlmProviderConfig(null, "key", 15), "gemini-2.0-flash", null));
        assertThrows(InternalServerException.class, () -> LLMService.chatOllama("{}", new LlmProviderConfig("   ", 15), null));
    }

    @Test
    public void testDynamicConfigReloadWithoutUpdateMethod() throws Exception {
        AtomicReference<String> receivedAuthHeader = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello from mock GPT\"}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
            AtomicReference<LlmProviderConfig> currentConfig = new AtomicReference<>(
                    new LlmProviderConfig(url, "token-v1", 12)
            );

            AlicebotContext.setProvider(new StubContextProvider() {
                @Override
                public LlmProviderConfig getLlmProviderConfig(LlmType llmType, String botName, String symbol) {
                    assertEquals(LlmType.GPT, llmType);
                    assertEquals("testBot", botName);
                    assertEquals("sprint", symbol);
                    return currentConfig.get();
                }

                @Override
                public AliceBotLlmModelMapper getLlmModelMapper(String modelLabel, String symbol) {
                    if ("gpt-fast".equals(modelLabel) && "sprint".equals(symbol)) {
                        return new AliceBotLlmModelMapper(1L, LlmType.GPT, "gpt-4o-mini", "gpt-fast", "desc", "sprint");
                    }
                    return null;
                }
            });

            Bot bot = new Bot("testBot", System.getProperty("java.io.tmpdir"), "none");
            bot.brain.addCategory(new Category(
                    0,
                    "ASK *",
                    "*",
                    "*",
                    "<think><set name=\"model\">gpt-fast</set><set name=\"user\"><star/></set></think><llm model=\"model\" user=\"user\"/>",
                    "test.aiml"
            ));

            Chat chat = new Chat(bot, "session-1", "CHAT", "sprint");
            String response1 = AIMLProcessor.respond("ASK hi", "that", "*", chat);
            assertEquals("Hello from mock GPT", response1.trim());
            assertEquals("Bearer token-v1", receivedAuthHeader.get());
            assertEquals(12, chat.maxHistory);

            // Simulate configuration change in SprintBot without calling any updateLLMConfiguration method
            currentConfig.set(new LlmProviderConfig(url, "token-v2", 25));

            String response2 = AIMLProcessor.respond("ASK hello again", "that", "*", chat);
            assertEquals("Hello from mock GPT", response2.trim());
            assertEquals("Bearer token-v2", receivedAuthHeader.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testAimlProcessorReturnsErrorWhenProviderNotSet() {
        AlicebotContext.setProvider(null);
        Bot bot = new Bot("testBot", System.getProperty("java.io.tmpdir"), "none");
        bot.brain.addCategory(new Category(
                0,
                "ASK *",
                "*",
                "*",
                "<think><set name=\"model\">gpt-fast</set><set name=\"user\"><star/></set></think><llm model=\"model\" user=\"user\"/>",
                "test.aiml"
        ));

        Chat chat = new Chat(bot, "session-2", "CHAT", "sprint");
        String response = AIMLProcessor.respond("ASK hi", "that", "*", chat);
        assertTrue(response.contains("ERR|AlicebotContextProvider not configured!"));
    }

    private abstract static class StubContextProvider implements AlicebotContextProvider {
        @Override public Map<String, String> getSessionData(String sessionId) { return Map.of(); }
        @Override public void updateSessionData(String sessionId, Map<String, String> data) {}
        @Override public String getDbSelect(String reportName, Set<Param> params) { return null; }
        @Override public void reportSave(String name, String symbol, Report report, String sessionId) {}
        @Override public String getRecordStatus(String parameter) { return null; }
        @Override public String getRecord(String parameter) { return null; }
        @Override public String updateRecord(String parameter) { return null; }
        @Override public String updateRecordStatus(String parameter) { return null; }
        @Override public Long getRecordId(String campaignName, String phoneNum) { return null; }
        @Override public List<String> getcampaigns(String phoneNum, boolean active) { return List.of(); }
    }
}

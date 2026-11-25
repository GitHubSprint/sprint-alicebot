package org.alicebot.ab.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.exception.InternalServerException;
import org.alicebot.ab.llm.dto.gpt.Choice;
import org.alicebot.ab.llm.dto.gpt.GptChatResponse;
import org.alicebot.ab.llm.dto.ollama.OllamaChatResponse;
import org.alicebot.ab.llm.report.CustomReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.alicebot.ab.MagicStrings.invalid_llm_configuration;

public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static volatile int timeout = 10;
    private static volatile int httpVersion = 2;
    private static volatile HttpClient client;

    static {
        client = createHttpClient(timeout, httpVersion);
    }

    private static HttpClient createHttpClient(int timeout, int version) {
        if(version == 1) {
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
        } else {
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .version(HttpClient.Version.HTTP_2)
                    .build();
        }
    }

    public static synchronized void setParameters(int newTimeout, int newVersion) {
        timeout = newTimeout;
        httpVersion = newVersion;
        client = createHttpClient(newTimeout, newVersion);
    }

    public static void ensureClientParameters(int newTimeout, int newVersion) {
        if (timeout != newTimeout || httpVersion != newVersion) {
            synchronized (LLMService.class) {
                if (timeout != newTimeout || httpVersion != newVersion) {
                    timeout = newTimeout;
                    httpVersion = newVersion;
                    client = createHttpClient(newTimeout, newVersion);
                    logger.info("LLMService HTTP client updated - timeout: {}, httpVersion: {}", newTimeout, newVersion);
                }
            }
        }
    }

    public static String chatGpt(String json, LlmProviderConfig config, String rag) throws Exception {
        if (config == null || config.apiUrl() == null || config.apiUrl().isBlank() || config.apiKey() == null || config.apiKey().isBlank()) {
            logger.warn("chatGpt invalid llmConfiguration: {}", config != null ? config.apiUrl() : null);
            throw new InternalServerException(invalid_llm_configuration);
        }

        return chatOpenAiCompatible("chatGpt", "GPT", json, config.apiUrl().trim(), config.apiKey(), rag);
    }

    public static String chatVllm(String json, LlmProviderConfig config, String rag) throws Exception {
        if (config == null || config.apiUrl() == null || config.apiUrl().isBlank()) {
            logger.warn("chatVllm invalid llmConfiguration: {}", config != null ? config.apiUrl() : null);
            throw new InternalServerException(invalid_llm_configuration);
        }

        return chatOpenAiCompatible("chatVllm", "vLLM", json, config.apiUrl().trim(), config.apiKey(), rag);
    }

    private static String chatOpenAiCompatible(String methodName, String providerLabel, String json, String apiUrl, String token, String rag) throws Exception {
        String report = "";
        String requestBody = json;

        try {
            JsonNode rootInputNode = mapper.readTree(json);
            boolean isModified = false;
            if (rootInputNode instanceof ObjectNode objectNode) {
                if (objectNode.has("report")) {
                    JsonNode reportNode = objectNode.get("report");
                    report = mapper.writeValueAsString(mapper.treeToValue(reportNode, CustomReport.class));
                    objectNode.remove("report");
                    isModified = true;
                }
                if (rag != null && !rag.isBlank()) {
                    ArrayNode messagesNode = (ArrayNode) objectNode.get("messages");

                    if (messagesNode != null) {
                        boolean systemFound = false;

                        for (JsonNode msg : messagesNode) {
                            if ("system".equals(msg.path("role").asText())) {
                                String currentContent = msg.path("content").asText("");
                                String updatedContent = currentContent + "\n\nContext:\n" + rag;
                                ((ObjectNode) msg).put("content", updatedContent);
                                systemFound = true;
                                break;
                            }
                        }

                        if (!systemFound) {
                            ObjectNode systemMsg = mapper.createObjectNode();
                            systemMsg.put("role", "system");
                            systemMsg.put("content", "Context:\n" + rag);
                            messagesNode.insert(0, systemMsg);
                        }

                        isModified = true;
                    }
                }
                if (isModified) {
                    requestBody = mapper.writeValueAsString(objectNode);
                }
            }
        } catch (Exception e) {
            logger.warn("{} problem modifying input JSON, sending original: {}", methodName, e.getMessage());
        }

        logger.info("{} URI: {} json: \n\n{}\n\n", methodName, apiUrl, requestBody);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

        if (token != null && !token.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> httpResponse = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        logger.debug("{} httpResponse statusCode: {}, body: {}", methodName, httpResponse.statusCode(), httpResponse.body());

        if (httpResponse.statusCode() != 200) {
            logger.error("{} API error! Status: {}, Body: {}", providerLabel, httpResponse.statusCode(), httpResponse.body());
            return MagicStrings.error_bot_response();
        }

        GptChatResponse response = mapper.readValue(httpResponse.body(), GptChatResponse.class);

        logger.info("{} response: {}", methodName, response);

        if(response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            Choice choice = response.getChoices().getFirst();

            String responseMessage = choice.getMessage().getContent() + report;
            logger.info("{} responseMessage: {}", methodName, responseMessage);
            return responseMessage;
        }
        return MagicStrings.error_bot_response();
    }

    public static String chatGemini(String json, LlmProviderConfig config, String model, String rag) throws Exception {
        if (config == null || config.apiUrl() == null || config.apiUrl().isBlank() || config.apiKey() == null || config.apiKey().isBlank()) {
            logger.warn("chatGemini invalid llmConfiguration: {}", config != null ? config.apiUrl() : null);
            throw new InternalServerException(invalid_llm_configuration);
        }

        return chatGeminiCompatible("chatGemini", "Gemini", json, config.apiUrl().trim(), config.apiKey(), false, model, rag);
    }

    public static String chatVertexAi(String json, LlmProviderConfig config, String model, String rag) throws Exception {
        if (config == null || config.apiUrl() == null || config.apiUrl().isBlank() || config.apiKey() == null || config.apiKey().isBlank()) {
            logger.warn("chatVertexAi invalid llmConfiguration: {}", config != null ? config.apiUrl() : null);
            throw new InternalServerException(invalid_llm_configuration);
        }

        return chatGeminiCompatible("chatVertexAi", "Vertex AI", json, config.apiUrl().trim(), config.apiKey(), true, model, rag);
    }

    private static String chatGeminiCompatible(String methodName, String providerLabel, String json, String baseUrl, String token, boolean useBearerAuth, String model, String rag) throws Exception {
        String report = "";
        String requestBody = json;

        try {
            JsonNode rootInputNode = mapper.readTree(json);
            boolean isModified = false;
            if (rootInputNode instanceof ObjectNode objectNode) {
                if (objectNode.has("report")) {
                    JsonNode reportNode = objectNode.get("report");
                    report = mapper.writeValueAsString(mapper.treeToValue(reportNode, CustomReport.class));
                    objectNode.remove("report");
                    isModified = true;
                }

                if (rag != null && !rag.isBlank()) {
                    ObjectNode systemInstruction;

                    if (objectNode.has("systemInstruction")) {
                        systemInstruction = (ObjectNode) objectNode.get("systemInstruction");
                    } else {
                        systemInstruction = mapper.createObjectNode();
                        objectNode.set("systemInstruction", systemInstruction);
                    }

                    ArrayNode partsNode;
                    if (systemInstruction.has("parts") && systemInstruction.get("parts").isArray()) {
                        partsNode = (ArrayNode) systemInstruction.get("parts");
                    } else {
                        partsNode = mapper.createArrayNode();
                        systemInstruction.set("parts", partsNode);
                    }

                    String existingText = "";
                    if (!partsNode.isEmpty() && partsNode.get(0).has("text")) {
                        existingText = partsNode.get(0).get("text").asText();
                    }

                    String updatedText = existingText.isBlank()
                            ? "Context:\n" + rag
                            : existingText + "\n\nContext:\n" + rag;

                    ObjectNode textPart = mapper.createObjectNode();
                    textPart.put("text", updatedText);

                    partsNode.removeAll(); // czyszczenie starych parts
                    partsNode.add(textPart);

                    isModified = true;
                }

                if (objectNode.has("modelname")) {
                    objectNode.remove("modelname");
                    isModified = true;
                }
                if (isModified) {
                    requestBody = mapper.writeValueAsString(objectNode);
                }
            }
        } catch (Exception e) {
            logger.warn("{} failed to modify input JSON, sending original: {}", methodName, e.getMessage());
        }

        String fullUrl = useBearerAuth
                ? String.format("%s/%s:generateContent", baseUrl, model)
                : String.format("%s/%s:generateContent?key=%s", baseUrl, model, token);

        logger.info("{} request to URL: {}/{}:generateContent with body: \n\n{}\n\n", methodName, baseUrl, model, requestBody);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

        if (useBearerAuth) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> httpResponse = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            logger.error("{} API error! Status: {}, Body: {}", providerLabel, httpResponse.statusCode(), httpResponse.body());
            return MagicStrings.error_bot_response();
        }

        try {
            JsonNode rootNode = mapper.readTree(httpResponse.body());
            JsonNode candidates = rootNode.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String textResponse = parts.get(0).path("text").asText();
                    return textResponse + report;
                }
            }
            logger.warn("{} response has an invalid structure: {}", providerLabel, httpResponse.body());
        } catch (Exception e) {
            logger.error("Error parsing {} response: {}", providerLabel, e.getMessage(), e);
        }

        return MagicStrings.error_bot_response();
    }


    public static String chatOllama(String json, LlmProviderConfig config, String rag) throws Exception {
        if (config == null || config.apiUrl() == null || config.apiUrl().isBlank()) {
            logger.warn("chatOllama invalid llmConfiguration!");
            throw new InternalServerException(invalid_llm_configuration);
        }

        String ollamaApiUrl = config.apiUrl().trim();

        String report = "";
        String requestBody = json;

        try {
            JsonNode rootInputNode = mapper.readTree(json);
            boolean isModified = false;
            if (rootInputNode instanceof ObjectNode objectNode) {
                if (objectNode.has("report")) {
                    JsonNode reportNode = objectNode.get("report");
                    report = mapper.writeValueAsString(mapper.treeToValue(reportNode, CustomReport.class));
                    objectNode.remove("report");
                    isModified = true;
                }
                if (rag != null && !rag.isBlank()) {
                    ArrayNode messagesNode = (ArrayNode) objectNode.get("messages");
                    if (messagesNode != null) {
                        boolean systemFound = false;

                        for (JsonNode msg : messagesNode) {
                            if ("system".equals(msg.path("role").asText())) {
                                String currentContent = msg.path("content").asText("");
                                String updatedContent = currentContent + "\n\nContext:\n" + rag;
                                ((ObjectNode) msg).put("content", updatedContent);
                                systemFound = true;
                                break;
                            }
                        }

                        if (!systemFound) {
                            ObjectNode systemMsg = mapper.createObjectNode();
                            systemMsg.put("role", "system");
                            systemMsg.put("content", "Context:\n" + rag);
                            messagesNode.insert(0, systemMsg);
                        }

                        isModified = true;
                    }
                }
                if (isModified) {
                    requestBody = mapper.writeValueAsString(objectNode);
                }
            }
        } catch (Exception e) {
            logger.warn("chatOllama problem modifying input JSON, sending original: {}", e.getMessage());
        }


        logger.info("chatOllama URI: {} json: \n\n{}\n\n", ollamaApiUrl, requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(ollamaApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            logger.error("OLLAMA API error! Status: {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
            return MagicStrings.error_bot_response();
        }

        OllamaChatResponse response =  mapper.readValue(httpResponse.body(), OllamaChatResponse.class);

        logger.info("chatOllama response: {}", response);

        if(response != null && response.getMessage() != null) {
            return response.getMessage().getContent() + report;
        }
        return MagicStrings.error_bot_response();
    }

    public static String chatClaude(String json, LlmProviderConfig config, String rag) throws Exception {
        if (config == null || config.apiUrl() == null || config.apiUrl().isBlank() || config.apiKey() == null || config.apiKey().isBlank()) {
            logger.warn("chatClaude invalid llmConfiguration: {}", config != null ? config.apiUrl() : null);
            throw new InternalServerException(invalid_llm_configuration);
        }

        String claudeApiUrl = config.apiUrl().trim();
        String apiKey = config.apiKey();
        String anthropicVersion = config.getExtraProperty("anthropicVersion", "2023-06-01");

        String report = "";
        String requestBody = json;

        try {
            JsonNode rootInputNode = mapper.readTree(json);
            boolean isModified = false;
            if (rootInputNode instanceof ObjectNode objectNode) {
                if (objectNode.has("report")) {
                    JsonNode reportNode = objectNode.get("report");
                    report = mapper.writeValueAsString(mapper.treeToValue(reportNode, CustomReport.class));
                    objectNode.remove("report");
                    isModified = true;
                }
                if (rag != null && !rag.isBlank()) {
                    String existingSystem = objectNode.path("system").asText("");
                    String updatedSystem = existingSystem.isBlank()
                            ? "Context:\n" + rag
                            : existingSystem + "\n\nContext:\n" + rag;
                    objectNode.put("system", updatedSystem);
                    isModified = true;
                }
                if (isModified) {
                    requestBody = mapper.writeValueAsString(objectNode);
                }
            }
        } catch (Exception e) {
            logger.warn("chatClaude problem modifying input JSON, sending original: {}", e.getMessage());
        }

        logger.info("chatClaude URI: {} version: {} json: \n\n{}\n\n", claudeApiUrl, anthropicVersion, requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(claudeApiUrl))
                .header("x-api-key", apiKey)
                .header("anthropic-version", anthropicVersion)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        logger.debug("chatClaude httpResponse statusCode: {}, body: {}", httpResponse.statusCode(), httpResponse.body());

        if (httpResponse.statusCode() != 200) {
            logger.error("Claude API error! Status: {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
            return MagicStrings.error_bot_response();
        }

        try {
            JsonNode rootNode = mapper.readTree(httpResponse.body());
            JsonNode content = rootNode.path("content");
            if (content.isArray() && !content.isEmpty()) {
                StringBuilder textBuilder = new StringBuilder();
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText()) && block.has("text")) {
                        textBuilder.append(block.get("text").asText());
                    }
                }
                if (!textBuilder.isEmpty()) {
                    String responseMessage = textBuilder + report;
                    logger.info("chatClaude responseMessage: {}", responseMessage);
                    return responseMessage;
                }
            }
            logger.warn("Claude response has an invalid structure: {}", httpResponse.body());
        } catch (Exception e) {
            logger.error("Error parsing Claude response: {}", e.getMessage(), e);
        }

        return MagicStrings.error_bot_response();
    }
}

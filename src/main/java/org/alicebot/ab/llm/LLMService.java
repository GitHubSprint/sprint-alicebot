package org.alicebot.ab.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static HttpClient client;

    static {
        client = createHttpClient(LLMConfiguration.timeout, LLMConfiguration.httpVersion);
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

    public static void setParameters(int timeout, int version) {
        LLMConfiguration.timeout = timeout;
        LLMConfiguration.httpVersion = version;
        client = createHttpClient(LLMConfiguration.timeout, version);
    }

    public static String chatGpt(String json, String token) throws Exception {
        if(LLMConfiguration.gptApiUrl == null || token == null) {
            logger.warn("chatGpt invalid llmConfiguration: {}", LLMConfiguration.gptApiUrl);
            throw new InternalServerException(invalid_llm_configuration);
        }

        String report = "";
        int idxReport = json.indexOf("{\"report\":");
        if(idxReport >= 0) {
            CustomReport customReport = mapper.readValue(json.substring(idxReport), CustomReport.class);
            if(customReport != null) {
                report = mapper.writeValueAsString(customReport);
            }
            json = json.substring(0,idxReport);
        }

        logger.info("chatGpt URI: {} json: \n\n{}\n\n", LLMConfiguration.gptApiUrl, json);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(LLMConfiguration.gptApiUrl.trim()))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();


        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        logger.debug("chatGpt httpResponse statusCode: {}, body: {}", httpResponse.statusCode(), httpResponse.body());

        if (httpResponse.statusCode() != 200) {
            logger.error("Błąd API GPT! Status: {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
            return MagicStrings.error_bot_response();
        }

        GptChatResponse response = mapper.readValue(httpResponse.body(), GptChatResponse.class);

        logger.info("chatGpt response: {}", response);

        if(response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            Choice choice = response.getChoices().getFirst();

            String responseMessage = choice.getMessage().getContent() + report;
            logger.info("chatGpt responseMessage: {}", responseMessage);
            return responseMessage;
        }
        return MagicStrings.error_bot_response();
    }

    public static String chatGemini(String json, String token, String model) throws Exception {
        if (LLMConfiguration.geminiApiUrl == null || token == null) {
            logger.warn("chatGemini invalid llmConfiguration: {}", LLMConfiguration.geminiApiUrl);
            throw new InternalServerException(invalid_llm_configuration);
        }

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

                if (objectNode.has("modelname")) {
                    objectNode.remove("modelname");
                    isModified = true;
                }
                if (isModified) {
                    requestBody = mapper.writeValueAsString(objectNode);
                }
            }
        } catch (Exception e) {
            logger.warn("Nie udało się zmodyfikować wejściowego JSON-a, wysyłam oryginał: {}", e.getMessage());
        }

        String baseUrl = LLMConfiguration.geminiApiUrl.trim();
        String fullUrl = String.format("%s/%s:generateContent?key=%s", baseUrl, model, token);

        if (logger.isInfoEnabled()) {
            logger.info("chatGemini request to URL: {}/{}:generateContent with body: \n{}\n", baseUrl, model, requestBody);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            logger.error("Błąd API Gemini! Status: {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
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

            logger.warn("Odpowiedź Gemini ma nieprawidłową strukturę: {}", httpResponse.body());
        } catch (Exception e) {
            logger.error("Błąd podczas parsowania odpowiedzi Gemini: {}", e.getMessage(), e);
        }

        return MagicStrings.error_bot_response();
    }


    public static String chatOllama(String json) throws Exception {
        if(LLMConfiguration.ollamaApiUrl == null) {
            logger.warn("chatOllama invalid llmConfiguration!");
            throw new InternalServerException(invalid_llm_configuration);
        }

        logger.info("chatOllama json: \n{}\n", json);

        String report = "";
        int idxReport = json.indexOf("{\"report\":");
        if(idxReport >= 0) {
            CustomReport customReport = mapper.readValue(json.substring(idxReport), CustomReport.class);
            if(customReport != null) {
                report = mapper.writeValueAsString(customReport);
            }
            json = json.substring(0,idxReport);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(LLMConfiguration.ollamaApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            logger.error("Błąd API OLLAMA! Status: {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
            return MagicStrings.error_bot_response();
        }

        OllamaChatResponse response =  mapper.readValue(httpResponse.body(), OllamaChatResponse.class);

        logger.info("chatOllama response: {}", response);

        if(response != null && response.getMessage() != null) {
            return response.getMessage().getContent() + report;
        }
        return MagicStrings.error_bot_response();
    }

    private static boolean isNull(String test){
        return test == null || test.isEmpty();
    }
}

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

    public static String chatGpt(String json, String token, String rag) throws Exception {
        if(LLMConfiguration.gptApiUrl == null || token == null) {
            logger.warn("chatGpt invalid llmConfiguration: {}", LLMConfiguration.gptApiUrl);
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
            logger.warn("chatGpt problem modifying input JSON, sending original: {}", e.getMessage());
        }

        logger.info("chatGpt URI: {} json: \n\n{}\n\n", LLMConfiguration.gptApiUrl, requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(LLMConfiguration.gptApiUrl.trim()))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();


        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        logger.debug("chatGpt httpResponse statusCode: {}, body: {}", httpResponse.statusCode(), httpResponse.body());

        if (httpResponse.statusCode() != 200) {
            logger.error("GPT API error! Status: {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
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

    public static String chatGemini(String json, String token, String model, String rag) throws Exception {
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
            logger.warn("Failed to modify input JSON, sending original: {}", e.getMessage());
        }

        String baseUrl = LLMConfiguration.geminiApiUrl.trim();
        String fullUrl = String.format("%s/%s:generateContent?key=%s", baseUrl, model, token);

        logger.info("chatGemini request to URL: {}/{}:generateContent with body: \n\n{}\n\n", baseUrl, model, requestBody);


        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            logger.error("Gemini API error! Status: {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
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
            logger.warn("Gemini response has an invalid structure: {}", httpResponse.body());
        } catch (Exception e) {
            logger.error("Error parsing Gemini response: {}", e.getMessage(), e);
        }

        return MagicStrings.error_bot_response();
    }


    public static String chatOllama(String json, String rag) throws Exception {
        if(LLMConfiguration.ollamaApiUrl == null) {
            logger.warn("chatOllama invalid llmConfiguration!");
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


        logger.info("chatOllama URI: {} json: \n\n{}\n\n", LLMConfiguration.ollamaApiUrl, requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(LLMConfiguration.ollamaApiUrl))
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
}

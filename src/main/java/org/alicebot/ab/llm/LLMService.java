package org.alicebot.ab.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.db.SprintBotDbUtils;
import org.alicebot.ab.exception.InternalServerException;
import org.alicebot.ab.llm.dto.google.Candidates;
import org.alicebot.ab.llm.dto.google.GeminiChatResponse;
import org.alicebot.ab.llm.dto.google.Predictions;
import org.alicebot.ab.llm.dto.gpt.Choice;
import org.alicebot.ab.llm.dto.gpt.GptChatResponse;
import org.alicebot.ab.llm.dto.ollama.OllamaChatResponse;
import org.alicebot.ab.llm.report.CustomReport;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.alicebot.ab.MagicStrings.invalid_llm_configuration;

public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client;
    
    static {
        client = HttpClient.newBuilder()
                .build();
    }

    public static String chatGpt(String json) throws Exception {
        if(LLMConfiguration.gptApiUrl == null || LLMConfiguration.gptToken == null) {
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

        logger.info("chatGpt json: {}", json);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(LLMConfiguration.gptApiUrl))
                .header("Authorization", "Bearer " + LLMConfiguration.gptToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();


        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

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

    public static String chatGemini(String json) throws Exception {
        if(LLMConfiguration.geminiApiUrl == null || LLMConfiguration.geminiToken == null) {
            logger.warn("chatGemini invalid llmConfiguration: {}", LLMConfiguration.geminiApiUrl);
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

        logger.info("chatGemini json: {}", json);


        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(LLMConfiguration.geminiApiUrl))
                .header("Authorization", "Bearer " + LLMConfiguration.geminiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();


        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        GeminiChatResponse response = mapper.readValue(httpResponse.body(), GeminiChatResponse.class);

        logger.info("chatGemini response: {}", response);

        if(response != null && response.getPredictions() != null && !response.getPredictions().isEmpty()) {
            Predictions predictions = response.getPredictions().getFirst();

            if(predictions.getCandidates() != null && !predictions.getCandidates().isEmpty()){
                Candidates candidates = predictions.getCandidates().getFirst();
                return candidates.getContent() + report;
            }
        }
        return MagicStrings.error_bot_response();
    }


    public static String chatOllama(String json) throws Exception {
        if(LLMConfiguration.ollamaApiUrl == null) {
            logger.warn("chatOllama invalid llmConfiguration: {}", LLMConfiguration.ollamaApiUrl);
            throw new InternalServerException(invalid_llm_configuration);
        }

        logger.info("chatOllama json: {}", json);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(LLMConfiguration.ollamaApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        OllamaChatResponse response =  mapper.readValue(httpResponse.body(), OllamaChatResponse.class);

        logger.info("chatOllama response: {}", response);

        if(response != null) {
            return response.getMessage().getContent();
        }
        return MagicStrings.error_bot_response();
    }

    private static boolean isNull(String test){
        return test == null || test.isEmpty();
    }
}

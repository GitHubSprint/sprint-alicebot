package org.alicebot.ab.gpt;

import org.alicebot.ab.AIMLProcessor;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatGPT {
    private static final Logger logger = LoggerFactory.getLogger(ChatGPT.class);

    @NotNull
    public static JSONObject createGPTResponse(String model,
                                               String system,
                                               String user,
                                               String assistant,
                                               int iTemperature,
                                               int maxTokens,
                                               int topP,
                                               int frequencyPenalty,
                                               int presencePenalty) throws JSONException
    {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        JSONArray messages = new JSONArray();

        if(system != null && !system.isEmpty()) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", system);
            messages.put(systemMessage);
        }

        if(assistant != null && !assistant.isEmpty()) {
            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", assistant);
            messages.put(assistantMessage);
        }

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", user);
        messages.put(userMessage);

        jsonRequest.put("messages", messages);

        jsonRequest.put("temperature", iTemperature);
        jsonRequest.put("max_tokens", maxTokens);
        jsonRequest.put("top_p", topP);
        jsonRequest.put("frequency_penalty", frequencyPenalty);
        jsonRequest.put("presence_penalty", presencePenalty);
        logger.info("createGPTResponse request: " + jsonRequest);
        return jsonRequest;
    }


    public static void addMessage(JSONArray messages, String role, String content) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        messages.put(message);
    }

    // Metoda do rozbudowywania istniejącego JSON o kolejne wiadomości
    public static String addMessageToJSON(String jsonString, String role, String content, int maxResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);

        // Pobranie tablicy "messages" z aktualnego JSON lub utworzenie nowej, jeśli nie istnieje
        JSONArray messages = jsonObject.optJSONArray("messages");
        if (messages == null) {
            messages = new JSONArray();
        }

        if(messages.length() > maxResponse)
            messages.remove(0);

        // Dodanie nowej wiadomości
        addMessage(messages, role, content);

        // Zaktualizowanie JSON o zaktualizowaną tablicę "messages"
        jsonObject.put("messages", messages);

        String response = jsonObject.toString();
        logger.info("addMessageToJSON request: " + response);
        return response;
    }
}

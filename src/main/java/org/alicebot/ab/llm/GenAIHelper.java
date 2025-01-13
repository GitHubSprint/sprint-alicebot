package org.alicebot.ab.llm;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenAIHelper {
    private static final Logger logger = LoggerFactory.getLogger(GenAIHelper.class);

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
        logger.info("createGPTResponse request: {}", jsonRequest);
        return jsonRequest;
    }

    @NotNull
    public static JSONObject createOllamaResponse(String model,
                                               String system,
                                               String user, boolean stream) throws JSONException
    {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        jsonRequest.put("stream", stream);
        JSONArray messages = new JSONArray();

        if(system != null && !system.isEmpty()) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", system);
            messages.put(systemMessage);
        }

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", user);
        messages.put(userMessage);

        jsonRequest.put("messages", messages);

        logger.info("createOllamaResponse request: {}", jsonRequest);
        return jsonRequest;
    }

    @NotNull
    public static JSONObject createGeminiResponse(String context,
                                                  String user,
                                                  double temperature,
                                                  int maxOutputTokens,
                                                  double topP,
                                                  int topK) throws JSONException
    {
        JSONObject jsonRequest = new JSONObject();
        JSONObject parameters = new JSONObject();
        parameters.put("temperature", temperature);
        parameters.put("maxOutputTokens", maxOutputTokens);
        parameters.put("topP", topP);
        parameters.put("topK", topK);
        jsonRequest.put("parameters", parameters);

        JSONArray instances = new JSONArray();
        JSONObject instance = new JSONObject();
        if(context != null && !context.isEmpty()) {
            instance.put("context", context);
        }

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("author","user");
        message.put("content",user);
        messages.put(message);
        instance.put("messages",messages);

        instances.put(instance);
        jsonRequest.put("instances", instances);

        return jsonRequest;
    }


    public static void addMessage(JSONArray messages, String roleName, String role, String content) throws JSONException {
        JSONObject message = new JSONObject();
        message.put(roleName, role);
        message.put("content", content);
        messages.put(message);
    }

    // Metoda do rozbudowywania istniejącego JSON o kolejne wiadomości
    public static String addGptMessageToJSON(String jsonString, String role, String content, int maxResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);

        // Pobranie tablicy "messages" z aktualnego JSON lub utworzenie nowej, jeśli nie istnieje
        JSONArray messages = jsonObject.optJSONArray("messages");
        if (messages == null) {
            messages = new JSONArray();
        }

        if(messages.length() > 1 && messages.length() > maxResponse)
            messages.remove(1);

        // Dodanie nowej wiadomości
        addMessage(messages, "role", role, content);

        // Zaktualizowanie JSON o zaktualizowaną tablicę "messages"
        jsonObject.put("messages", messages);

        String response = jsonObject.toString();
        logger.info("addMessageToJSON response: {}", response);
        return response;
    }

    public static String addOllamaMessageToJSON(String jsonString, String role, String content, int maxResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);

        // Pobranie tablicy "messages" z aktualnego JSON lub utworzenie nowej, jeśli nie istnieje
        JSONArray messages = jsonObject.optJSONArray("messages");
        if (messages == null) {
            messages = new JSONArray();
        }

        if(messages.length() > 1 && messages.length() > maxResponse)
            messages.remove(1);

        // Dodanie nowej wiadomości
        addMessage(messages, "role", role, content);

        // Zaktualizowanie JSON o zaktualizowaną tablicę "messages"
        jsonObject.put("messages", messages);

        String response = jsonObject.toString();
        logger.info("addOllamaMessageToJSON response: {}", response);
        return response;
    }

    public static String addGeminiMessageToJSON(String jsonString, String context, String author, String content, int maxResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray instances = jsonObject.optJSONArray("instances");
        JSONObject parameters = jsonObject.optJSONObject("parameters");

        // Pobranie tablicy "messages" z aktualnego JSON lub utworzenie nowej, jeśli nie istnieje
        JSONObject instance = instances.optJSONObject(0);
        if(context == null || context.isEmpty())
            context = instance.getString("context");

        JSONArray messages = instance.optJSONArray("messages");
        if (messages == null) {
            messages = new JSONArray();
        }
        if(messages.length() > 1 && messages.length() > maxResponse)
            messages.remove(1);

        // Dodanie nowej wiadomości
        addMessage(messages, "author", author, content);

        instance = new JSONObject();
        instance.put("context", context);
        instance.put("messages", messages);

        instances = new JSONArray();
        instances.put(instance);

        jsonObject = new JSONObject();
        jsonObject.put("parameters", parameters);
        jsonObject.put("instances", instances);
        String response = jsonObject.toString();

        logger.info("addGeminiMessageToJSON response: {}", response);
        return response;
    }


}

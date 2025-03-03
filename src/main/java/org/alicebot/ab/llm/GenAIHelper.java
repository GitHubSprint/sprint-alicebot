package org.alicebot.ab.llm;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GenAIHelper {
    private static final Logger logger = LoggerFactory.getLogger(GenAIHelper.class);


    public static void main(String[] args) {
        try {
            JSONObject json = createGPTResponse("gpt-3.5-turbo",
                    "System message",
                    "User message",
                    "Assistant message",
                    Map.of("temperature", "0.5",
                            "max_tokens", "100",
                            "top_p", "0.9",
                            "frequency_penalty", "0.0",
                            "presence_penalty", "0.0"));
            logger.info("json: {}", json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject json = createOllamaResponse("ollama",
                    "System message",
                    "User message", false);
            logger.info("json: {}", json);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        try {
            JSONObject json = createGeminiResponse("context",
                    "User message",
                    Map.of("temperature", "0.5",
                            "max_tokens", "100",
                            "top_p", "0.9",
                            "frequency_penalty", "0.0",
                            "presence_penalty", "0.0"));
            logger.info("json: {}", json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    public static JSONObject createGPTResponse(String model,
                                               String system,
                                               String user,
                                               String assistant,
                                               Map<String, String> addParams) throws JSONException
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

        for(Map.Entry<String, String> entry : addParams.entrySet()) {
            if(isNumeric(entry.getValue()))
                jsonRequest.put(entry.getKey(), Double.parseDouble(entry.getValue()));
            else
                jsonRequest.put(entry.getKey(), entry.getValue());
        }

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
                                                  Map<String, String> addParams) throws JSONException
    {
        JSONObject jsonRequest = new JSONObject();
        JSONObject parameters = new JSONObject();

        for(Map.Entry<String, String> entry : addParams.entrySet()) {
            if(isNumeric(entry.getValue()))
                parameters.put(entry.getKey(), Double.parseDouble(entry.getValue()));
            else
                parameters.put(entry.getKey(), entry.getValue());
        }

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

    private static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


}

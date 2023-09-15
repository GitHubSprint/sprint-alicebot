package org.alicebot.ab.gpt;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatGPT {

    public static void main(String[] args) throws JSONException {


        // Tworzenie obiektu JSON
        JSONObject jsonRequest = createGPTResponse(
                "gpt-4",
                "rozpoznaj, czy to czytanie czy pisanie",
                "potrzebuję recepty",
                "To jest pisanie.",
                1,
                1,
                0,
                0,
                0);


        String jsonString = jsonRequest.toString();

        // Wyświetlenie wynikowego JSON
        System.out.println(jsonString);

        // Aktualizowanie JSON o kolejne wiadomości
        jsonString = addMessageToJSON(jsonString, "user", "kolejna wiadomość od użytkownika");
        jsonString = addMessageToJSON(jsonString, "assistant", "kolejna wiadomość od asystenta");

        // Wyświetlenie zaktualizowanego JSON
        System.out.println(jsonString);

        String test = "GPT={}";
        System.out.println(test.substring(4));
    }



    @NotNull
    public static JSONObject createGPTResponse(String model, String system, String user, String assistant, int iTemperature, int maxTokens, int topP, int frequencyPenalty, int presencePenalty) throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        JSONArray messages = new JSONArray();

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", user);
        messages.put(userMessage);

        if(system != null) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", system);
            messages.put(systemMessage);
        }


        if(assistant != null) {
            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", assistant);
            messages.put(assistantMessage);
        }

        jsonRequest.put("messages", messages);

        jsonRequest.put("temperature", iTemperature);
        jsonRequest.put("max_tokens", maxTokens);
        jsonRequest.put("top_p", topP);
        jsonRequest.put("frequency_penalty", frequencyPenalty);
        jsonRequest.put("presence_penalty", presencePenalty);
        return jsonRequest;
    }


    public static void addMessage(JSONArray messages, String role, String content) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        messages.put(message);
    }

    // Metoda do rozbudowywania istniejącego JSON o kolejne wiadomości
    public static String addMessageToJSON(String jsonString, String role, String content) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);

        // Pobranie tablicy "messages" z aktualnego JSON lub utworzenie nowej, jeśli nie istnieje
        JSONArray messages = jsonObject.optJSONArray("messages");
        if (messages == null) {
            messages = new JSONArray();
        }

        // Dodanie nowej wiadomości
        addMessage(messages, role, content);

        // Zaktualizowanie JSON o zaktualizowaną tablicę "messages"
        jsonObject.put("messages", messages);

        return jsonObject.toString();
    }
}

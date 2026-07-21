package org.alicebot.ab.llm;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GenAIHelper {

    private static final Map<String, String> GEMINI_PARAM_MAP = Map.of(
            "max_tokens", "maxOutputTokens",
            "temperature", "temperature",
            "top_p", "topP",
            "top_k", "topK"
    );

    // --- GPT ---
    @NotNull
    public static JSONObject buildGptRequest(ChatContext context, String model, String addparams) throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);

        JSONArray messages = new JSONArray();

        if (context.getSystemPrompt() != null && !context.getSystemPrompt().isEmpty()) {
            messages.put(new JSONObject().put("role", "system").put("content", context.getSystemPrompt()));
        }

        // Dodaj historię i bieżące wiadomości
        for (ChatContext.ChatMessage msg : context.getMessages()) {
            String roleStr = msg.role() == ChatContext.Role.ASSISTANT ? "assistant" : "user";
            messages.put(new JSONObject().put("role", roleStr).put("content", msg.content()));
        }

        jsonRequest.put("messages", messages);
        applyParamsDirectly(jsonRequest, parseParams(addparams));
        return jsonRequest;
    }

    // --- OLLAMA ---
    @NotNull
    public static JSONObject buildOllamaRequest(ChatContext context, String model, boolean stream, String addparams) throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        jsonRequest.put("stream", stream);

        JSONArray messages = new JSONArray();

        if (context.getSystemPrompt() != null && !context.getSystemPrompt().isEmpty()) {
            messages.put(new JSONObject().put("role", "system").put("content", context.getSystemPrompt()));
        }

        for (ChatContext.ChatMessage msg : context.getMessages()) {
            String roleStr = msg.role() == ChatContext.Role.ASSISTANT ? "assistant" : "user";
            messages.put(new JSONObject().put("role", roleStr).put("content", msg.content()));
        }

        jsonRequest.put("messages", messages);

        Map<String, String> params = parseParams(addparams);
        if (!params.isEmpty()) {
            JSONObject options = new JSONObject();
            applyParamsDirectly(options, params);
            jsonRequest.put("options", options);
        }
        return jsonRequest;
    }

    // --- GEMINI ---
    @NotNull
    public static JSONObject buildGeminiRequest(ChatContext context, String model, String addparams) throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("modelname", model);

        if (context.getSystemPrompt() != null && !context.getSystemPrompt().isEmpty()) {
            jsonRequest.put("systemInstruction", new JSONObject().put("parts",
                    new JSONArray().put(new JSONObject().put("text", context.getSystemPrompt()))));
        }

        JSONArray contents = new JSONArray();
        for (ChatContext.ChatMessage msg : context.getMessages()) {
            String roleStr = msg.role() == ChatContext.Role.ASSISTANT ? "model" : "user";
            contents.put(new JSONObject().put("role", roleStr).put("parts",
                    new JSONArray().put(new JSONObject().put("text", msg.content()))));
        }

        if (!contents.isEmpty()) {
            jsonRequest.put("contents", contents);
        }

        Map<String, String> params = parseParams(addparams);
        if (!params.isEmpty()) {
            JSONObject genConfig = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String geminiKey = GEMINI_PARAM_MAP.getOrDefault(entry.getKey(), entry.getKey());
                if (isNumeric(entry.getValue())) genConfig.put(geminiKey, Double.parseDouble(entry.getValue()));
                else genConfig.put(geminiKey, entry.getValue());
            }
            jsonRequest.put("generationConfig", genConfig);
        }
        return jsonRequest;
    }

    // --- UTILS ---
    private static Map<String, String> parseParams(String addparams) {
        Map<String, String> additionalParameters = new HashMap<>();
        if (addparams != null && !addparams.isEmpty()) {
            for (String param : addparams.split(",")) {
                String[] keyVal = param.split("=");
                if (keyVal.length == 2) additionalParameters.put(keyVal[0].trim(), keyVal[1].trim());
            }
        }
        return additionalParameters;
    }

    private static void applyParamsDirectly(JSONObject target, Map<String, String> params) throws JSONException {
        if (params == null) return;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (isNumeric(entry.getValue())) target.put(entry.getKey(), Double.parseDouble(entry.getValue()));
            else target.put(entry.getKey(), entry.getValue());
        }
    }

    private static boolean isNumeric(String str) {
        if (str == null) return false;
        try { Double.parseDouble(str); return true; } catch (NumberFormatException e) { return false; }
    }
}
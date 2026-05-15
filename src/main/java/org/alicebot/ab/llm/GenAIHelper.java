package org.alicebot.ab.llm;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GenAIHelper {

    private static final Map<String, String> GEMINI_PARAM_MAP = Map.of(
            "max_tokens", "maxOutputTokens",
            "temperature", "temperature",
            "top_p", "topP",
            "top_k", "topK"
    );

    public static String gptRequest(String addparams, String assistant, String system, String json, String model, int iMaxResponse) throws JSONException {
        Map<String, String> additionalParameters = parseParams(addparams);

        if (json == null || (assistant != null && !assistant.isEmpty() && system != null && !system.isEmpty())) {
            return createGPTResponse(model, system, null, assistant, additionalParameters).toString();
        } else {
            if (assistant != null && !assistant.isEmpty())
                json = addGptMessageToJSON(json, "assistant", assistant.replaceAll("\\<.*?\\>", ""), iMaxResponse);
            if (system != null && !system.isEmpty())
                json = addGptMessageToJSON(json, "system", system.replaceAll("\\<.*?\\>", ""), iMaxResponse);

            return json;
        }
    }

    public static String ollamaRequest(String addparams, String assistant, String system, String json, String model, int iMaxResponse) throws JSONException {
        Map<String, String> additionalParameters = parseParams(addparams);

        if (json == null) {
            return createOllamaResponse(model, system, null, false, additionalParameters).toString();
        } else {
            if (system != null && !system.isEmpty())
                json = addOllamaMessageToJSON(json, "system", system.replaceAll("\\<.*?\\>", ""), iMaxResponse);
            if (assistant != null && !assistant.isEmpty())
                json = addOllamaMessageToJSON(json, "assistant", assistant.replaceAll("\\<.*?\\>", ""), iMaxResponse);

            return json;
        }
    }

    public static String geminiRequest(String addparams, String assistant, String system, String json, String model, int iMaxResponse) throws JSONException {
        Map<String, String> additionalParameters = parseParams(addparams);

        if (json == null) {
            return createGeminiResponse(model, system, null, additionalParameters).toString();
        } else {
            if (assistant != null && !assistant.isEmpty())
                json = addGeminiMessageToJSON(json, "model", assistant.replaceAll("\\<.*?\\>", ""), iMaxResponse);

            return json;
        }
    }

    @NotNull
    public static JSONObject createGPTResponse(String model, String system, String user, String assistant, Map<String, String> addParams) throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        JSONArray messages = new JSONArray();
        if (system != null && !system.isEmpty()) messages.put(new JSONObject().put("role", "system").put("content", system));
        if (assistant != null && !assistant.isEmpty()) messages.put(new JSONObject().put("role", "assistant").put("content", assistant));
        if(user !=null && !user.isEmpty()) messages.put(new JSONObject().put("role", "user").put("content", user));
        jsonRequest.put("messages", messages);
        applyParamsDirectly(jsonRequest, addParams);
        return jsonRequest;
    }

    @NotNull
    public static JSONObject createOllamaResponse(String model, String system, String user, boolean stream, Map<String, String> addParams) throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", model);
        jsonRequest.put("stream", stream);
        JSONArray messages = new JSONArray();
        if (system != null && !system.isEmpty()) messages.put(new JSONObject().put("role", "system").put("content", system));
        if(user !=null && !user.isEmpty()) messages.put(new JSONObject().put("role", "user").put("content", user));
        jsonRequest.put("messages", messages);
        if (addParams != null) {
            JSONObject options = new JSONObject();
            applyParamsDirectly(options, addParams);
            jsonRequest.put("options", options);
        }
        return jsonRequest;
    }

    @NotNull
    public static JSONObject createGeminiResponse(String model, String system, String user, Map<String, String> addParams) throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("modelname", model);
        if (system != null && !system.isEmpty()) {
            jsonRequest.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", system))));
        }

        if(user !=null && !user.isEmpty()) {
            JSONArray contents = new JSONArray();
            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(new JSONObject().put("text", user))));
            jsonRequest.put("contents", contents);
        }

        if (addParams != null) {
            JSONObject genConfig = new JSONObject();
            for (Map.Entry<String, String> entry : addParams.entrySet()) {
                String geminiKey = GEMINI_PARAM_MAP.getOrDefault(entry.getKey(), entry.getKey());
                if (isNumeric(entry.getValue())) genConfig.put(geminiKey, Double.parseDouble(entry.getValue()));
                else genConfig.put(geminiKey, entry.getValue());
            }
            jsonRequest.put("generationConfig", genConfig);
        }
        return jsonRequest;
    }


    public static String addGptMessageToJSON(String jsonString, String role, String content, int maxResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray messages = jsonObject.optJSONArray("messages");
        if (messages == null) messages = new JSONArray();
        if (messages.length() > 1 && messages.length() > maxResponse) messages.remove(1);
        messages.put(new JSONObject().put("role", role).put("content", content));
        return jsonObject.put("messages", messages).toString();
    }

    public static String addOllamaMessageToJSON(String jsonString, String role, String content, int maxResponse) throws JSONException {
        return addGptMessageToJSON(jsonString, role, content, maxResponse); // Ollama używa tego samego formatu messages
    }

    public static String addGeminiMessageToJSON(String jsonString, String role, String content, int maxResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray contents = jsonObject.optJSONArray("contents");
        if (contents == null) contents = new JSONArray();
        if (contents.length() > 1 && contents.length() > maxResponse) contents.remove(1);

        String geminiRole = role.equalsIgnoreCase("assistant") || role.equalsIgnoreCase("model") ? "model" : "user";

        JSONObject newMessage = new JSONObject().put("role", geminiRole)
                .put("parts", new JSONArray().put(new JSONObject().put("text", content)));
        contents.put(newMessage);
        return jsonObject.put("contents", contents).toString();
    }


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
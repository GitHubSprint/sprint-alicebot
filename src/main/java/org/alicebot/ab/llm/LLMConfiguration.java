package org.alicebot.ab.llm;

import java.util.HashMap;
import java.util.Map;

public class LLMConfiguration {
    public static Map<String, String> gptTokens = new HashMap<>();
    public static String gptApiUrl;
    public static String ollamaApiUrl;
    public static Map<String, String> geminiTokens = new HashMap<>();
    public static String geminiApiUrl;
    public static int timeout = 10;

    public static String gptDefaultModel;
    public static String ollamaDefaultModel;

    public static int gptMaxHistory;
    public static int ollamaMaxHistory;
    public static int geminiMaxHistory;


}

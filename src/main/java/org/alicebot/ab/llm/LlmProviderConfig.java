package org.alicebot.ab.llm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a single LLM provider fetched dynamically from {@link org.alicebot.ab.utils.AlicebotContextProvider}.
 *
 * @param apiUrl          Base or endpoint URL for the LLM provider API
 * @param apiKey          Optional API key / bearer token (may be null for providers like Ollama)
 * @param maxHistory      Default maximum conversation history size for this provider
 * @param extraProperties Optional provider-specific properties for future integrations
 */
public record LlmProviderConfig(
        String apiUrl,
        String apiKey,
        int maxHistory,
        Map<String, String> extraProperties
) {
    public LlmProviderConfig {
        extraProperties = extraProperties == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(extraProperties));
    }

    public LlmProviderConfig(String apiUrl, String apiKey, int maxHistory) {
        this(apiUrl, apiKey, maxHistory, Map.of());
    }

    public LlmProviderConfig(String apiUrl, int maxHistory) {
        this(apiUrl, null, maxHistory, Map.of());
    }

    public String getExtraProperty(String key) {
        return extraProperties.get(key);
    }

    public String getExtraProperty(String key, String defaultValue) {
        return extraProperties.getOrDefault(key, defaultValue);
    }
}

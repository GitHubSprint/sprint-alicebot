package org.alicebot.ab.model.say;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SayButton(
        @JsonProperty("subject")
        String subject,

        @JsonProperty("iconUrl")
        String iconUrl,

        @JsonProperty("label")
        String label,

        @JsonProperty("type")
        String type,

        @JsonProperty("class")
        String className,

        @JsonProperty("value")
        String value) {}
package org.alicebot.ab.model.survey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Item(
        @JsonProperty("icon")
        String icon,

        @JsonProperty("color")
        String color,

        @JsonProperty("text")
        String text,

        @JsonProperty("value")
        String value,

        @JsonProperty("min-value")
        String minValue,

        @JsonProperty("max-value")
        String maxValue
) {}

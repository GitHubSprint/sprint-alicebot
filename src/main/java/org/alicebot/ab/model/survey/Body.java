package org.alicebot.ab.model.survey;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Body(
        @JsonProperty("class")
        String className,

        @JsonProperty("type")
        String type,

        @JsonProperty("caption")
        String caption,

        @JsonProperty("label")
        String label) {}

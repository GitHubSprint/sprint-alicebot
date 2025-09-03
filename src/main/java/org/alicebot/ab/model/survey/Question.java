package org.alicebot.ab.model.survey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Question(
        @JsonProperty("question")
        String question,

        @JsonProperty("id")
        String id,

        @JsonProperty("placeholder")
        String placeholder,

        @JsonProperty("type")
        String type,

        @JsonProperty("class")
        String className,

        @JsonProperty("answers")
        List<String> answers) {}
package org.alicebot.ab.model.survey;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Survey(
        @JsonProperty("subject")
        String subject,

        @JsonProperty("questions")
        List<Question> questions,

        @JsonProperty("description")
        String description
) {}
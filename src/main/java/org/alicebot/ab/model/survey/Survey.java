package org.alicebot.ab.model.survey;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Survey {
    @JsonProperty("prompt")
    private Prompt prompt;

    @JsonProperty("body")
    private Body body;

    @JsonProperty("question")
    private Question question;

    @JsonProperty("summary")
    private Summary summary;

    public Survey(Prompt prompt) {
        this.prompt = prompt;
    }

    public Survey(Body body) {
        this.body = body;
    }

    public Survey(Question question) {
        this.question = question;
    }

    public Survey(Summary summary) {
        this.summary = summary;
    }
}
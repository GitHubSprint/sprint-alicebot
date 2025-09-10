package org.alicebot.ab.model.survey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Question(
        @JsonProperty("class")
        String className,

        @JsonProperty("type")
        String type,

        @JsonProperty("caption")
        String caption,

        @JsonProperty("label")
        String label,

        @JsonProperty("counter")
        String counter,

        @JsonProperty("items")
        List<Item> items,

        @JsonProperty("btn-back")
        String btnBack,

        @JsonProperty("btn-next")
        String btnNext,

        @JsonProperty("btn-cancel")
        String btnCancel
) {}
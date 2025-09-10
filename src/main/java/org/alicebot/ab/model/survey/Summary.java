package org.alicebot.ab.model.survey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Summary(
        @JsonProperty("class")
        String className,

        @JsonProperty("type")
        String type,

        @JsonProperty("caption")
        String caption,

        @JsonProperty("label")
        String label,

        @JsonProperty("btn-end")
        String btnEnd
) {
}

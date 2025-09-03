package org.alicebot.ab.model.feedback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Feedback {

	@JsonProperty("type")
	private String type;

    private List<String> labels;

    public Feedback(String type, List<String> labels) {
        this.type = type;
        this.labels = labels;
    }

    public String getType() {
        return type;
    }

    public List<String> getLabels() {
        return labels;
    }
}
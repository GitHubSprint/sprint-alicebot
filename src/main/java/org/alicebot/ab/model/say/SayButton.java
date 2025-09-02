package org.alicebot.ab.model.say;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
public class SayButton {

	@JsonProperty("subject")
	private String subject;

	@JsonProperty("iconUrl")
	private String iconUrl;

	@JsonProperty("label")
	private String label;

	@JsonProperty("type")
	private String type;

	@JsonProperty("class")
	private String className;

	@JsonProperty("value")
	private String value;

    public SayButton(String subject, String iconUrl, String label, String type, String className, String value) {
        this.subject = subject;
        this.iconUrl = iconUrl;
        this.label = label;
        this.type = type;
        this.className = className;
        this.value = value;
    }

    public String getSubject() {
        return subject;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public String getValue() {
        return value;
    }
}
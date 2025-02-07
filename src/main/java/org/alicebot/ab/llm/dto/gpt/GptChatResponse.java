package org.alicebot.ab.llm.dto.gpt;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GptChatResponse {
	private int created;
	private Usage usage;
	private String model;
	private String id;
	private List<Choice> choices;
	private Object systemFingerprint;
	private String object;

	public int getCreated() {
		return created;
	}

	public void setCreated(int created) {
		this.created = created;
	}

	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Choice> getChoices() {
		return choices;
	}

	public void setChoices(List<Choice> choices) {
		this.choices = choices;
	}

	public Object getSystemFingerprint() {
		return systemFingerprint;
	}

	public void setSystemFingerprint(Object systemFingerprint) {
		this.systemFingerprint = systemFingerprint;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}


	@Override
	public String toString() {
		return "GptChatResponse{" +
				"created=" + created +
				", usage=" + usage +
				", model='" + model + '\'' +
				", id='" + id + '\'' +
				", choices=" + choices +
				", object='" + object + '\'' +
				'}';
	}
}
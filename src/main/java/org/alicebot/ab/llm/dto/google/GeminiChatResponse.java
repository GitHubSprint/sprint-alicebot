package org.alicebot.ab.llm.dto.google;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiChatResponse {
	private Metadata metadata;
	private List<Predictions> predictions;

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public List<Predictions> getPredictions() {
		return predictions;
	}

	public void setPredictions(List<Predictions> predictions) {
		this.predictions = predictions;
	}

	@Override
	public String toString() {
		return "GeminiChatResponse{" +
				"metadata=" + metadata +
				", predictions=" + predictions +
				'}';
	}
}
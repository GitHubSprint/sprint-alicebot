package org.alicebot.ab.llm.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenMetadata{
	private InputTokenCount inputTokenCount;
	private OutputTokenCount outputTokenCount;

	public InputTokenCount getInputTokenCount() {
		return inputTokenCount;
	}

	public void setInputTokenCount(InputTokenCount inputTokenCount) {
		this.inputTokenCount = inputTokenCount;
	}

	public OutputTokenCount getOutputTokenCount() {
		return outputTokenCount;
	}

	public void setOutputTokenCount(OutputTokenCount outputTokenCount) {
		this.outputTokenCount = outputTokenCount;
	}

	@Override
	public String toString() {
		return "TokenMetadata{" +
				"inputTokenCount=" + inputTokenCount +
				", outputTokenCount=" + outputTokenCount +
				'}';
	}
}
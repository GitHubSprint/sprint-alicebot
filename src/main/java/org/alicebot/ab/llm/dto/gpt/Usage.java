package org.alicebot.ab.llm.dto.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Usage{
	private int completionTokens;
	private int promptTokens;
	private int totalTokens;

	public int getCompletionTokens() {
		return completionTokens;
	}

	public void setCompletionTokens(int completionTokens) {
		this.completionTokens = completionTokens;
	}

	public int getPromptTokens() {
		return promptTokens;
	}

	public void setPromptTokens(int promptTokens) {
		this.promptTokens = promptTokens;
	}

	public int getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(int totalTokens) {
		this.totalTokens = totalTokens;
	}

	@Override
	public String toString() {
		return "Usage{" +
				"completionTokens=" + completionTokens +
				", promptTokens=" + promptTokens +
				", totalTokens=" + totalTokens +
				'}';
	}
}
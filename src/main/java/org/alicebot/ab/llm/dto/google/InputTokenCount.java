package org.alicebot.ab.llm.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InputTokenCount{
	private Integer totalBillableCharacters;
	private Integer totalTokens;

	@Override
	public String toString() {
		return "InputTokenCount{" +
				"totalBillableCharacters=" + totalBillableCharacters +
				", totalTokens=" + totalTokens +
				'}';
	}

	public Integer getTotalBillableCharacters() {
		return totalBillableCharacters;
	}

	public void setTotalBillableCharacters(Integer totalBillableCharacters) {
		this.totalBillableCharacters = totalBillableCharacters;
	}

	public Integer getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(Integer totalTokens) {
		this.totalTokens = totalTokens;
	}
}
package org.alicebot.ab.llm.dto.google;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CitationMetadata {
	private List<Object> citations;

	public List<Object> getCitations() {
		return citations;
	}

	public void setCitations(List<Object> citations) {
		this.citations = citations;
	}
}
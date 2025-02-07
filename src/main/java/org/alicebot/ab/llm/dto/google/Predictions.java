package org.alicebot.ab.llm.dto.google;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Predictions {
	private List<Candidates> candidates;
	private List<GroundingMetadata> groundingMetadata;
	private List<CitationMetadata> citationMetadata;
	private List<SafetyAttributes> safetyAttributes;

	public List<Candidates> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<Candidates> candidates) {
		this.candidates = candidates;
	}

	public List<GroundingMetadata> getGroundingMetadata() {
		return groundingMetadata;
	}

	public void setGroundingMetadata(List<GroundingMetadata> groundingMetadata) {
		this.groundingMetadata = groundingMetadata;
	}

	public List<CitationMetadata> getCitationMetadata() {
		return citationMetadata;
	}

	public void setCitationMetadata(List<CitationMetadata> citationMetadata) {
		this.citationMetadata = citationMetadata;
	}

	public List<SafetyAttributes> getSafetyAttributes() {
		return safetyAttributes;
	}

	public void setSafetyAttributes(List<SafetyAttributes> safetyAttributes) {
		this.safetyAttributes = safetyAttributes;
	}

	@Override
	public String toString() {
		return "Predictions{" +
				"candidates=" + candidates +
				", groundingMetadata=" + groundingMetadata +
				", citationMetadata=" + citationMetadata +
				", safetyAttributes=" + safetyAttributes +
				'}';
	}
}
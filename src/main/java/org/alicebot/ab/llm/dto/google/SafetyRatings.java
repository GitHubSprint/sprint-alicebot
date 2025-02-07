package org.alicebot.ab.llm.dto.google;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SafetyRatings {
	private String severity;
	private Double probabilityScore;
	private String category;
	private Double severityScore;

	@Override
	public String toString() {
		return "SafetyRatings{" +
				"severity='" + severity + '\'' +
				", probabilityScore=" + probabilityScore +
				", category='" + category + '\'' +
				", severityScore=" + severityScore +
				'}';
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public Double getProbabilityScore() {
		return probabilityScore;
	}

	public void setProbabilityScore(Double probabilityScore) {
		this.probabilityScore = probabilityScore;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Double getSeverityScore() {
		return severityScore;
	}

	public void setSeverityScore(Double severityScore) {
		this.severityScore = severityScore;
	}
}
package org.alicebot.ab.llm.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SafetyAttributes {
	private Boolean blocked;
	private List<Double> scores;
	private List<SafetyRatings> safetyRatings;
	private List<String> categories;

	@Override
	public String toString() {
		return "SafetyAttributes{" +
				"blocked=" + blocked +
				", scores=" + scores +
				", safetyRatings=" + safetyRatings +
				", categories=" + categories +
				'}';
	}

	public Boolean getBlocked() {
		return blocked;
	}

	public void setBlocked(Boolean blocked) {
		this.blocked = blocked;
	}

	public List<Double> getScores() {
		return scores;
	}

	public void setScores(List<Double> scores) {
		this.scores = scores;
	}

	public List<SafetyRatings> getSafetyRatings() {
		return safetyRatings;
	}

	public void setSafetyRatings(List<SafetyRatings> safetyRatings) {
		this.safetyRatings = safetyRatings;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
}
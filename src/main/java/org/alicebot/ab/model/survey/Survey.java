package org.alicebot.ab.model.survey;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Survey{

	@JsonProperty("subject")
	private String subject;

	@JsonProperty("questions")
	private List<Question> questions;

	@JsonProperty("description")
	private String description;

	public void setSubject(String subject){
		this.subject = subject;
	}

	public String getSubject(){
		return subject;
	}

	public void setQuestions(List<Question> questions){
		this.questions = questions;
	}

	public List<Question> getQuestions(){
		return questions;
	}

	public void setDescription(String description){
		this.description = description;
	}

	public String getDescription(){
		return description;
	}
}
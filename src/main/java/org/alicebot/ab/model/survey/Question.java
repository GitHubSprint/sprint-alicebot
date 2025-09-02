package org.alicebot.ab.model.survey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {

	@JsonProperty("question")
	private String question;

	@JsonProperty("id")
	private String id;

	@JsonProperty("placeholder")
	private String placeholder;

	@JsonProperty("type")
	private String type;

	@JsonProperty("class")
	private String className;

	@JsonProperty("answers")
	private List<String> answers;

	public void setQuestion(String question){
		this.question = question;
	}

	public String getQuestion(){
		return question;
	}

	public void setId(String id){
		this.id = id;
	}

	public String getId(){
		return id;
	}

	public void setPlaceholder(String placeholder){
		this.placeholder = placeholder;
	}

	public String getPlaceholder(){
		return placeholder;
	}

	public void setType(String type){
		this.type = type;
	}

	public String getType(){
		return type;
	}

    public void setClassName(String className) {
        this.className = className;
    }

    public void setAnswers(List<String> answers){
		this.answers = answers;
	}

	public List<String> getAnswers(){
		return answers;
	}
}
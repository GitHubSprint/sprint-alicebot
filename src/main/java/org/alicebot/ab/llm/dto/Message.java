package org.alicebot.ab.llm.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message{
	private String role;
	private String content;

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Message(String role, String content) {
		this.role = role;
		this.content = content;
	}

	public Message() {
	}

	@Override
	public String toString() {
		return "Message{" +
				"role='" + role + '\'' +
				", content='" + content + '\'' +
				'}';
	}
}
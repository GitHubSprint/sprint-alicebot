package org.alicebot.ab.llm.dto.gpt;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.alicebot.ab.llm.dto.Message;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {
	private String finishReason;
	private int index;
	private Message message;
	private Object logprobs;

	public String getFinishReason() {
		return finishReason;
	}

	public void setFinishReason(String finishReason) {
		this.finishReason = finishReason;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public Object getLogprobs() {
		return logprobs;
	}

	public void setLogprobs(Object logprobs) {
		this.logprobs = logprobs;
	}

	@Override
	public String toString() {
		return "Choice{" +
				"finishReason='" + finishReason + '\'' +
				", index=" + index +
				", message=" + message +
				'}';
	}
}
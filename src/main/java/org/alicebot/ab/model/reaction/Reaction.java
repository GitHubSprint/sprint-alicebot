package org.alicebot.ab.model.reaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Reaction{

	@JsonProperty("type")
	private String type;

    public Reaction(String type) {
        this.type = type;
    }

    public String getType(){
		return type;
	}
}
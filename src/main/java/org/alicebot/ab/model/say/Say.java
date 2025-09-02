package org.alicebot.ab.model.say;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Say {

	@JsonProperty("buttons")
	private List<SayButton> sayButtons;

    public Say(List<SayButton> sayButtons) {
        this.sayButtons = sayButtons;
    }

    public List<SayButton> getButtons(){
		return sayButtons;
	}
}
package org.alicebot.ab.model.say;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Say {
    @JsonProperty("buttons")
    List<SayButton> sayButtons;

    public Say() {
    }

    public Say(List<SayButton> sayButtons) {
        this.sayButtons = sayButtons;
    }

    public List<SayButton> getSayButtons() {
        return sayButtons;
    }

    public void setSayButtons(List<SayButton> sayButtons) {
        this.sayButtons = sayButtons;
    }

    @Override
    public String toString() {
        return "Say{" +
                "sayButtons=" + sayButtons +
                '}';
    }
}
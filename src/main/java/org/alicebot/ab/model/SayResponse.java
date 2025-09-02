package org.alicebot.ab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.alicebot.ab.model.reaction.Reaction;
import org.alicebot.ab.model.say.Say;
import org.alicebot.ab.model.survey.Survey;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SayResponse {
    private Reaction reaction;
    private Say say;
    private Survey survey;

    public SayResponse(Reaction reaction, Say say, Survey survey) {
        this.reaction = reaction;
        this.say = say;
        this.survey = survey;
    }

    public SayResponse(Reaction reaction) {
        this.reaction = reaction;
    }

    public SayResponse(Say say) {
        this.say = say;
    }

    public SayResponse(Survey survey) {
        this.survey = survey;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public Say getSay() {
        return say;
    }

    public Survey getSurvey() {
        return survey;
    }
}

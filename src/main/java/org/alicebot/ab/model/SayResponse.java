package org.alicebot.ab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.alicebot.ab.model.feedback.Feedback;
import org.alicebot.ab.model.say.Say;
import org.alicebot.ab.model.survey.Survey;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SayResponse {
    private Feedback feedback;
    private Say say;
    private Survey survey;

    public SayResponse(Feedback feedback, Say say, Survey survey) {
        this.feedback = feedback;
        this.say = say;
        this.survey = survey;
    }

    public SayResponse(Feedback feedback) {
        this.feedback = feedback;
    }

    public SayResponse(Say say) {
        this.say = say;
    }

    public SayResponse(Survey survey) {
        this.survey = survey;
    }

    public Feedback getReaction() {
        return feedback;
    }

    public Say getSay() {
        return say;
    }

    public Survey getSurvey() {
        return survey;
    }
}

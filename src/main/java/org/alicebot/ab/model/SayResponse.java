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
    private String text;

    public SayResponse() {
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

    public SayResponse(String text) {
        this.text = text;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public Say getSay() {
        return say;
    }

    public void setSay(Say say) {
        this.say = say;
    }

    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    @Override
    public String toString() {
        return "SayResponse{" +
                "feedback=" + feedback +
                ", say=" + say +
                ", survey=" + survey +
                ", text='" + text + '\'' +
                '}';
    }
}

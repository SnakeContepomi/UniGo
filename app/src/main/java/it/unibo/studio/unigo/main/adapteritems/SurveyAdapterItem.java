package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.Survey;

public class SurveyAdapterItem
{
    private Survey survey;
    private String survey_key;

    public SurveyAdapterItem(String questionKey, Survey survey)
    {
        this.survey = survey;
        this.survey_key = questionKey;
    }

    public Survey getSurvey()
    {
        return survey;
    }

    public String getSurveyKey()
    {
        return survey_key;
    }
}
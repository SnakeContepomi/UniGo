package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.Question;

public class QuestionAdapterItem
{
    private Question question;
    private String question_key;

    public QuestionAdapterItem(Question question, String questionKey)
    {
        this.question = question;
        this.question_key = questionKey;
    }

    public Question getQuestion()
    {
        return question;
    }

    public String getQuestionKey()
    {
        return question_key;
    }
}
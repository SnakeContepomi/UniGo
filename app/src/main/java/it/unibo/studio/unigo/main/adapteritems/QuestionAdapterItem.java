package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.Question;

public class QuestionAdapterItem
{
    private Question question;
    private String question_key, photo;

    public QuestionAdapterItem(Question question, String questionKey, String photoUrl)
    {
        this.question = question;
        this.question_key = questionKey;
        this.photo = photoUrl;
    }

    public Question getQuestion()
    {
        return question;
    }

    public String getQuestionKey()
    {
        return question_key;
    }

    public String getPhotoUrl()
    {
        return photo;
    }
}
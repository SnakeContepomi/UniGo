package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.Answer;

public class DetailAdapterItem
{
    private Answer answer;
    private String answer_key, photo;

    public DetailAdapterItem(Answer answer, String answer_key, String photo)
    {
        this.answer = answer;
        this.answer_key = answer_key;
        this.photo = photo;
    }

    public Answer getAnswer()
    {
        return answer;
    }

    public String getAnswerKey()
    {
        return answer_key;
    }

    public String getPhoto()
    {
        return photo;
    }
}
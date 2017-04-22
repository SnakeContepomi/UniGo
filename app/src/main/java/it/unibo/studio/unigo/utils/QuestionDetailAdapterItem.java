package it.unibo.studio.unigo.utils;

import it.unibo.studio.unigo.utils.firebase.Answer;

public class QuestionDetailAdapterItem
{
    public Answer answer;
    public String photo;

    public QuestionDetailAdapterItem() { }

    public QuestionDetailAdapterItem(Answer answer, String photo)
    {
        this.answer = answer;
        this.photo = photo;
    }

    public Answer getAnswer()
    {
        return answer;
    }

    public String getPhoto()
    {
        return photo;
    }
}
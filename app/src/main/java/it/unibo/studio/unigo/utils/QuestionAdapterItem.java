package it.unibo.studio.unigo.utils;

import android.graphics.Bitmap;
import it.unibo.studio.unigo.utils.firebase.Question;

public class QuestionAdapterItem
{
    private Question question;
    private String photo;

    public QuestionAdapterItem(Question question, String photoUrl)
    {
        this.question = question;
        this.photo = photoUrl;
    }

    public Question getQuestion()
    {
        return question;
    }

    public String getPhotoUrl()
    {
        return photo;
    }
}
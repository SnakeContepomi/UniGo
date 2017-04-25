package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.Answer;

public class DetailAdapterItem
{
    public Answer answer;
    public String photo;

    public DetailAdapterItem() { }

    public DetailAdapterItem(Answer answer, String photo)
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
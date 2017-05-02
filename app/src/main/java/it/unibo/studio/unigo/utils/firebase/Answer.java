package it.unibo.studio.unigo.utils.firebase;

import java.util.HashMap;

import it.unibo.studio.unigo.utils.Util;

public class Answer
{
    public String user_key;
    public String desc;
    public String date;
    public HashMap<String, Boolean> likes;

    public Answer() { }

    public Answer(String user_key, String desc)
    {
        this.user_key = user_key;
        this.date = Util.getDate();
        this.desc = desc;
    }
}
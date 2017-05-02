package it.unibo.studio.unigo.utils.firebase;

import it.unibo.studio.unigo.utils.Util;

public class Comment
{
    public String user_key, desc, date;

    public Comment() { }

    public Comment(String user_key, String desc)
    {
        this.user_key = user_key;
        this.desc = desc;
        this.date = Util.getDate();
    }
}
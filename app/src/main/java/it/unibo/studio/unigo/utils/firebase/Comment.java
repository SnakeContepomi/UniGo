package it.unibo.studio.unigo.utils.firebase;

public class Comment
{
    public String user_key, desc, date;

    public Comment() { }

    public Comment(String user_key, String desc, String date)
    {
        this.user_key = user_key;
        this.desc = desc;
        this.date = date;
    }
}
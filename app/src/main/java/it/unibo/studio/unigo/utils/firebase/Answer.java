package it.unibo.studio.unigo.utils.firebase;

public class Answer
{
    public String user_key;
    public String date;
    public String desc;

    public Answer() { }

    public Answer(String user_key, String date, String desc)
    {
        this.user_key = user_key;
        this.date = date;
        this.desc = desc;
    }
}
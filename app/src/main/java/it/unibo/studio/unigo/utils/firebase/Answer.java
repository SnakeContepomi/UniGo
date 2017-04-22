package it.unibo.studio.unigo.utils.firebase;

public class Answer
{
    public String desc;
    public String user_key;

    public Answer() { }

    public Answer(String user_key, String desc)
    {
        this.user_key = user_key;
        this.desc = desc;
    }
}
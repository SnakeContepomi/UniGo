package it.unibo.studio.unigo.utils.firebase;

import it.unibo.studio.unigo.utils.Util;

public class Question
{
    public String title;
    public String course;
    public String desc;
    public String date;
    public String user_key;
    public String course_key;

    public Question() { }

    public Question(String title, String course, String desc, String user_key, String course_key)
    {
        this.title = title;
        this.course = course;
        this.desc = desc;
        this.date = Util.getDate();
        this.user_key = user_key;
        this.course_key = course_key;
    }
}
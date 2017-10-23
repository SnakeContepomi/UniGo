package it.unibo.studio.unigo.utils.firebase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import it.unibo.studio.unigo.utils.Util;

public class Survey
{
    public String title;
    public String desc;
    public String date;
    public String user_key;
    public String course_key;
    public HashMap<String, List<String>> choices;

    public Survey() { }

    public Survey(String title, String desc, String user_key, String course_key)
    {
        this.title = title;
        this.desc = desc;
        this.date = Util.getDate();
        this.user_key = user_key;
        this.course_key = course_key;
    }
}
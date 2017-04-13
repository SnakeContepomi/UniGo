package it.unibo.studio.unigo.utils.firebase;

import java.util.HashMap;

public class Course
{
    public String name;
    public String school_key;
    public HashMap<String, Boolean> users;
    public HashMap<String, String> questions;

    public Course() { }

    public Course(String name, String school_key)
    {
        this.name = name;
        this.school_key = school_key;
    }
}

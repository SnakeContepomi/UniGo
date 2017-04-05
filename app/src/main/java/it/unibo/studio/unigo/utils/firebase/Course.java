package it.unibo.studio.unigo.utils.firebase;

public class Course
{
    public String name;
    public String school_key;

    public Course() { }

    public Course(String name, String school_key)
    {
        this.name = name;
        this.school_key = school_key;
    }
}

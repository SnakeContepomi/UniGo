package it.unibo.studio.unigo.utils.firebase;

import java.util.HashMap;

import it.unibo.studio.unigo.utils.Util;

public class User
{
    public String email, name, lastName, phone, city, courseKey;
    public int exp, credits;
    public HashMap<String, Boolean> questions;

    public User() { }

    public User(String email, String name, String lastName, String phone, String city, String courseKey)
    {
        this.email = email;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.city = city;
        this.courseKey = courseKey;
        this.exp = Util.EXP_START;
        this.credits = Util.CREDITS_START;
    }
}

package it.unibo.studio.unigo.utils.firebase;

import java.util.HashMap;

import it.unibo.studio.unigo.utils.Util;

public class User
{
    public String photoUrl, name, lastName, phone, city, courseKey;
    public int exp, credits;
    public HashMap<String, Boolean> questions;
    public HashMap<String, Boolean> favorites;

    public User() { }

    public User(String photoUrl, String name, String lastName, String phone, String city, String courseKey)
    {
        this.photoUrl = photoUrl;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.city = city;
        this.courseKey = courseKey;
        this.exp = Util.EXP_START;
        this.credits = Util.CREDITS_START;
    }
}

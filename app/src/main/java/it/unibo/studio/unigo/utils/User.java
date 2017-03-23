package it.unibo.studio.unigo.utils;

public class User
{
    String email, name, lastName, phone, city, courseKey;
    int exp, credits;

    public User() { }

    public User(String email, String name, String lastName, String phone, String city, String courseKey)
    {
        this.email = email;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.city = city;
        this.courseKey = courseKey;
        this.exp = 0;
        this.credits = 50;
    }
}
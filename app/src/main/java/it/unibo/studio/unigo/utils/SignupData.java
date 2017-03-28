package it.unibo.studio.unigo.utils;

import android.graphics.Bitmap;

// Classe statica che memorizza temporaneamente le informazioni inserite dall'utente durante la registrazione
public class SignupData
{
    private static String email, password, name, last_name, phone, city, course_key;
    private static Bitmap profile_pic;

    public static String getEmail()
    {
        return email;
    }

    public static String getPassword()
    {
        return password;
    }

    public static Bitmap getProfilePic()
    {
        return profile_pic;
    }

    public static String getName()
    {
        return name;
    }

    public static String getLastName()
    {
        return last_name;
    }

    public static String getPhone()
    {
        return phone;
    }

    public static String getCity()
    {
        return city;
    }

    public static String getCourseKey()
    {
        return course_key;
    }

    public static void setEmail(String s_email)
    {
        email = s_email;
    }

    public static void setPassword(String s_password)
    {
        password = s_password;
    }

    public static void setProfilePic(Bitmap is_profile_pic)
    {
        profile_pic = is_profile_pic;
    }

    public static void setName(String s_name)
    {
        name = s_name;
    }

    public static void setLastName(String s_lastName)
    {
        last_name = s_lastName;
    }

    public static void setPhone(String s_phone)
    {
        phone = s_phone;
    }

    public static void setCity(String s_city)
    {
        city = s_city;
    }

    public static void setCourseKey(String s_key)
    {
        course_key = s_key;
    }

    public static void clear()
    {
        email = null;
        password = null;
        profile_pic = null;
        name = null;
        last_name = null;
        phone = null;
        city = null;
        course_key = null;
    }
}
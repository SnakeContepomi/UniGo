package it.unibo.studio.unigo.utils;

public class SignupData
{
    private static String email, password, name, lastName, phone, city, school_key;

    public static String getEmail()
    {
        return email;
    }

    public static String getPassword()
    {
        return password;
    }

    public static String getName()
    {
        return name;
    }

    public static String getLastName()
    {
        return lastName;
    }

    public static String getPhone()
    {
        return phone;
    }

    public static String getCity()
    {
        return city;
    }

    public static String getSchoolKey()
    {
        return school_key;
    }

    public static void setEmail(String s_email)
    {
        email = s_email;
    }

    public static void setPassword(String s_password)
    {
        password = s_password;
    }

    public static void setName(String s_name)
    {
        name = s_name;
    }

    public static void setLastName(String s_lastName)
    {
        lastName = s_lastName;
    }

    public static void setPhone(String s_phone)
    {
        phone = s_phone;
    }

    public static void setCity(String s_city)
    {
        city = s_city;
    }

    public static void setSchoolKey(String s_key)
    {
        school_key = s_key;
    }

    public static void clear()
    {
        email = null;
        password = null;
        name = null;
        lastName = null;
        phone = null;
        city = null;
        school_key = null;
    }
}
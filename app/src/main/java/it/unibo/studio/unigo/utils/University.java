package it.unibo.studio.unigo.utils;

import java.util.Map;

public class University
{
    public String name;
    public String region;
    public Map<String, Boolean> schools;

    public University()
    {    }

    public University(String region, String name)
    {
        this.region = region;
        this.name = name;
    }
}
package it.unibo.studio.unigo.utils.firebase;

public class University
{
    public String name;
    public String region;

    public University() { }

    public University(String region, String name)
    {
        this.region = region;
        this.name = name;
    }
}
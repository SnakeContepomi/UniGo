package it.unibo.studio.unigo.utils.firebase;

public class Message
{
    public String sender_id;
    public String message;
    public String date;

    public Message() { }

    public Message(String sender_id, String message, String date)
    {
        this.sender_id = sender_id;
        this.message = message;
        this.date = date;
    }
}
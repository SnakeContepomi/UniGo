package it.unibo.studio.unigo.utils.firebase;

import java.util.HashMap;

public class ChatRoom
{
    public String id_1, id_2;
    public String name_1, name_2;
    public String photo_url_1, photo_url_2;
    public String last_message;
    public String last_message_id;
    public String last_time;
    public int msg_unread_1, msg_unread_2;
    public HashMap<String, Message> messages;

    public ChatRoom() { }

    public ChatRoom(String id_1, String id_2, String name_1, String name_2, String photo_url_1, String photo_url_2)
    {
        this.id_1 = id_1;
        this.id_2 = id_2;
        this.name_1 = name_1;
        this.name_2 = name_2;
        this.photo_url_1 = photo_url_1;
        this.photo_url_2 = photo_url_2;
    }
}
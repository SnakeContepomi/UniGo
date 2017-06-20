package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.Chat;

public class ChatRoomAdapterItem
{
    private Chat chat;
    private String chat_key;

    public ChatRoomAdapterItem(Chat chat, String chatKey)
    {
        this.chat = chat;
        this.chat_key = chatKey;
    }

    public Chat getChat()
    {
        return chat;
    }

    public String getChatKey()
    {
        return chat_key;
    }
}

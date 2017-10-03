package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.ChatRoom;

public class ChatRoomAdapterItem
{
    private ChatRoom chatRoom;
    private String chat_key;

    public ChatRoomAdapterItem(ChatRoom chatRoom, String chatKey)
    {
        this.chatRoom = chatRoom;
        this.chat_key = chatKey;
    }

    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    public String getChatKey()
    {
        return chat_key;
    }
}
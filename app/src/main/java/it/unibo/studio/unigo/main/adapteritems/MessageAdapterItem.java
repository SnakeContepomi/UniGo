package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.Message;

public class MessageAdapterItem
{
    private Message message;
    private String msgKey;

    public MessageAdapterItem(Message message, String msgKey)
    {
        this.message = message;
        this.msgKey = msgKey;
    }

    public Message getMessage()
    {
        return message;
    }

    public String getMsgKey()
    {
        return msgKey;
    }
}
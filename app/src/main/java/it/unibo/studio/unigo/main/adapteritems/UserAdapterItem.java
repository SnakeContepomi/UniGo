package it.unibo.studio.unigo.main.adapteritems;

import it.unibo.studio.unigo.utils.firebase.User;

public class UserAdapterItem
{
    private User user;
    private String userKey;

    public UserAdapterItem(User user, String userKey)
    {
        this.user = user;
        this.userKey = userKey;
    }

    public User getUser()
    {
        return user;
    }

    public String getUserKey()
    {
        return userKey;
    }
}

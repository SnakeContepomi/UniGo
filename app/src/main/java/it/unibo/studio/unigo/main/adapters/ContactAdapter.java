package it.unibo.studio.unigo.main.adapters;

import android.content.Intent;
import android.view.View;
import java.util.List;
import it.unibo.studio.unigo.main.ChatActivity;
import it.unibo.studio.unigo.main.adapteritems.UserAdapterItem;

public class ContactAdapter extends UserAdapter
{
    public ContactAdapter(List<UserAdapterItem> userList)
    {
        super(userList);
    }

    @Override
    public void onBindViewHolder(final UserHolder holder, int position)
    {
        super.onBindViewHolder(holder, position);
        // ClickListener
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                holder.context.startActivity(new Intent(holder.context, ChatActivity.class).putExtra("user_key", userList.get(holder.getAdapterPosition()).getUserKey()));
            }
        });
    }
}
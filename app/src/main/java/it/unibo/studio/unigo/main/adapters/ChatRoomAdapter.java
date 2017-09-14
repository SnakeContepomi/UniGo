package it.unibo.studio.unigo.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import cn.nekocode.badge.BadgeDrawable;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.ChatActivity;
import it.unibo.studio.unigo.main.adapteritems.ChatRoomAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.ChatRoom;
import static it.unibo.studio.unigo.R.layout.chat_room_item;
import static it.unibo.studio.unigo.utils.Util.NEW_MSG;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder>
{
    private List<ChatRoomAdapterItem> chatList;
    private Activity activity;
    private BadgeDrawable unreadMsgBadge;

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        Context context;
        LinearLayout layout;
        TextView txtName, txtDate, txtLastMessage;
        MaterialLetterIcon userPhoto;
        ImageView imgBadge;

        ViewHolder(LinearLayout v)
        {
            super(v);
            layout = v;
            context = v.getContext();
            txtName = (TextView) v.findViewById(R.id.chat_name);
            txtDate = (TextView) v.findViewById(R.id.chat_date);
            txtLastMessage = (TextView) v.findViewById(R.id.chat_lastMessage);
            userPhoto = (MaterialLetterIcon ) v.findViewById(R.id.chat_userPhoto);
            imgBadge = (ImageView) v.findViewById(R.id.chat_imgBadge);
        }
    }

    public ChatRoomAdapter(Activity activity)
    {
        this.chatList = new ArrayList<>();
        this.activity = activity;
        unreadMsgBadge =
            new BadgeDrawable.Builder()
                    .type(BadgeDrawable.TYPE_NUMBER)
                    .badgeColor(ContextCompat.getColor(activity.getApplicationContext(), R.color.colorPrimary))
                    .textSize(sp2px(activity.getApplicationContext(), 8))
                    .build();
    }

    @Override
    public ChatRoomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(chat_room_item, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public int getItemCount()
    {
        return chatList.size();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        ChatRoom chatRoom = chatList.get(position).getChatRoom();
        // Nome e foto profilo dell'utente destinatario
        final String id, name, photoUrl;

        // Se l'utilizzatore è l'utente "1", allora verranno caricate le informazioni del destinatario 'utente_2'
        if (chatRoom.id_1.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
        {
            id = chatRoom.id_2;
            name = chatRoom.name_2;
            photoUrl = chatRoom.photo_url_2;
            // Viene impostato il numero di messaggi non letti, qualora ve ne fossero
            showUnreadMessagesNumber(chatRoom.msg_unread_1, holder);
        }
        else
        {
            id = chatRoom.id_1;
            name = chatRoom.name_1;
            photoUrl = chatRoom.photo_url_1;
            showUnreadMessagesNumber(chatRoom.msg_unread_2, holder);
        }

        // Se non è presente una connessione o l'utente non ha impostato un'immagine profilo, viene visualizzata la lettera corrispondente al nome utente
        if (!Util.isNetworkAvailable(holder.context) || photoUrl.equals(holder.context.getResources().getString(R.string.empty_profile_pic_url)))
        {
            holder.userPhoto.setLetter(name);
            holder.userPhoto.setShapeColor(Util.getLetterBackgroundColor(holder.context, name));
        }
        else
            Picasso.with(holder.userPhoto.getContext()).load(photoUrl).placeholder(R.drawable.empty_profile_pic).fit().into(holder.userPhoto);

        holder.txtName.setText(name);
        holder.txtDate.setText(Util.formatDate(chatRoom.last_time));
        holder.txtLastMessage.setText(chatRoom.last_message);

        // Al click viene aperta l'activity contenente la chatRoom
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(holder.context, ChatActivity.class);
                intent.putExtra("user_key", id);
                activity.startActivity(intent);

                holder.txtLastMessage.setTypeface(null, Typeface.NORMAL);
                holder.txtDate.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(holder.context, R.color.md_grey_500)));
                holder.imgBadge.setImageDrawable(null);
            }
        });
    }

    // Aggiornamento parziale di uno o più elementi della recyclerview in realtime (rating, commenti, favourite)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position, List<Object> payloads)
    {
        // Non sono presenti nuovi messaggi
        if (payloads.isEmpty())
            onBindViewHolder(holder, position);

        // Sono presenti nuovi messaggi nella conversazione
        else
        {
            if ((int) payloads.get(0) == NEW_MSG)
            {
                // Viene recuperato l'oggetto interessato all'aggiornamento in modo da cambiare solamente i campi da aggiornare,
                // senza quindi ricaricare un intero oggetto
                final ChatRoomAdapterItem chat = chatList.get(position);
                Util.getDatabase().getReference("ChatRoom").child(chat.getChatKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        ChatRoom newItem = dataSnapshot.getValue(ChatRoom.class);

                        chatList.remove(position);
                        notifyItemRemoved(position);

                        holder.txtLastMessage.setText(newItem.last_message);
                        holder.txtDate.setText(Util.formatDate(newItem.last_time));

                        if (Util.encodeEmail(Util.getCurrentUser().getEmail()).equals(newItem.id_1))
                            showUnreadMessagesNumber(newItem.msg_unread_1, holder);
                        else
                            showUnreadMessagesNumber(newItem.msg_unread_2, holder);

                        chatList.add(0, new ChatRoomAdapterItem(newItem, chat.getChatKey()));
                        notifyItemInserted(0);

                        // Per rimuovere l'animazione "pop-out - pop-in" della conversazione aggiornata,
                        // utilizzare il metodo notifyDataSetChanged() al posto di ItemRemoved + ItemInserted
                        //notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        }
    }

    private int sp2px(Context context, float spValue)
    {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public int getPositionByKey(String chatKey)
    {
        for(int i = 0; i < chatList.size(); i++)
            if (chatList.get(i).getChatKey().equals(chatKey))
                return i;
        return -1;
    }

    // Metodo che permette di impostare un badge di notifica per evidenziare il numero di messaggi non letti di una conversazione
    private void showUnreadMessagesNumber(int msgUnread, ViewHolder holder)
    {
        if (msgUnread != 0)
        {
            holder.txtLastMessage.setTypeface(null, Typeface.BOLD);
            holder.txtDate.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(holder.context, R.color.colorPrimary)));
            unreadMsgBadge =
                    new BadgeDrawable.Builder()
                            .type(BadgeDrawable.TYPE_NUMBER)
                            .badgeColor(ContextCompat.getColor(activity.getApplicationContext(), R.color.colorPrimary))
                            .textSize(sp2px(activity.getApplicationContext(), 8))
                            .number(msgUnread)
                            .build();
            holder.imgBadge.setImageDrawable(unreadMsgBadge);
        }
        else
        {
            holder.txtLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.txtDate.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(holder.context, R.color.md_grey_500)));
            holder.imgBadge.setImageDrawable(null);
        }
    }

    public void addElement(ChatRoomAdapterItem chatRoom)
    {
        int pos;

        for(pos = 0; pos < chatList.size(); pos++)
            if (chatRoom.getChatRoom().last_time.compareTo(chatList.get(pos).getChatRoom().last_time) > 0)
                break;

        chatList.add(pos, chatRoom);
        notifyItemInserted(pos);
    }
}
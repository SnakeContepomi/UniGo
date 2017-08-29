package it.unibo.studio.unigo.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.squareup.picasso.Picasso;
import java.util.List;
import cn.nekocode.badge.BadgeDrawable;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.ChatActivity;
import it.unibo.studio.unigo.main.MainActivity;
import it.unibo.studio.unigo.main.adapteritems.ChatRoomAdapterItem;
import it.unibo.studio.unigo.main.fragments.ChatRoomFragment;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Chat;
import static it.unibo.studio.unigo.R.layout.chat_room_item;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder>
{
    private List<ChatRoomAdapterItem> chatList;
    private Activity activity;

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

    public ChatRoomAdapter(List<ChatRoomAdapterItem> chatList, Activity activity)
    {
        this.chatList = chatList;
        this.activity = activity;
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
        Chat chat = chatList.get(position).getChat();
        // Nome e foto profilo dell'utente destinatario
        final String id, name, photoUrl, lastRead;

        if (chat.id_1.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
        {
            id = chat.id_2;
            name = chat.name_2;
            photoUrl = chat.photo_url_2;
            lastRead = chat.last_read_1;
        }
        else
        {
            id = chat.id_1;
            name = chat.name_1;
            photoUrl = chat.photo_url_1;
            lastRead = chat.last_read_2;
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
        holder.txtDate.setText(Util.formatDate(chat.last_time));
        holder.txtLastMessage.setText(chat.last_message);

        // Al click viene aperta l'activity contenente la chat
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(holder.context, ChatActivity.class);
                intent.putExtra("user_key", id);
                activity.startActivity(intent);
            }
        });

        // Inizializzazione Badge per le notifiche dei messaggi non letti
        final BadgeDrawable unreadMessage =
                new BadgeDrawable.Builder()
                        .type(BadgeDrawable.TYPE_NUMBER)
                        .badgeColor(ContextCompat.getColor(holder.context, R.color.colorPrimary))
                        .textSize(sp2px(holder.context, 8))
                        .build();


        holder.imgBadge.setImageDrawable(unreadMessage);
    }

    // Aggiornamento parziale di uno o più elementi della recyclerview in realtime (rating, commenti, favourite)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position, List<Object> payloads)
    {
        // Non sono presenti nuovi messaggi
        if (payloads.isEmpty())
            onBindViewHolder(holder, position);

        else if (payloads.get(0) instanceof Integer)
            switch ((int) payloads.get(0))
            {
                // Sono presenti nuovi messaggi
                case ChatRoomFragment.CODE_NEW_MESSAGE:
                    // ** Grassetto **
                    holder.txtDate.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(holder.context, R.color.colorPrimary)));
                    break;

                // Non sono presenti nuovi messaggi
                case ChatRoomFragment.CODE_NO_NEW_MESSAGE:
                    // ** NON Grassetto **
                    break;

                default:
                    break;
            }
    }

    private int sp2px(Context context, float spValue)
    {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
}
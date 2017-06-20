package it.unibo.studio.unigo.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.l4digital.fastscroll.FastScroller;
import com.squareup.picasso.Picasso;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.ProfileActivity;
import it.unibo.studio.unigo.main.adapteritems.UserAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> implements FastScroller.SectionIndexer{

    protected List<UserAdapterItem> userList;

    class UserHolder extends RecyclerView.ViewHolder
    {
        Context context;
        LinearLayout layout;
        MaterialLetterIcon imgProfile;
        TextView txtName;

        UserHolder(View v)
        {
            super(v);
            context = v.getContext();
            layout = (LinearLayout) v.findViewById(R.id.itemp_layout);
            imgProfile = (MaterialLetterIcon) v.findViewById(R.id.itemp_photo);
            txtName = (TextView) v.findViewById(R.id.itemp_name);
        }
    }

    public UserAdapter(List<UserAdapterItem> userList)
    {
        this.userList = userList;
    }

    @Override
    public UserHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false);

        return new UserHolder(v);
    }

    @Override
    public void onBindViewHolder(final UserHolder holder, int position)
    {
        final User user = userList.get(position).getUser();

        // Immagine profilo
        if(!Util.isNetworkAvailable(holder.context) || user.photoUrl.equals(holder.context.getResources().getString(R.string.empty_profile_pic_url)))
        {
            holder.imgProfile.setLetter(user.name);
            holder.imgProfile.setShapeColor(Util.getLetterBackgroundColor(holder.context, user.name));
        }
        else
            Picasso.with(holder.context).load(user.photoUrl).placeholder(R.drawable.empty_profile_pic).fit().into(holder.imgProfile);

        // Nome e cognome
        holder.txtName.setText(user.name + " " + user.lastName);

        // ClickListener
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                holder.context.startActivity(new Intent(holder.context, ProfileActivity.class).putExtra("user_key", userList.get(holder.getAdapterPosition()).getUserKey()));
            }
        });
    }

    @Override
    public String getSectionText(int position)
    {
        return String.valueOf(userList.get(position).getUser().name.charAt(0));
    }

    @Override
    public int getItemCount()
    {
        return userList.size();
    }
}
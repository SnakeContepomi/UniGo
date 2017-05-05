package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
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
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> implements FastScroller.SectionIndexer{

    private List<User> userList;

    class UserHolder extends RecyclerView.ViewHolder
    {
        Context context;
        MaterialLetterIcon imgProfile;
        TextView txtName;

        UserHolder(View v)
        {
            super(v);
            context = v.getContext();
            imgProfile = (MaterialLetterIcon ) v.findViewById(R.id.itemp_photo);
            txtName = (TextView) v.findViewById(R.id.itemp_name);
        }
    }

    public UserAdapter(List<User> userList)
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
    public void onBindViewHolder(UserHolder holder, int position)
    {
        final User user = userList.get(position);
        if(!Util.isNetworkAvailable(holder.context) || user.photoUrl.equals(holder.context.getResources().getString(R.string.empty_profile_pic_url)))
        {
            holder.imgProfile.setLetter(user.name);
            holder.imgProfile.setShapeColor(Util.getLetterBackgroundColor(holder.context, user.name));
        }
        else
            Picasso.with(holder.context).load(user.photoUrl).fit().into(holder.imgProfile);

        holder.txtName.setText(user.name + " " + user.lastName);
    }

    @Override
    public String getSectionText(int position)
    {
        return String.valueOf(userList.get(position).name.charAt(0));
    }

    @Override
    public int getItemCount()
    {
        return userList.size();
    }
}
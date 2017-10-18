package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.l4digital.fastscroll.FastScroller;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.ProfileActivity;
import it.unibo.studio.unigo.main.adapteritems.UserAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> implements FastScroller.SectionIndexer, Filterable
{
    private Filter mFilter = new ItemFilter();
    protected List<UserAdapterItem> userList, backupList;

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
        this.backupList = userList;
    }

    // Classe per filtrare la lista dell'Adapter
    private class ItemFilter extends Filter
    {
        // Il filtro restituisce gli elementi che contengono la chiave di ricerca
        // nei campi Titolo, Descrizione e Materia
        @Override
        protected FilterResults performFiltering(CharSequence constraint)
        {
            String filterString = constraint.toString().toLowerCase();
            userList = backupList;
            List<UserAdapterItem> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            for(int i = 0; i < userList.size(); i++)
                if (userList.get(i).getUser().name.toLowerCase().contains(filterString)
                        || userList.get(i).getUser().lastName.toLowerCase().contains(filterString))
                    filteredList.add(0, userList.get(i));

            results.values = filteredList;
            results.count = filteredList.size();

            return results;
        }

        // Metodo per aggiornare graficamente la lista
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results)
        {
            userList = (ArrayList<UserAdapterItem>) results.values;
            Collections.sort(userList, new Comparator<UserAdapterItem>() {
                @Override
                public int compare(UserAdapterItem qItem1, UserAdapterItem qItem2)
                {
                    return new CompareToBuilder()
                            .append(qItem1.getUser().name, qItem2.getUser().name)
                            .append(qItem1.getUser().lastName, qItem2.getUser().lastName).toComparison();
                }
            });
            notifyDataSetChanged();
        }
    }

    @Override
    public Filter getFilter()
    {
        return mFilter;
    }

    @Override
    public UserHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);

        return new UserHolder(v);
    }

    @Override
    public void onBindViewHolder(final UserHolder holder, int position)
    {
        final User user = userList.get(position).getUser();

        Picasso.with(holder.context).load(user.photoUrl).placeholder(R.drawable.empty_profile_pic).fit().into(holder.imgProfile, new Callback() {
            @Override
            public void onSuccess() { }

            @Override
            public void onError()
            {
                holder.imgProfile.setLetter(user.name);
                holder.imgProfile.setShapeColor(Util.getLetterBackgroundColor(holder.context, user.name));
            }
        });

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
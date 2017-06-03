package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Comment;

import static it.unibo.studio.unigo.R.layout.comment;

class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder>
{
    private List<Comment> commentList;
    private List<String> commentKeyList;

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        Context context;
        TextView txtName, txtDate, txtLvl, txtDesc;
        MaterialLetterIcon imgProfile;

        ViewHolder(View v)
        {
            super(v);

            context = v.getContext();
            txtName = (TextView) v.findViewById(R.id.comment_name);
            txtDate = (TextView) v.findViewById(R.id.comment_date);
            txtLvl = (TextView) v.findViewById(R.id.comment_lvl);
            txtDesc = (TextView) v.findViewById(R.id.comment_desc);
            imgProfile = (MaterialLetterIcon ) v.findViewById(R.id.comment_userPhoto);
        }
    }

    CommentAdapter(List<Comment> commentList, List<String> commentKeyList)
    {
        this.commentList = commentList;
        this.commentKeyList = commentKeyList;
        sortCommentList();
    }

    @Override
    public CommentAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(comment, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        final Comment comment = commentList.get(position);

        // Query per recuperare le informazioni riguardanti l'autore del commento
        Util.getDatabase().getReference("User").child(comment.user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                holder.txtName.setText(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                holder.txtLvl.setText(String.valueOf(dataSnapshot.child("exp").getValue(Integer.class)));
                if (!Util.isNetworkAvailable(holder.context) || dataSnapshot.child("photoUrl").getValue(String.class).equals(holder.context.getResources().getString(R.string.empty_profile_pic_url)))
                {
                    holder.imgProfile.setLetter(dataSnapshot.child("name").getValue(String.class));
                    holder.imgProfile.setShapeColor(Util.getLetterBackgroundColor(holder.context, dataSnapshot.child("name").getValue(String.class)));
                }
                else
                    Picasso.with(holder.context).load(dataSnapshot.child("photoUrl").getValue(String.class)).placeholder(R.drawable.empty_profile_pic).fit().into(holder.imgProfile);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        holder.txtDate.setText(Util.formatDate(comment.date));
        holder.txtDesc.setText(comment.desc);
    }

    @Override
    public int getItemCount()
    {
        return commentList.size();
    }

    // Metodo per aggiornare in tempo reale l'aggiunta di un commento ad una risposta
    void refreshAnswerComments(Comment comment, String commentKey)
    {
        // Aggiornamento grafico del numero di commenti
        // Aggiunta di commento in coda, evitando quelli trigghetari all'avvio del listener di firebase
        if (!commentExists(commentKey))
        {
            commentList.add(comment);
            commentKeyList.add(commentKey);
            notifyItemInserted(commentList.size() - 1);
        }
    }

    // Metodo utilizzato per sapere se il commento passato come parametro è già presente nella lista
    private boolean commentExists(String commentKey)
    {
        for(String key : commentKeyList)
            if (commentKey.equals(key)) return true;
        return false;
    }

    // Metodo per ordinare cronologicamente la lista dei commenti
    private void sortCommentList()
    {
        Collections.sort(commentList, new Comparator<Comment>() {
            @Override
            public int compare(Comment comment1, Comment comment2)
            {
                return new CompareToBuilder().append(comment1.date, comment2.date).toComparison();
            }
        });
    }
}
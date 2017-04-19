package it.unibo.studio.unigo.utils;

import android.content.res.ColorStateList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.List;
import it.unibo.studio.unigo.R;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder>
{
    private List<QuestionAdapterItem> questionList;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder
    {
        // Campi del Recycler Item Question
         TextView txtTitle, txtCourse, txtDesc, txtDate;
        RoundedImageView imgProfile;
        ImageView imgIcon;

        ViewHolder(LinearLayout v)
        {
            super(v);
            txtTitle = (TextView) v.findViewById(R.id.itemq_title);
            txtCourse = (TextView) v.findViewById(R.id.itemq_course);
            txtDesc = (TextView) v.findViewById(R.id.itemq_desc);
            txtDate = (TextView) v.findViewById(R.id.itemq_date);
            imgProfile = (RoundedImageView) v.findViewById(R.id.itemq_user);
            imgIcon = (ImageView) v.findViewById(R.id.itemq_star);
        }
    }

    public QuestionAdapter(List<QuestionAdapterItem> questionList)
    {
        this.questionList = questionList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public QuestionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item_question, parent, false);
        // set the view's size, margins, paddings and layout parameters

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final QuestionAdapterItem q_item = questionList.get(position);
        final DatabaseReference current_question = Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(q_item.getQuestionKey());

        holder.txtTitle.setText(q_item.getQuestion().title);
        holder.txtCourse.setText(q_item.getQuestion().course);
        holder.txtDesc.setText(q_item.getQuestion().desc);
        holder.txtDate.setText(Util.formatDate(q_item.getQuestion().date));
        Picasso.with(holder.imgProfile.getContext()).load(q_item.getPhotoUrl()).fit().into(holder.imgProfile);

        // Viene controllato se la domanda è stata inserita all'interno dei preferiti dell'utente
        current_question.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Se la domanda non risulta nei preferiti, non viene evidenziata
                if (dataSnapshot.getValue() == null)
                {
                    holder.imgIcon.setBackgroundResource(R.drawable.ic_star_border_black_24dp);
                    holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.md_grey_500)));
                }
                // Altrimenti viene evidenziata
                else
                {
                    holder.imgIcon.setBackgroundResource(R.drawable.ic_star_black_24dp);
                    holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.colorYellow)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Pulsante che consente di aggiungere una domanda ai preferiti
        holder.imgIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                current_question.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        // Se la domanda non è nei preferiti, essa viene aggiunta ed evidenziata
                        if (dataSnapshot.getValue() == null)
                        {
                            current_question.setValue(true);
                            holder.imgIcon.setBackgroundResource(R.drawable.ic_star_black_24dp);
                            holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.colorYellow)));
                        }
                        // Se la domanda è già nei preferiti, essa viene rimossa
                        else
                        {
                            current_question.removeValue();
                            holder.imgIcon.setBackgroundResource(R.drawable.ic_star_border_black_24dp);
                            holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.md_grey_500)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return questionList.size();
    }
}
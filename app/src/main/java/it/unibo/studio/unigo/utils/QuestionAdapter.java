package it.unibo.studio.unigo.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.PostActivity;
import it.unibo.studio.unigo.main.QuestionDetailActivity;

import static android.R.attr.data;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder>
{
    private List<QuestionAdapterItem> questionList;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder
    {
        Context context;
        // Campi del Recycler Item Question
        LinearLayout layout;
        TextView txtTitle, txtCourse, txtDesc, txtDate;
        MaterialLetterIcon imgProfile;
        ImageView imgIcon;

        ViewHolder(LinearLayout v)
        {
            super(v);
            context = v.getContext();
            layout = v;
            txtTitle = (TextView) v.findViewById(R.id.itemq_title);
            txtCourse = (TextView) v.findViewById(R.id.itemq_course);
            txtDesc = (TextView) v.findViewById(R.id.itemq_desc);
            txtDate = (TextView) v.findViewById(R.id.itemq_date);
            imgProfile = (MaterialLetterIcon ) v.findViewById(R.id.itemq_user);
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

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(holder.context, QuestionDetailActivity.class);
                Bundle b = new Bundle();
                b.putSerializable("question", q_item.getQuestion());
                b.putString("question_key", q_item.getQuestionKey());
                b.putString("photo_url", q_item.getPhotoUrl());
                intent.putExtras(b);
                holder.context.startActivity(intent);
            }
        });

        holder.txtTitle.setText(q_item.getQuestion().title);
        holder.txtCourse.setText(q_item.getQuestion().course);
        holder.txtDesc.setText(q_item.getQuestion().desc);
        holder.txtDate.setText(Util.formatDate(q_item.getQuestion().date));
        if (!Util.isNetworkAvailable(holder.context) || q_item.getPhotoUrl().equals(holder.context.getResources().getString(R.string.empty_profile_pic_url)))
        {
            Util.getDatabase().getReference("User").child(q_item.getQuestion().user_key).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    holder.imgProfile.setLetter(dataSnapshot.getValue(String.class));
                    holder.imgProfile.setShapeColor(getBackgroundColor(holder.context, dataSnapshot.getValue(String.class)));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else
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

    // Metodo per ottenere il colore di sfondo per la lettera material, scelto in base alla prima lettera della stringa passata
    private int getBackgroundColor(Context context, String s)
    {
        int[] colors = context.getResources().getIntArray(R.array.colors);

        s.toLowerCase();
        switch (s.charAt(0))
        {
            case 'A':
                return colors[0];
            case 'B':
                return colors[2];
            case 'C':
                return colors[4];
            case 'D':
                return colors[6];
            case 'E':
                return colors[8];
            case 'F':
                return colors[10];
            case 'G':
                return colors[12];
            case 'H':
                return colors[1];
            case 'I':
                return colors[3];
            case 'J':
                return colors[5];
            case 'K':
                return colors[7];
            case 'L':
                return colors[9];
            case 'M':
                return colors[11];
            case 'N':
                return colors[0];
            case 'O':
                return colors[2];
            case 'P':
                return colors[4];
            case 'Q':
                return colors[6];
            case 'R':
                return colors[8];
            case 'S':
                return colors[10];
            case 'T':
                return colors[12];
            case 'U':
                return colors[1];
            case 'V':
                return colors[3];
            case 'W':
                return colors[5];
            case 'X':
                return colors[7];
            case 'Y':
                return colors[9];
            case 'Z':
                return colors[11];
        }
        return 0;
    }
}
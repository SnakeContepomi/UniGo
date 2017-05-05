package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.DetailActivity;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.utils.Util;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder>
{
    private List<QuestionAdapterItem> questionList;

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        Context context;
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

    @Override
    public QuestionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_question, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        final QuestionAdapterItem qItem = questionList.get(position);
        final DatabaseReference favoriteReference = Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(qItem.getQuestionKey());

        holder.txtTitle.setText(qItem.getQuestion().title);
        holder.txtCourse.setText(qItem.getQuestion().course);
        holder.txtDesc.setText(qItem.getQuestion().desc);
        holder.txtDate.setText(Util.formatDate(qItem.getQuestion().date));

        // Viene recuperata l'immagine profilo dell'utente che ha effettauto la domanda
        Util.getDatabase().getReference("User").child(qItem.getQuestion().user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (!Util.isNetworkAvailable(holder.context) || dataSnapshot.child("photoUrl").getValue(String.class).equals(holder.context.getResources().getString(R.string.empty_profile_pic_url)))
                {
                    holder.imgProfile.setLetter(dataSnapshot.getValue(String.class));
                    holder.imgProfile.setShapeColor(Util.getLetterBackgroundColor(holder.context, dataSnapshot.getValue(String.class)));
                }
                else
                    Picasso.with(holder.imgProfile.getContext()).load(dataSnapshot.child("photoUrl").getValue(String.class)).fit().into(holder.imgProfile);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Viene controllato se la domanda è stata inserita all'interno dei preferiti dell'utente
        holder.imgIcon.setBackgroundResource(R.drawable.ic_star_border_black_24dp);
        holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.md_grey_500)));
        favoriteReference.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Se la domanda risulta tra i preferiti, viene evidenziata
                if (dataSnapshot.getValue() != null)
                {
                    holder.imgIcon.setBackgroundResource(R.drawable.ic_star_black_24dp);
                    holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.colorAmber)));
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
                favoriteReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        // Se la domanda non è nei preferiti, essa viene aggiunta ed evidenziata
                        if (dataSnapshot.getValue() == null)
                        {
                            favoriteReference.setValue(true);
                            holder.imgIcon.setBackgroundResource(R.drawable.ic_star_black_24dp);
                            holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.colorAmber)));
                        }
                        // Se la domanda è già nei preferiti, essa viene rimossa
                        else
                        {
                            favoriteReference.removeValue();
                            holder.imgIcon.setBackgroundResource(R.drawable.ic_star_border_black_24dp);
                            holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(holder.imgIcon.getContext().getResources().getColor(R.color.md_grey_500)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });

        // Al click della card viene aperta l'activity che ne mostra i dettagli
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(holder.context, DetailActivity.class);
                intent.putExtra("question_key", qItem.getQuestionKey());
                holder.context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return questionList.size();
    }
}
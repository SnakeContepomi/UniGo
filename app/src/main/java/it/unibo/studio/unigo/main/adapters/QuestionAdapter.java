package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.v7.widget.CardView;
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
        CardView layout;
        MaterialLetterIcon imgProfile;
        TextView txtName, txtCourse, txtTitle, txtDate, txtRating, txtAnswer;
        LinearLayout rating;
        ImageView imgRating, imgFavorite;

        ViewHolder(CardView v)
        {
            super(v);
            context = v.getContext();
            layout = v;
            imgProfile = (MaterialLetterIcon ) v.findViewById(R.id.itemq_user);
            txtName = (TextView) v.findViewById(R.id.itemq_name);
            txtDate = (TextView) v.findViewById(R.id.itemq_date);
            txtCourse = (TextView) v.findViewById(R.id.itemq_course);
            txtTitle = (TextView) v.findViewById(R.id.itemq_title);
            rating = (LinearLayout) v.findViewById(R.id.itemq_rating);
            imgRating = (ImageView) v.findViewById(R.id.itemq_imgrating);
            txtRating = (TextView) v.findViewById(R.id.itemq_nrating);
            imgFavorite = (ImageView) v.findViewById(R.id.itemq_imgfavorite);
            txtAnswer = (TextView) v.findViewById(R.id.itemq_nanswer);
        }
    }

    public QuestionAdapter(List<QuestionAdapterItem> questionList)
    {
        this.questionList = questionList;
    }

    @Override
    public QuestionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ViewHolder((CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_question, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        final QuestionAdapterItem qItem = questionList.get(position);

        initQuestion(holder, qItem);
        initActionRating(holder, qItem);
        initActionFavorite(holder, qItem.getQuestionKey());

        // Viene inizializzato il numero di risposte relative alla domanda in questione
        if (qItem.getQuestion().answers != null)
            holder.txtAnswer.setText(String.valueOf(qItem.getQuestion().answers.size()));
        else
            holder.txtAnswer.setText("0");

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

    // Metodo che recupera le informazioni relative a ciascuna domanda
    private void initQuestion(final ViewHolder holder, QuestionAdapterItem qItem)
    {
        holder.txtDate.setText(Util.formatDate(qItem.getQuestion().date));
        holder.txtCourse.setText(qItem.getQuestion().course);
        holder.txtTitle.setText(qItem.getQuestion().title);

        // Viene recuperata l'immagine profilo dell'utente che ha effettauto la domanda
        Util.getDatabase().getReference("User").child(qItem.getQuestion().user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                holder.txtName.setText(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
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
    }

    // Metodo che inizializza la logica del pulsante "Rating" relativo alla domanda in questione
    // Il pulsante Rating è cliccabile soltanto una volta
    private void initActionRating(final ViewHolder holder, final QuestionAdapterItem qItem)
    {
        // Click Listener relativo alla Action "Rating" (cuore)
        holder.rating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Util.getDatabase().getReference("Question").child(qItem.getQuestionKey()).child("ratings").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).setValue(true);
                holder.imgRating.setImageTintList(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                holder.txtRating.setTextColor(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                holder.txtRating.setText(String.valueOf(Integer.valueOf(String.valueOf(holder.txtRating.getText())) + 1));
                holder.rating.setClickable(false);
            }
        });

        // Al caricamento di ogni domanda, viene controllato se l'utente ha già votato la domanda
        // (un utente può votare la domanda una sola volta)
        if (qItem.getQuestion().ratings != null)
        {
            // Inizializzazione del numero di rating della domanda corrente
            holder.txtRating.setText(String.valueOf(qItem.getQuestion().ratings.size()));

            for (String key : qItem.getQuestion().ratings.keySet())
                if (key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                {
                    holder.imgRating.setImageTintList(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                    holder.txtRating.setTextColor(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                    holder.rating.setClickable(false);
                    break;
                }
        }
        else
            holder.txtRating.setText("0");
    }

    // Metodo che verifica se la domanda appartiene all'elenco dei preferiti dell'utente
    // e abilita il clickListener per poterla inserire
    private void initActionFavorite(final ViewHolder holder, String questionKey)
    {
        final DatabaseReference favoriteReference = Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(questionKey);

        // Viene controllato se la domanda è presente all'interno dei preferiti dell'utente
        favoriteReference.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Se la domanda risulta tra i preferiti, viene evidenziata
                if (dataSnapshot.getValue() != null)
                    holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorAmber)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Pulsante che consente di aggiungere una domanda ai preferiti
        holder.imgFavorite.setOnClickListener(new View.OnClickListener() {
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
                            holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorAmber)));
                        }
                        // Se la domanda è già nei preferiti, essa viene rimossa
                        else
                        {
                            favoriteReference.removeValue();
                            holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorIconGray)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });
    }
}
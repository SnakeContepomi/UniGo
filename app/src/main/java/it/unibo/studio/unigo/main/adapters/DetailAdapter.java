package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import net.cachapa.expandablelayout.ExpandableLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.DetailAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Comment;

public class DetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_QUESTION = 1;
    private static final int TYPE_ANSWER = 2;
    private List<DetailAdapterItem> answerList;
    private String question_key;

    private static class questionHolder extends RecyclerView.ViewHolder
    {
        Context context;
        MaterialLetterIcon userPhoto;
        TextView txtName, txtDate, txtLvl, txtCourse, txtTitle, txtDesc, txtNAnswer, txtRating;
        LinearLayout rating;
        ImageView imgrating, imgfavorite;

        questionHolder(View v)
        {
            super(v);
            context = v.getContext();
            userPhoto = (MaterialLetterIcon) v.findViewById(R.id.cardq_userPhoto);
            txtName = (TextView) v.findViewById(R.id.cardq_name);
            txtDate = (TextView) v.findViewById(R.id.cardq_date);
            txtLvl = (TextView) v.findViewById(R.id.cardq_lvl);
            txtCourse = (TextView) v.findViewById(R.id.cardq_course);
            txtTitle = (TextView) v.findViewById(R.id.cardq_title);
            txtDesc = (TextView) v.findViewById(R.id.cardq_desc);
            txtNAnswer = (TextView) v.findViewById(R.id.cardq_nanswer);
            rating = (LinearLayout) v.findViewById(R.id.cardq_rating);
            imgrating = (ImageView) v.findViewById(R.id.cardq_imgrating);
            txtRating = (TextView) v.findViewById(R.id.cardq_nrating);
            imgfavorite = (ImageView) v.findViewById(R.id.cardq_favorite);
        }
    }

    private static class answerHolder extends RecyclerView.ViewHolder
    {
        Context context;

        MaterialLetterIcon imgProfile;
        LinearLayout btnComment;
        ExpandableLayout expandableLayout;
        RecyclerView recyclerViewComment;
        CommentAdapter cAdapter;
        TextView txtName, txtDesc;

        answerHolder(View v)
        {
            super(v);
            context = v.getContext();
            imgProfile = (MaterialLetterIcon) v.findViewById(R.id.carda_userPhoto);
            txtName = (TextView) v.findViewById(R.id.carda_name);
            txtDesc = (TextView) v.findViewById(R.id.carda_desc);
            btnComment = (LinearLayout) v.findViewById(R.id.carda_comment);
            expandableLayout = (ExpandableLayout) v.findViewById(R.id.expandable_layout);
            recyclerViewComment = (RecyclerView) v.findViewById(R.id.recyclerViewComment);
            recyclerViewComment.setLayoutManager(new LinearLayoutManager(context));
        }
    }

    public DetailAdapter(List<DetailAdapterItem> answerList, String question_key)
    {
        this.answerList = answerList;
        this.question_key = question_key;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        if (viewType == 1)
            return new questionHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.question_detail, parent, false));
        else
            return new answerHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_answer, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position)
    {
        final DetailAdapterItem qd_item = answerList.get(position);

        switch (holder.getItemViewType())
        {
            case TYPE_QUESTION:
                final questionHolder qh = (questionHolder) holder;
                getQuestionInfo(qh);
                initActions(qh);
                initActionsClickListener(qh);
                break;

            case TYPE_ANSWER:
                final answerHolder ah = (answerHolder) holder;
                Picasso.with(ah.imgProfile.getContext()).load(qd_item.getPhoto()).fit().into(ah.imgProfile);
                ah.txtName.setText(qd_item.getAnswer().user_key);
                ah.txtDesc.setText(qd_item.getAnswer().desc);
                ah.btnComment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        ah.expandableLayout.toggle();
                    }
                });

                initCommentList(ah, answerList.get(position).getAnswerKey());
                break;
        }
    }

    // Se la posizione è la prima, l'oggetto viene gestito come Question, altrimenti come Answer
    @Override
    public int getItemViewType(int position)
    {
        return position == 0 ? TYPE_QUESTION : TYPE_ANSWER;
    }

    @Override
    public int getItemCount()
    {
        return answerList.size();
    }

    // Metodo per recuperare i dettagli della domanda e dell'utente che l'ha effettuata
    private void getQuestionInfo(final questionHolder qh)
    {
        // Query per la domanda
        Util.getDatabase().getReference("Question").child(question_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Query per l'utente
                Util.getDatabase().getReference("User").child(dataSnapshot.child("user_key").getValue(String.class)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        // Impostazione dei campi relativi all'utente
                        qh.txtName.setText(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                        qh.txtLvl.setText(String.valueOf(dataSnapshot.child("exp").getValue(Integer.class)));
                        if (!Util.isNetworkAvailable(qh.context) || dataSnapshot.child("photoUrl").getValue(String.class).equals(qh.context.getResources().getString(R.string.empty_profile_pic_url)))
                        {
                            qh.userPhoto.setLetter(dataSnapshot.child("name").getValue(String.class));
                            qh.userPhoto.setShapeColor(Util.getLetterBackgroundColor(qh.context, dataSnapshot.child("name").getValue(String.class)));
                        }
                        else
                            Picasso.with(qh.context).load(dataSnapshot.child("photoUrl").getValue(String.class)).into(qh.userPhoto);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });

                // Impostazione dei campi relativi alla domanda
                qh.txtDate.setText(Util.formatDate(dataSnapshot.child("date").getValue(String.class)));
                qh.txtCourse.setText(dataSnapshot.child("course").getValue(String.class));
                qh.txtTitle.setText(dataSnapshot.child("title").getValue(String.class));
                qh.txtDesc.setText(dataSnapshot.child("desc").getValue(String.class));
                qh.txtNAnswer.setText(dataSnapshot.child("answers").getChildrenCount() + qh.context.getResources().getString(R.string.detail_nanswer));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che inizializza i colori dei pulsanti "Rating" e "Favorite" della domanda
    private void initActions(final questionHolder qh)
    {
        // Inizializzazione del numero di rating della domanda corrente
        Util.getDatabase().getReference("Question").child(question_key).child("ratings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                qh.txtRating.setText(String.valueOf(dataSnapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Inizializzazione della Action "Rating"
        Util.getDatabase().getReference("Question").child(question_key).child("ratings").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null)
                {
                    qh.imgrating.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
                    qh.txtRating.setTextColor(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
                }
                else
                {
                    qh.rating.setClickable(false);
                    qh.imgrating.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAccent)));
                    qh.txtRating.setTextColor(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAccent)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Inizializzazione della Action "Favorite"
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(question_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() == null)
                    qh.imgfavorite.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
                else
                    qh.imgfavorite.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorYellow)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che inizializza la logica dei pulsanti "Rating" e "Favorite" relativi alla domanda in questione
    // Il pulsante Rating è cliccabile soltanto una volta
    // Il pultante Favorite è aggiunger/rimuove la domanda corrente dalla lista dei preferiti
    private void initActionsClickListener(final questionHolder qh)
    {
        // Click Listener relativo alla Action "Rating" (cuore)
        final DatabaseReference ratingReference = Util.getDatabase().getReference("Question").child(question_key).child("ratings").child(Util.encodeEmail(Util.getCurrentUser().getEmail()));
        qh.rating.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                ratingReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        ratingReference.setValue(true);
                        qh.imgrating.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAccent)));
                        qh.txtRating.setTextColor((ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAccent))));
                        qh.txtRating.setText(String.valueOf(Integer.valueOf(String.valueOf(qh.txtRating.getText())) + 1));
                        qh.rating.setClickable(false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });

        // Click Listener relativo alla Action "Favorite" (stella)
        final DatabaseReference favoriteReference = Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(question_key);
        qh.imgfavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                favoriteReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.getValue() == null)
                        {
                            favoriteReference.setValue(true);
                            qh.imgfavorite.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorYellow)));
                        }
                        else
                        {
                            favoriteReference.removeValue();
                            qh.imgfavorite.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });
    }

    // Metodo per recupereare tutti i commenti di una relativa risposta
    private void initCommentList(final answerHolder ah, String answer_key)
    {
        final List<Comment> commentList = new ArrayList<>();

        // Query recupero commenti
        Util.getDatabase().getReference("Question").child(question_key).child("answers").child(answer_key).child("comments").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                final Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                while (iterator.hasNext())
                {
                    final DataSnapshot comment = iterator.next();

                    commentList.add(comment.getValue(Comment.class));
                    if (!iterator.hasNext())
                    {
                        ah.cAdapter = new CommentAdapter(commentList);
                        ah.recyclerViewComment.setAdapter(ah.cAdapter);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}
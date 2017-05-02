package it.unibo.studio.unigo.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.DetailAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Comment;
import it.unibo.studio.unigo.utils.firebase.User;

import static it.unibo.studio.unigo.utils.Util.getDatabase;

public class DetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_QUESTION = 1;
    private static final int TYPE_ANSWER = 2;
    private boolean answerAllowed = true;
    private List<DetailAdapterItem> answerList;
    private String question_key, answerNotSent, commentNotSent;
    private Activity activity;

    private static class questionHolder extends RecyclerView.ViewHolder
    {
        Context context;
        MaterialLetterIcon userPhoto;
        TextView txtName, txtDate, txtLvl, txtCourse, txtTitle, txtDesc, txtNAnswer, txtRating;
        LinearLayout rating, favorite, answer;
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
            favorite = (LinearLayout) v.findViewById(R.id.cardq_favorite);
            imgfavorite = (ImageView) v.findViewById(R.id.cardq_imgfavorite);
            answer = (LinearLayout) v.findViewById(R.id.cardq_answer);
        }
    }

    private static class answerHolder extends RecyclerView.ViewHolder
    {
        Context context;

        MaterialLetterIcon imgProfile;
        LinearLayout like, layoutComments;
        ImageView imgLike, imgComment;
        ExpandableLayout expandableLayout;
        RecyclerView recyclerViewComment;
        CommentAdapter cAdapter;
        TextView txtName, txtDate, txtLvl, txtDesc, txtLike, txtComment;

        answerHolder(View v)
        {
            super(v);
            context = v.getContext();
            imgProfile = (MaterialLetterIcon) v.findViewById(R.id.carda_userPhoto);
            txtName = (TextView) v.findViewById(R.id.carda_name);
            txtDate = (TextView) v.findViewById(R.id.carda_date);
            txtLvl = (TextView) v.findViewById(R.id.carda_lvl);
            txtDesc = (TextView) v.findViewById(R.id.carda_desc);
            like = (LinearLayout) v.findViewById(R.id.carda_like);
            imgLike = (ImageView) v.findViewById(R.id.carda_imglike);
            txtLike = (TextView) v.findViewById(R.id.carda_txtlike);
            layoutComments = (LinearLayout) v.findViewById(R.id.carda_comment);
            txtComment = (TextView) v.findViewById(R.id.carda_txtcomment);
            imgComment = (ImageView) v.findViewById(R.id.carda_reply);
            expandableLayout = (ExpandableLayout) v.findViewById(R.id.expandable_layout);
            recyclerViewComment = (RecyclerView) v.findViewById(R.id.recyclerViewComment);
            recyclerViewComment.setLayoutManager(new LinearLayoutManager(context));
        }
    }

    public DetailAdapter(List<DetailAdapterItem> answerList, String question_key, Activity activity)
    {
        this.answerList = answerList;
        this.question_key = question_key;
        this.activity = activity;
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
        switch (holder.getItemViewType())
        {
            case TYPE_QUESTION:
                final questionHolder qh = (questionHolder) holder;
                getQuestionInfo(qh);
                initQuestionActions(qh);
                initQuestionActionsClickListener(qh);
                break;

            case TYPE_ANSWER:
                final answerHolder ah = (answerHolder) holder;
                getAnswerInfo(ah, answerList.get(position));
                initAnswerActions(ah, answerList.get(position));
                initAnswerActionsClickListener(ah, answerList.get(position));
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
        getDatabase().getReference("Question").child(question_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Query per l'utente
                getDatabase().getReference("User").child(dataSnapshot.child("user_key").getValue(String.class)).addListenerForSingleValueEvent(new ValueEventListener() {
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
    private void initQuestionActions(final questionHolder qh)
    {
        // Inizializzazione del numero di rating della domanda corrente
        getDatabase().getReference("Question").child(question_key).child("ratings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                qh.txtRating.setText(String.valueOf(dataSnapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Inizializzazione della Action "Rating"
        getDatabase().getReference("Question").child(question_key).child("ratings").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).addListenerForSingleValueEvent(new ValueEventListener() {
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
                    qh.imgrating.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.primary)));
                    qh.txtRating.setTextColor(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.primary)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Inizializzazione della Action "Favorite"
        getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(question_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() == null)
                    qh.imgfavorite.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
                else
                    qh.imgfavorite.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAmber)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Inizializzazione del comportamento del tasto "Rispondi":
        // Se la variaible booleana viene impostata a false, significa che l'utente ha già risposto alla domanda e non può più
        // effettuarne altre
        getDatabase().getReference("Question").child(question_key).child("answers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getChildrenCount() == 0)
                    initReplyQuestionClickListener(qh);
                else
                {
                    final Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                    while (iterator.hasNext()) {
                        final DataSnapshot child = iterator.next();

                        if (child.getValue(Answer.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                            answerAllowed = false;
                        if (!iterator.hasNext())
                            initReplyQuestionClickListener(qh);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che inizializza la logica dei pulsanti "Rating" e "Favorite" relativi alla domanda in questione
    // Il pulsante Rating è cliccabile soltanto una volta
    // Il pultante Favorite permette di aggiungere/rimuovere la domanda corrente dalla lista dei preferiti
    private void initQuestionActionsClickListener(final questionHolder qh)
    {
        // Click Listener relativo alla Action "Rating" (cuore)
        final DatabaseReference ratingReference = getDatabase().getReference("Question").child(question_key).child("ratings").child(Util.encodeEmail(Util.getCurrentUser().getEmail()));
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
                        qh.txtRating.setTextColor(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAccent)));
                        qh.txtRating.setText(String.valueOf(Integer.valueOf(String.valueOf(qh.txtRating.getText())) + 1));
                        qh.rating.setClickable(false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });

        // Click Listener relativo alla Action "Favorite" (stella)
        final DatabaseReference favoriteReference = getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(question_key);
        qh.favorite.setOnClickListener(new View.OnClickListener() {
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
                            qh.imgfavorite.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAmber)));
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

    // Inizializzazione del comportamento del pulsante "Rispondi alla domanda":
    // Se l'utente ha già risposto alla domanda in questione, verrà mostrato un messaggio per informare l'utente che non è possibile
    // aggiungere ulteriori risposte, altrimenti verrà aperto un alert dialog per inserire la risposta
    private void initReplyQuestionClickListener(final questionHolder qh)
    {
        qh.answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (answerAllowed)
                    openAnswerDialog();
                else
                    Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.detail_error_answer_done, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // Metodo per recuperare le informazioni di una risposta e del suo autore
    private void getAnswerInfo(final answerHolder ah, final DetailAdapterItem detailItem)
    {
        // Query per recuperare le informazioni dell'autore della risposta
        getDatabase().getReference("User").child(detailItem.getAnswer().user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (!Util.isNetworkAvailable(ah.context) || dataSnapshot.child("photoUrl").getValue(String.class).equals(ah.context.getResources().getString(R.string.empty_profile_pic_url)))
                {
                    ah.imgProfile.setLetter(dataSnapshot.child("name").getValue(String.class));
                    ah.imgProfile.setShapeColor(Util.getLetterBackgroundColor(ah.context, dataSnapshot.child("name").getValue(String.class)));
                }
                else
                    Picasso.with(ah.imgProfile.getContext()).load(dataSnapshot.child("photoUrl").getValue(String.class)).into(ah.imgProfile);

                ah.txtName.setText(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                ah.txtLvl.setText(String.valueOf(dataSnapshot.child("exp").getValue(Integer.class)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Impostazione delle informazioni relative alla risposta
        ah.txtDate.setText(Util.formatDate(detailItem.getAnswer().date));
        ah.txtDesc.setText(detailItem.getAnswer().desc);
        // Recupero dei commenti relativi alla risposta corrente
        initCommentList(ah, detailItem.getAnswerKey());
    }

    // Metodo per recupereare tutti i commenti di una relativa risposta
    private void initCommentList(final answerHolder ah, String answer_key)
    {
        final List<Comment> commentList = new ArrayList<>();

        // Query recupero commenti
        getDatabase().getReference("Question").child(question_key).child("answers").child(answer_key).child("comments").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
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

    // Metodo che inizializza i colori dei pulsanti "Like" e "Comments" della risposta
    private void initAnswerActions(final answerHolder ah, final DetailAdapterItem detailItem)
    {
        DatabaseReference likeReference = getDatabase().getReference("Question").child(question_key).child("answers").child(detailItem.getAnswerKey()).child("likes");

        // Verifica se l'utente ha già inserito il "Like" per la risposta corrente
        likeReference.child(Util.encodeEmail(Util.getCurrentUser().getEmail())).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() == null)
                {
                    ah.imgLike.setBackgroundTintList(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorDarkGray)));
                    ah.txtLike.setTextColor((ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorDarkGray))));
                }
                else
                {
                    ah.imgLike.setBackgroundTintList(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue)));
                    ah.txtLike.setTextColor((ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue))));
                    ah.like.setClickable(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Inizializzazione del numero di "Like" della risposta corrente
        likeReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                ah.txtLike.setText(String.valueOf(dataSnapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Inizializzazione del numero di "Comment" della risposta corrente
        getDatabase().getReference("Question").child(question_key).child("answers").child(detailItem.getAnswerKey()).child("comments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                ah.txtComment.setText(String.valueOf(dataSnapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che inizializza la logica dei pulsanti "Like" e "Comment" relativi alla risposta in questione
    // Il pulsante Like è cliccabile soltanto una volta
    // Il pultante Comment permette di mostrare/nascondere i commenti relativi alla risposta corrente
    private void initAnswerActionsClickListener(final answerHolder ah, final DetailAdapterItem detailItem)
    {
        // Click Listener relativo alla Action "Like" (pollice)
        final DatabaseReference likeReference = getDatabase().getReference("Question").child(question_key).child("answers").child(detailItem.getAnswerKey()).child("likes").child(Util.encodeEmail(Util.getCurrentUser().getEmail()));
        ah.like.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                likeReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (Util.encodeEmail(Util.getCurrentUser().getEmail()).equals(detailItem.getAnswer().user_key))
                            Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.detail_error_autolike, Snackbar.LENGTH_SHORT).show();
                        else if (!Util.isNetworkAvailable(activity.getApplicationContext()))
                            Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.detail_error_like_without_connection, Snackbar.LENGTH_LONG).show();
                        else
                        {
                            likeReference.setValue(true);
                            ah.imgLike.setBackgroundTintList(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue)));
                            ah.txtLike.setTextColor(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue)));
                            ah.txtLike.setText(String.valueOf(Integer.valueOf(String.valueOf(ah.txtLike.getText())) + 1));
                            ah.like.setClickable(false);
                            updateExpForLike(detailItem.getAnswer().user_key);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });

        // Click Listener relativo alla Action "Comments"
        ah.layoutComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                ah.expandableLayout.toggle();
            }
        });

        ah.imgComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                openCommentDialog(detailItem.getAnswerKey());
            }
        });
    }

    // Metodo per aggiungere una risposta ad una determinata domanda
    private void openAnswerDialog()
    {
        final View dialogLayout = activity.getLayoutInflater().inflate(R.layout.alert_reply_layout, null);

        if (!Util.isNetworkAvailable(activity.getApplicationContext()) || Util.getCurrentUser().getPhotoUrl().equals(activity.getApplicationContext().getResources().getString(R.string.empty_profile_pic_url)))
        {
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setLetter(Util.getCurrentUser().getDisplayName());
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setShapeColor(Util.getLetterBackgroundColor(activity.getApplicationContext(), Util.getCurrentUser().getDisplayName()));
        }
        else
            Picasso.with(activity.getApplicationContext())
                    .load(Util.getCurrentUser().getPhotoUrl())
                    .into((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto));

        ((TextView)dialogLayout.findViewById(R.id.reply_name)).setText(Util.getCurrentUser().getDisplayName());
        if (answerNotSent != null)
            ((EditText) dialogLayout.findViewById(R.id.reply_desc)).setText(answerNotSent);
        else
            ((EditText)dialogLayout.findViewById(R.id.reply_desc)).setHint(R.string.detail_write_answer);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setPositiveButton(activity.getApplicationContext().getString(R.string.alert_dialog_send),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                if (((EditText)dialogLayout.findViewById(R.id.reply_desc)).getText().toString().equals(""))
                                    Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.detail_snackbar_reply_cancel, Snackbar.LENGTH_LONG).show();
                                else if (!Util.isNetworkAvailable(activity.getApplicationContext()))
                                {
                                    Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.snackbar_no_internet_connection, Snackbar.LENGTH_LONG).show();
                                    answerNotSent = ((EditText) dialogLayout.findViewById(R.id.reply_desc)).getText().toString();
                                }
                                else
                                {
                                    writeAnswer(((EditText) dialogLayout.findViewById(R.id.reply_desc)).getText().toString());
                                    answerNotSent = null;
                                }
                            }
                        })
                .setNegativeButton(activity.getApplicationContext().getString(R.string.alert_dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        })
                .setView(dialogLayout)
                .setCancelable(false);
        builder.show();
    }

    // Metodo per aggiungere un commento alla risposta corrente
    private void openCommentDialog(final String answer_key)
    {
        final View dialogLayout = activity.getLayoutInflater().inflate(R.layout.alert_reply_layout, null);

        if (!Util.isNetworkAvailable(activity.getApplicationContext()) || Util.getCurrentUser().getPhotoUrl().equals(activity.getApplicationContext().getResources().getString(R.string.empty_profile_pic_url)))
        {
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setLetter(Util.getCurrentUser().getDisplayName());
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setShapeColor(Util.getLetterBackgroundColor(activity.getApplicationContext(), Util.getCurrentUser().getDisplayName()));
        }
        else
            Picasso.with(activity.getApplicationContext())
                    .load(Util.getCurrentUser().getPhotoUrl())
                    .into((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto));

        ((TextView)dialogLayout.findViewById(R.id.reply_name)).setText(Util.getCurrentUser().getDisplayName());
        if (commentNotSent != null)
            ((EditText) dialogLayout.findViewById(R.id.reply_desc)).setText(commentNotSent);
        else
            ((EditText)dialogLayout.findViewById(R.id.reply_desc)).setHint(R.string.detail_write_comment);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setPositiveButton(activity.getApplicationContext().getString(R.string.alert_dialog_send),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                if (((EditText)dialogLayout.findViewById(R.id.reply_desc)).getText().toString().equals(""))
                                    Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.detail_snackbar_reply_cancel, Snackbar.LENGTH_LONG).show();
                                else
                                {
                                    writeComment(((EditText) dialogLayout.findViewById(R.id.reply_desc)).getText().toString(), answer_key);
                                    commentNotSent = null;
                                }
                            }
                        })
                .setNegativeButton(activity.getApplicationContext().getString(R.string.alert_dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        })
                .setView(dialogLayout)
                .setCancelable(false);
        builder.show();
    }

    // Metodo per inserire una risposta nel database e collegarlo al rispettivo autore
    private void writeAnswer(String desc)
    {
        String answer_key = Util.getDatabase().getReference("Question").child(question_key).child("answers").push().getKey();

        Util.getDatabase().getReference("Question").child(question_key).child("answers").child(answer_key).setValue(
                new Answer(Util.encodeEmail(Util.getCurrentUser().getEmail()), desc));
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("answers").child(answer_key).setValue(true);
        updateExpForAnswer();
    }

    // Metodo per inserire un commento ad una risposta e collegarlo al rispettivo autore
    private void writeComment(String desc, String answer_key)
    {
        String comment_key = Util.getDatabase().getReference("Question").child(question_key).child("answers").child(answer_key).child("comments").push().getKey();

        Util.getDatabase().getReference("Question").child(question_key).child("answers").child(answer_key).child("comments").child(comment_key).setValue(
                new Comment(Util.encodeEmail(Util.getCurrentUser().getEmail()), desc));
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("comments").child(comment_key).setValue(true);
    }

    // Metodo che viene utilizzato per aggiungere i crediti a fronte di una risposta data e di aumentare l'exp dell'utente
    private void updateExpForAnswer()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail()))
            .runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData)
                {
                    User u = mutableData.getValue(User.class);
                    if (u == null)
                        return Transaction.success(mutableData);

                    u.credits += Util.CREDITS_ANSWER;
                    u.exp += Util.EXP_ANSWER;
                    mutableData.setValue(u);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean success, DataSnapshot dataSnapshot)
                {
                    if (success)
                        Toast.makeText(activity.getApplicationContext(), R.string.detail_toast_answer, Toast.LENGTH_LONG).show();
                }
            });
    }

    // Metodo che viene utilizzato per aggiungere punti esperienza all'utente che ha ricevuto il "like"
    private void updateExpForLike(String user_key)
    {
        Util.getDatabase().getReference("User").child(user_key)
            .runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData)
                {
                    User u = mutableData.getValue(User.class);
                    if (u == null)
                        return Transaction.success(mutableData);

                    u.exp += Util.EXP_LIKE;
                    u.credits += Util.CREDITS_LIKE;
                    mutableData.setValue(u);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean success, DataSnapshot dataSnapshot) { }
            });
    }
}
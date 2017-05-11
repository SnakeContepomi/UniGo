package it.unibo.studio.unigo.main.adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
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
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Comment;
import it.unibo.studio.unigo.utils.firebase.Question;
import it.unibo.studio.unigo.utils.firebase.User;
import static it.unibo.studio.unigo.utils.Util.getCurrentUser;


public class DetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_QUESTION = 1;
    private static final int TYPE_ANSWER = 2;
    private static final int UPDATE_CODE_ANSWER = 1;
    private static final int UPDATE_CODE_RATING = 2;
    private static final int UPDATE_CODE_LIKES = 3;
    private static final int UPDATE_CODE_COMMENTS = 4;

    private boolean answerAllowed = true;
    private Question question;
    private List<Answer> answerList;
    private List<String> answerKeyList;
    private Comment newComment;
    private String questionKey, answerNotSent, commentNotSent, newCommentKey;
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
        List<Comment> commentList;
        List<String> commentKeyList;

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

    public DetailAdapter(Question question, String questionKey, Activity activity)
    {
        this.question = question;
        this.questionKey = questionKey;
        this.activity = activity;
        answerList = new ArrayList<>();
        answerKeyList = new ArrayList<>();

        if (question.answers != null)
        {
            // Vengono recuperate le chiavi delle varie risposte
            for (String key : question.answers.keySet())
                answerKeyList.add(key);
            // Viene ordinata la lista delle risposte in base alla chiave
            // (la chiave include un ordinamento temporale)
            Collections.sort(answerKeyList, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2)
                {
                    return new CompareToBuilder().append(s1, s2).toComparison();
                }
            });
            // Viene inizializzata la lista di Answer
            for (String key : answerKeyList)
                answerList.add(question.answers.get(key));
        }
        // Viene aggiunto un elemento in testa per definire lo spazio relativo alla domanda
        answerList.add(0, null);
        answerKeyList.add(0, null);
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
                initActionRating(qh);
                initActionFavorite(qh);
                initActionQuestionReply(qh);
                break;

            case TYPE_ANSWER:
                final answerHolder ah = (answerHolder) holder;
                getAnswerInfo(ah, answerList.get(position));
                initActionLike(ah, answerList.get(position), answerKeyList.get(position));
                initActionComments(ah);
                initActionAnswerReply(ah, answerKeyList.get(position));
                break;
        }
    }

    // Aggiornamento parziale di uno o più elementi della recyclerview in realtime
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position, List<Object> payload)
    {
        // Aggiornamento totale
        if (payload.isEmpty())
            onBindViewHolder(holder, position);
        // Aggiornamento parziale
        else
            switch (holder.getItemViewType())
            {
                case TYPE_QUESTION:
                    questionHolder qh = (questionHolder) holder;
                    if (payload.get(0) instanceof Integer)
                        switch ((Integer) payload.get(0))
                        {
                            // Aggiornamento del numero di risposte
                            case UPDATE_CODE_ANSWER:
                                qh.txtNAnswer.setText(qh.context.getResources().getString(R.string.detail_nanswer, answerList.size() - 1));
                                break;

                            // Aggiornamento del numero di rating
                            case UPDATE_CODE_RATING:
                                if (question.ratings != null)
                                    qh.txtRating.setText(String.valueOf(question.ratings.size()));
                                else
                                    qh.txtRating.setText("0");
                                break;

                            default:
                                break;
                        }
                    break;

                case TYPE_ANSWER:
                    final answerHolder ah = (answerHolder) holder;
                    if (payload.get(0) instanceof Integer)
                        switch ((Integer) payload.get(0))
                        {
                            // Aggiornamento del numero di likes
                            case UPDATE_CODE_LIKES:
                                ah.txtLike.setText(String.valueOf(answerList.get(position).likes.size()));
                                break;

                            // Aggiornamento del numero di commenti
                            case UPDATE_CODE_COMMENTS:
                                ah.cAdapter.refreshAnswerComments(newComment, newCommentKey);
                                ah.txtComment.setText(String.valueOf(ah.commentList.size()));
                                break;

                            default:
                                break;
                        }
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
        // Query per l'utente
        Util.getDatabase().getReference("User").child(question.user_key).addListenerForSingleValueEvent(new ValueEventListener() {
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
                    Picasso.with(qh.context).load(dataSnapshot.child("photoUrl").getValue(String.class)).fit().into(qh.userPhoto);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Impostazione dei campi relativi alla domanda
        qh.txtDate.setText(Util.formatDate(question.date));
        qh.txtCourse.setText(question.course);
        qh.txtTitle.setText(question.title);
        qh.txtDesc.setText(question.desc);
        qh.txtNAnswer.setText(qh.context.getResources().getString(R.string.detail_nanswer, answerList.size() - 1));
    }

    // Metodo che inizializza la logica del pulsante "Rating" relativo alla domanda in questione
    // Il pulsante Rating è cliccabile soltanto una volta
    private void initActionRating(final questionHolder qh)
    {
        // Click Listener relativo alla Action "Rating" (cuore)
        qh.rating.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                Util.getDatabase().getReference("Question").child(questionKey).child("ratings").child(Util.encodeEmail(getCurrentUser().getEmail())).setValue(true);
                qh.imgrating.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorPrimary)));
                qh.txtRating.setTextColor(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorPrimary)));
                qh.txtRating.setText(String.valueOf(Integer.valueOf(String.valueOf(qh.txtRating.getText())) + 1));
                qh.rating.setClickable(false);
            }
        });

        // Inizializzazione della Action "Rating"
        qh.imgrating.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
        qh.txtRating.setTextColor(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
        if (question.ratings != null)
        {
            // Inizializzazione del numero di rating della domanda corrente
            qh.txtRating.setText(String.valueOf(question.ratings.size()));

            for (String key : question.ratings.keySet())
                if (key.equals(Util.encodeEmail(getCurrentUser().getEmail())))
                {
                    qh.imgrating.setBackgroundTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorPrimary)));
                    qh.txtRating.setTextColor(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorPrimary)));
                    qh.rating.setClickable(false);
                    break;
                }
        }
        else
            qh.txtRating.setText("0");
    }

    // Metodo che inizializza la logica del pulsante "Favorite" relativo alla domanda in questione
    // Il pultante Favorite permette di aggiungere/rimuovere la domanda corrente dalla lista dei preferiti
    private void initActionFavorite(final questionHolder qh)
    {
        // Inizializzazione della Action "Favorite"
        Util.getDatabase().getReference("User").child(Util.encodeEmail(getCurrentUser().getEmail())).child("favorites").child(questionKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() != null)
                    qh.imgfavorite.setImageTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAmber)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Click Listener relativo alla Action "Favorite" (stella)
        final DatabaseReference favoriteReference = Util.getDatabase().getReference("User").child(Util.encodeEmail(getCurrentUser().getEmail())).child("favorites").child(questionKey);
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
                            qh.imgfavorite.setImageTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorAmber)));
                        }
                        else
                        {
                            favoriteReference.removeValue();
                            qh.imgfavorite.setImageTintList(ColorStateList.valueOf(qh.context.getResources().getColor(R.color.colorDarkGray)));
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
    private void initActionQuestionReply(final questionHolder qh)
    {
        // Inizializzazione del comportamento del tasto "Rispondi":
        // Se la variaible booleana viene impostata a false, significa che l'utente ha già risposto alla domanda e non può più
        // effettuarne altre
        if (answerList.size() != 1)
            for(int i = 1; i < answerList.size(); i++)
                if (Util.encodeEmail(getCurrentUser().getEmail()).equals(answerList.get(i).user_key))
                {
                    answerAllowed = false;
                    break;
                }

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
    private void getAnswerInfo(final answerHolder ah, final Answer answer)
    {
        // Query per recuperare le informazioni dell'autore della risposta
        Util.getDatabase().getReference("User").child(answer.user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (!Util.isNetworkAvailable(ah.context) || dataSnapshot.child("photoUrl").getValue(String.class).equals(ah.context.getResources().getString(R.string.empty_profile_pic_url)))
                {
                    ah.imgProfile.setLetter(dataSnapshot.child("name").getValue(String.class));
                    ah.imgProfile.setShapeColor(Util.getLetterBackgroundColor(ah.context, dataSnapshot.child("name").getValue(String.class)));
                }
                else
                    Picasso.with(ah.imgProfile.getContext()).load(dataSnapshot.child("photoUrl").getValue(String.class)).fit().into(ah.imgProfile);

                ah.txtName.setText(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                ah.txtLvl.setText(String.valueOf(dataSnapshot.child("exp").getValue(Integer.class)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Impostazione delle informazioni relative alla risposta
        ah.txtDate.setText(Util.formatDate(answer.date));
        ah.txtDesc.setText(answer.desc);
        // Recupero dei commenti relativi alla risposta corrente
        initCommentList(ah, answer);
    }

    // Metodo per recupereare tutti i commenti di una relativa risposta
    private void initCommentList(final answerHolder ah, Answer answer)
    {
        ah.commentList = new ArrayList<>();
        ah.commentKeyList = new ArrayList<>();

        if (answer.comments != null)
            for(String key : answer.comments.keySet())
            {
                ah.commentList.add(answer.comments.get(key));
                ah.commentKeyList.add(key);
            }

        ah.cAdapter = new CommentAdapter(ah.commentList, ah.commentKeyList);
        ah.recyclerViewComment.setAdapter(ah.cAdapter);
    }

    // Metodo che inizializza la logica del pulsante "Like" relativo alla risposta in questione
    // Il pultante Like è cliccabile soltanto una volta e fornisce punti e crediti all'autore della domanda
    private void initActionLike(final answerHolder ah, final Answer answer, final String answerKey)
    {
        // Click Listener relativo alla Action "Like" (pollice)
        ah.like.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                if (Util.encodeEmail(getCurrentUser().getEmail()).equals(answer.user_key))
                    Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.detail_error_autolike, Snackbar.LENGTH_SHORT).show();
                else if (!Util.isNetworkAvailable(activity.getApplicationContext()))
                    Snackbar.make(activity.findViewById(R.id.l_detailContainer), R.string.detail_error_like_without_connection, Snackbar.LENGTH_LONG).show();
                else
                {
                    Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(answerKey).child("likes").child(Util.encodeEmail(getCurrentUser().getEmail())).setValue(true);
                    ah.imgLike.setBackgroundTintList(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue)));
                    ah.txtLike.setTextColor(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue)));
                    ah.txtLike.setText(String.valueOf(Integer.valueOf(String.valueOf(ah.txtLike.getText())) + 1));
                    ah.like.setClickable(false);
                    updateExpForLike(answer.user_key);
                }
            }
        });

        // Verifica se l'utente ha già inserito il "Like" per la risposta corrente
        ah.imgLike.setBackgroundTintList(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorDarkGray)));
        ah.txtLike.setTextColor((ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorDarkGray))));
        if (answer.likes != null)
        {
            // Inizializzazione del numero di "Like" della risposta corrente
            ah.txtLike.setText(String.valueOf(answer.likes.size()));

            for (String key : answer.likes.keySet())
                if (key.equals(Util.encodeEmail(getCurrentUser().getEmail())))
                {
                    ah.imgLike.setBackgroundTintList(ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue)));
                    ah.txtLike.setTextColor((ColorStateList.valueOf(ah.context.getResources().getColor(R.color.colorBlue))));
                    ah.like.setClickable(false);
                    break;
                }
        }
        else
            ah.txtLike.setText("0");
    }

    // Metodo che inizializza la logica del pulsante "Comment" relativo alla risposta in questione
    // Il pultante Comment permette di mostrare/nascondere i commenti relativi alla risposta corrente
    private void initActionComments(final answerHolder ah)
    {
        // Inizializzazione del numero di "Comment" della risposta corrente
        ah.txtComment.setText(String.valueOf(ah.commentList.size()));

        // Click Listener relativo alla Action "Comments"
        ah.layoutComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (ah.expandableLayout.isExpanded())
                    ah.expandableLayout.collapse();
                else
                {
                    ah.expandableLayout.expand();
                    ah.cAdapter = new CommentAdapter(ah.commentList, ah.commentKeyList);
                    ah.recyclerViewComment.setAdapter(ah.cAdapter);
                }
            }
        });
    }

    // Metodo che inizializza la logica del pulsante "Aggiungi commento" relativo alla risposta in questione
    private void initActionAnswerReply(final answerHolder ah, final String answerKey)
    {
        // Click Listener relativo al tasto "Commenta"
        ah.imgComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                openCommentDialog(answerKey);
            }
        });
    }

    // Metodo per aggiungere una risposta ad una determinata domanda
    private void openAnswerDialog()
    {
        final View dialogLayout = activity.getLayoutInflater().inflate(R.layout.alert_reply_layout, null);

        if (!Util.isNetworkAvailable(activity.getApplicationContext()) || getCurrentUser().getPhotoUrl().getPath().contains(activity.getApplicationContext().getResources().getString(R.string.empty_profile_pic_url)))
        {
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setLetter(getCurrentUser().getDisplayName());
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setShapeColor(Util.getLetterBackgroundColor(activity.getApplicationContext(), getCurrentUser().getDisplayName()));
        }
        else
            Picasso.with(activity.getApplicationContext())
                    .load(getCurrentUser().getPhotoUrl())
                    .fit()
                    .into((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto));

        ((TextView)dialogLayout.findViewById(R.id.reply_name)).setText(getCurrentUser().getDisplayName());
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
                                    answerAllowed = false;
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
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                    {
                        if (keyCode == KeyEvent.KEYCODE_BACK)
                            dialog.dismiss();
                        return true;
                    }
                })
                .setView(dialogLayout)
                .setCancelable(false);
        builder.show();
    }

    // Metodo per aggiungere un commento alla risposta corrente
    private void openCommentDialog(final String answerKey)
    {
        final View dialogLayout = activity.getLayoutInflater().inflate(R.layout.alert_reply_layout, null);

        if (!Util.isNetworkAvailable(activity.getApplicationContext()) || getCurrentUser().getPhotoUrl().equals(activity.getApplicationContext().getResources().getString(R.string.empty_profile_pic_url)))
        {
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setLetter(getCurrentUser().getDisplayName());
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setShapeColor(Util.getLetterBackgroundColor(activity.getApplicationContext(), getCurrentUser().getDisplayName()));
        }
        else
            Picasso.with(activity.getApplicationContext())
                    .load(getCurrentUser().getPhotoUrl())
                    .fit()
                    .into((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto));

        ((TextView)dialogLayout.findViewById(R.id.reply_name)).setText(getCurrentUser().getDisplayName());
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
                                    writeComment(((EditText) dialogLayout.findViewById(R.id.reply_desc)).getText().toString(), answerKey);
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
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                    {
                        if (keyCode == KeyEvent.KEYCODE_BACK)
                            dialog.dismiss();
                        return true;
                    }
                })
                .setView(dialogLayout)
                .setCancelable(false);
        builder.show();
    }

    // Metodo per inserire una risposta nel database e collegarlo al rispettivo autore
    private void writeAnswer(String desc)
    {
        String answer_key = Util.getDatabase().getReference("Question").child(questionKey).child("answers").push().getKey();

        Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(answer_key).setValue(new Answer(Util.encodeEmail(getCurrentUser().getEmail()), desc));
        Util.getDatabase().getReference("User").child(Util.encodeEmail(getCurrentUser().getEmail())).child("answers").child(answer_key).setValue(questionKey);

        updateExpForAnswer();
    }

    // Metodo per inserire un commento ad una risposta e collegarlo al rispettivo autore
    private void writeComment(String desc, String answerKey)
    {
        String comment_key = Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(answerKey).child("comments").push().getKey();

        Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(answerKey).child("comments").child(comment_key).setValue(
                new Comment(Util.encodeEmail(getCurrentUser().getEmail()), desc));
        Util.getDatabase().getReference("User").child(Util.encodeEmail(getCurrentUser().getEmail())).child("comments").child(comment_key).setValue(true);
    }

    // Metodo che viene utilizzato per aggiungere i crediti a fronte di una risposta data e di aumentare l'exp dell'utente
    private void updateExpForAnswer()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(getCurrentUser().getEmail()))
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

    // Metodo per aggiornare in tempo reale il numero di rating della domanda
    public void refreshRating(Question question)
    {
        this.question = question;
        notifyItemChanged(0, UPDATE_CODE_RATING);
    }

    // Metodo per aggiornare in tempo reale l'aggiunta di una domanda nuova
    public void refreshNewAnswer(Answer answer, String answerKey)
    {
        answerList.add(answer);
        answerKeyList.add(answerKey);
        notifyItemInserted(answerList.size() - 1);
        notifyItemChanged(0, UPDATE_CODE_ANSWER);
    }

    // Metodo per aggiornare in tempo reale l'aggiunta di un like ad una risposta
    public void refreshAnswerLikes(Answer answer, String answerKey, String likeKey)
    {
        int position;

        // Recupero della posizione della risposta modificata
        for(position = 0; position < answerKeyList.size(); position++)
            if (answerKey.equals(answerKeyList.get(position)))
                break;

        // Aggiornamento grafico del numero di like
        if (answerList.get(position).likes != null)
        {
            // Se la chiave del like passato come parametro non è presente tra i like della risposta,
            // quest'ultima viene aggiornata (se è già presente, si tratta del trigget iniziale della query di firebase)
            boolean likeIsNew = true;
            for(String key : answerList.get(position).likes.keySet())
                if (likeKey.equals(key))
                {
                    likeIsNew = false;
                    break;
                }
            if (likeIsNew)
            {
                answerList.set(position, answer);
                notifyItemChanged(position, UPDATE_CODE_LIKES);
            }
        }
        else
        {
            answerList.set(position, answer);
            notifyItemChanged(position, UPDATE_CODE_LIKES);
        }
    }

    // Metodo per aggiornare in tempo reale l'aggiunta di un like ad una risposta
    public void refreshAnswerComments(String answerKey, Comment comment, String commentKey)
    {
        int position;

        // Recupero della posizione della risposta modificata
        for(position = 0; position < answerKeyList.size(); position++)
            if (answerKey.equals(answerKeyList.get(position)))
                break;

        newComment = comment;
        newCommentKey = commentKey;
        notifyItemChanged(position, UPDATE_CODE_COMMENTS);
    }

    // Metodo utilizzato per sapere se la risposta passata come parametro è già presente nella lista
    public boolean containsAnswerKey(String answerKey)
    {
        for(String key : answerKeyList)
            if (answerKey.equals(key)) return true;
        return false;
    }
}
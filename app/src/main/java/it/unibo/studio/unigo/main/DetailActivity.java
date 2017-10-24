package it.unibo.studio.unigo.main;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.QuestionDetailAdapter;
import it.unibo.studio.unigo.utils.BackgroundService;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Comment;
import it.unibo.studio.unigo.utils.firebase.Question;

public class DetailActivity extends AppCompatActivity
{
    private final static int REQUEST_FILE_PERMISSION = 1;
    private Question question;
    private QuestionDetailAdapter mAdapter;
    private RecyclerView recyclerViewQuestionDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        initialize();
    }

    // Alla chiusura dell'activity, viene restituito l'id della domanda per aggiornare il campo "favorite"
    @Override
    public void onBackPressed()
    {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("question_key", getIntent().getStringExtra("question_key"));
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
        super.onBackPressed();
    }

    // Metodo per inizializzare l'oggetto che memorizza tutti i dati relativi alla domanda attuale
    private void initialize()
    {
        Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                question = dataSnapshot.getValue(Question.class);
                initComponents();
                initQuestionChangeListener();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        BackgroundService.resetNotification();
    }

    private void initComponents()
    {
        // Inizializzazione Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.questionToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        // Inizializzazione RecyclerView
        recyclerViewQuestionDetail = (RecyclerView) findViewById(R.id.recyclerViewAnswer);
        recyclerViewQuestionDetail.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        mAdapter = new QuestionDetailAdapter(question, getIntent().getStringExtra("question_key"), this);

        recyclerViewQuestionDetail.setAdapter(mAdapter);
    }

    // Listener sui cambiamenti della domanda in questione per poter efettuare l'aggiornamento realtime della GUI
    private void initQuestionChangeListener()
    {
        final DatabaseReference questionReference = Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key"));

        // Refresh in realtime del rating della domanda
        setRatingOnChangeListener(questionReference);

        // Aggiunta/modifica in realtime di una risposta
        setAnswerOnAddListener(questionReference);
    }

    // Refresh in realtime del rating della domanda
    private void setRatingOnChangeListener(DatabaseReference questionReference)
    {
        questionReference.child("ratings").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                refreshRating();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Aggiunta/modifica in realtime di una risposta
    private void setAnswerOnAddListener(DatabaseReference questionReference)
    {
        questionReference.child("answers").orderByKey().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Se la risposta non è presente nella GUI, allora viene aggiunta
                if (!mAdapter.containsAnswerKey(dataSnapshot.getKey()))
                    mAdapter.refreshNewAnswer(dataSnapshot.getValue(Answer.class), dataSnapshot.getKey());

                // Per ogni risposta alla domanda viene avviato il listener sui suoi cambiamenti (like/commenti)
                setAnswerOnChangeListener(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per attacare un listener ad ogni risposta della domanda per individuarne i cambiamenti (like/commenti)
    private void setAnswerOnChangeListener(final String answerKey)
    {
        // Listener sui like della risposta
        Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key")).child("answers").child(answerKey).child("likes").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                refreshAnswerLikes(answerKey, dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Listener sui commenti della risposta
        Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key")).child("answers").child(answerKey).child("comments").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                mAdapter.refreshAnswerComments(answerKey, dataSnapshot.getValue(Comment.class), dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per aggiornare il numero di rating della domanda corrente
    private void refreshRating()
    {
        Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                mAdapter.refreshRating(dataSnapshot.getValue(Question.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per aggiornare la domanda corrente (per recuperare il valore aggiornato di rating, num domande, etc..)
    private void refreshAnswerLikes(String answerKey, final String likeKey)
    {
        Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key")).child("answers").child(answerKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                mAdapter.refreshAnswerLikes(dataSnapshot.getValue(Answer.class), dataSnapshot.getKey(), likeKey);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    public void collapseCommentList(int position)
    {
        QuestionDetailAdapter.answerHolder holder = (QuestionDetailAdapter.answerHolder) recyclerViewQuestionDetail.findViewHolderForAdapterPosition(position);
        holder.closeCommentList();
    }

    // Metodo che consente di verificare ed eventualmente richiedere i permessi di scrittura su memoria di massa,
    // necessari per scaricare gli allegati delle domande
    public void getWritePermission()
    {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FILE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_FILE_PERMISSION)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                mAdapter.docAdapter.downloadFileAfterPermissions();
            else if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                new MaterialDialog.Builder(this)
                        .title(getString(R.string.permission_denied))
                        .content(getString(R.string.permission_needed_read))
                        .positiveText(getString(R.string.drawer_impostazioni))
                        .positiveColor(ContextCompat.getColor(this, R.color.colorAccent))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.setData(Uri.fromParts(getString(R.string.intent_package), getPackageName(), null));
                                startActivity(intent);
                            }
                        })
                        .negativeText(getString(R.string.alert_dialog_cancel))
                        .negativeColor(ContextCompat.getColor(this, R.color.colorAccent))
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .build()
                        .show();
            }
    }
}
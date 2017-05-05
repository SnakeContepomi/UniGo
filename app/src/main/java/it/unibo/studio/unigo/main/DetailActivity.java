package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.DetailAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Question;

public class DetailActivity extends AppCompatActivity
{
    private Question question;
    private DetailAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        initialize();
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
        RecyclerView recyclerViewQuestionDetail = (RecyclerView) findViewById(R.id.recyclerViewAnswer);
        recyclerViewQuestionDetail.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        mAdapter = new DetailAdapter(question, getIntent().getStringExtra("question_key"), this);

        recyclerViewQuestionDetail.setAdapter(mAdapter);
    }

    // Listener sui cambiamenti della domanda in questione per poter efettuare l'aggiornamento realtime della GUI
    private void initQuestionChangeListener()
    {
        final DatabaseReference questionReference = Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key"));

        // Refresh in realtime del rating della domanda
        setRatingOnChangeListener(questionReference);

        // Aggiunta/modifica in realtime di una risposta
        setAnswerChangeListener(questionReference);
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
    private void setAnswerChangeListener(DatabaseReference questionReference)
    {
        questionReference.child("answers").orderByKey().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                if (!mAdapter.containsAnswerKey(dataSnapshot.getKey()))
                    refreshNewAnswer(dataSnapshot.getValue(Answer.class), dataSnapshot.getKey());
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

    // Metodo per aggiornare la domanda corrente (per recuperare il valore aggiornato di rating, num domande, etc..)
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

    // Metodo per aggiornare la GUI aggiungendo l'ultima domanda inserita
    private void refreshNewAnswer(Answer answer, String answerKey)
    {
        mAdapter.refreshNewAnswer(answer, answerKey);
    }
}
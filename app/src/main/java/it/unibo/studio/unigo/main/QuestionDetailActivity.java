package it.unibo.studio.unigo.main;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.main.adapters.DetailAdapter;
import it.unibo.studio.unigo.main.adapteritems.DetailAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Question;


public class QuestionDetailActivity extends AppCompatActivity
{
    private QuestionAdapterItem question;
    private String user_name;

    private RecyclerView recyclerViewQuestionDetail;
    private List<DetailAdapterItem> answerList;
    private DetailAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        initComponents();
    }

    private void initComponents() {
        answerList = new ArrayList<>();
        answerList.add(null);
        question = new QuestionAdapterItem((Question) getIntent().getExtras().getSerializable("question"),
                                            getIntent().getExtras().getString("question_key"),
                getIntent().getExtras().getString("photo_url"));
        // Inizializzazione Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.questionToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        recyclerViewQuestionDetail = (RecyclerView) findViewById(R.id.recyclerViewQuestionDetail);
        recyclerViewQuestionDetail.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        getQuestionUserInfo();
        initAnswerList();
    }

    private void getQuestionUserInfo()
    {
        Util.getDatabase().getReference("User").child(question.getQuestion().user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                user_name = dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void initAnswerList()
    {
        Util.getDatabase().getReference("Question").child(question.getQuestionKey()).child("answers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                final Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                // Per ogni chiave recuperata, viene eseguita una query sulla tabella "Question" al fine di ottenerne
                // i dettagli. Ogni domanda viene quindi inserita nella lista presente in Utils ed infine
                // viene inizializzato il listener per sull'inserimento delle nuove domande
                while (iterator.hasNext())
                {
                    final DataSnapshot answer = iterator.next();

                    Util.getDatabase().getReference("User").child(answer.getValue(Answer.class).user_key).child("photoUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {

                            answerList.add(new DetailAdapterItem(answer.getValue(Answer.class), dataSnapshot.getValue(String.class)));
                            if (!iterator.hasNext())
                            {
                                mAdapter = new DetailAdapter(answerList, question, user_name);
                                recyclerViewQuestionDetail.setAdapter(mAdapter);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}
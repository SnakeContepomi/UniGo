package it.unibo.studio.unigo.main;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Util;

import static it.unibo.studio.unigo.R.id.txtQDKey;

public class QuestionDetailActivity extends AppCompatActivity
{
    private String questionId;
    private TextView txtQDKey;
    private EditText etQuestDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        initComponents();
        getQuestion();
    }

    private void initComponents()
    {
        questionId = getIntent().getStringExtra("questionId");
        txtQDKey = (TextView) findViewById(R.id.txtQDKey);
        etQuestDesc = (EditText) findViewById(R.id.etQuestDesc);

        txtQDKey.setText(questionId);
    }

    private void getQuestion()
    {
        Util.getDatabase().getReference("Question").child(questionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                etQuestDesc.setText(dataSnapshot.child("desc").getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}
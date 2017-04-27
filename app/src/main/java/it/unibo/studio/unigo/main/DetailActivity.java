package it.unibo.studio.unigo.main;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.DetailAdapterItem;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.main.adapters.DetailAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Question;

import static android.R.attr.dialogLayout;

public class DetailActivity extends AppCompatActivity
{
    private List<DetailAdapterItem> answerList;

    private RecyclerView recyclerViewQuestionDetail;
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

        // Inizializzazione Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.questionToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        // Inizializzazione RecyclerView
        recyclerViewQuestionDetail = (RecyclerView) findViewById(R.id.recyclerViewAnswer);
        recyclerViewQuestionDetail.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        // Inizializzazione Fab
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fabDetail);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                writeAnswer();
            }
        });

        initAnswerList();
    }

    private void initAnswerList()
    {
        Util.getDatabase().getReference("Question").child(getIntent().getStringExtra("question_key")).child("answers").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
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

                            answerList.add(new DetailAdapterItem(answer.getValue(Answer.class), answer.getKey(), dataSnapshot.getValue(String.class)));
                            if (!iterator.hasNext())
                            {
                                mAdapter = new DetailAdapter(answerList, getIntent().getStringExtra("question_key"));
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

    // Metodo per aggiungere una risposta ad una determinata domanda
    private void writeAnswer()
    {
        View dialogLayout = getLayoutInflater().inflate(R.layout.alert_reply_layout, null);
        if (!Util.isNetworkAvailable(getApplicationContext()) || Util.getCurrentUser().getPhotoUrl().equals(getResources().getString(R.string.empty_profile_pic_url)))
        {
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setLetter(Util.getCurrentUser().getDisplayName());
            ((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto)).setShapeColor(Util.getLetterBackgroundColor(getApplicationContext(), Util.getCurrentUser().getDisplayName()));
        }
        else
            Picasso.with(getApplicationContext())
                    .load(Util.getCurrentUser().getPhotoUrl())
                    .into((MaterialLetterIcon) dialogLayout.findViewById(R.id.reply_userPhoto));
        ((TextView)dialogLayout.findViewById(R.id.reply_name)).setText(Util.getCurrentUser().getDisplayName());
        ((EditText)dialogLayout.findViewById(R.id.reply_desc)).setHint(R.string.detail_write_answer);
        AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this)
                .setPositiveButton(getString(R.string.alert_dialog_send),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // Altro
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(getString(R.string.alert_dialog_cancel),
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
}
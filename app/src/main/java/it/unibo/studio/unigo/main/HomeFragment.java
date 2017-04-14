package it.unibo.studio.unigo.main;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.QuestionAdapter;
import it.unibo.studio.unigo.utils.QuestionAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Question;
import it.unibo.studio.unigo.utils.firebase.User;

public class HomeFragment extends Fragment
{
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private DividerItemDecoration divider;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        initComponents(v);
        retrieveUserInfo();

        return v;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopQuestionListener();
    }

    private void initComponents(View v)
    {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewQuestion);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(Util.getQuestionList());
        mRecyclerView.setAdapter(mAdapter);
        divider = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divider, null));

        loadQuestionFromList();
    }

    // Memorizzazione utente corrente per poter effettuare operazioni anche in modalità offline
    private void retrieveUserInfo()
    {
        if (Util.CURRENT_COURSE_KEY == null)
        {
            mRecyclerView.setVisibility(View.GONE);
            Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail()))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {
                            User u = dataSnapshot.getValue(User.class);
                            Util.CURRENT_COURSE_KEY = u.courseKey;
                            startQuestionListener();
                            mRecyclerView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });
        }
    }

    // Metodo che recupera gli id di tutte le domande del corso corrente.
    // Viene aggiunto un listener sulle domande recuperate per poter individuare le nuove domande/cambiamenti
    private void startQuestionListener()
    {
        // Listener sul campo "questions" della tabella Course per recuperare tutte le domande relative a quel corso
        // e per poter gestire gestire anche le domande che verranno inserite in futuro
        Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("questions").orderByValue().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Per ogni chiave del corso trovata, vengono recuperate le relative informazioni dalla tabella Question
                // e viene aggiunto un elemento nella RecyclerView
                Util.getDatabase().getReference("Question").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        addQuestionIntoRecyclerView(dataSnapshot.getValue(Question.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });

                // Viene agganciata ad ogni domanda recuperata il listener che ne cattura gli eventuali cambiamenti
                addOnChangeListenerToQuestion(dataSnapshot.getKey());
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


    // Metodo per aggiungere alla RecyclerView la domanda passata come parametro
    private void addQuestionIntoRecyclerView(final Question question)
    {
        // Viene recuperato l'utente che ha effettuato la domanda, in modo da caricare la sua foto profilo
        Util.getDatabase().getReference("User").child(question.user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Util.getQuestionList().add(0, new QuestionAdapterItem(question, dataSnapshot.getValue(User.class).photoUrl));
                mRecyclerView.addItemDecoration(divider);
                mAdapter.notifyItemInserted(0);
                mRecyclerView.scrollToPosition(0);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per agganciare ad una domanda il listener sui suoi cambiamenti
    private void addOnChangeListenerToQuestion(String question_key)
    {
        // Listener sui cambiamenti del post appena inserito (commenti, like, ...)
        Util.getDatabase().getReference("Question").child(question_key).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                //Toast.makeText(getActivity().getApplicationContext(), dataSnapshot.getKey() + ": " + dataSnapshot.getValue(String.class), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo utilizzato per caricare le domande già recuperate dal database e presenti nella lista questionList
    private void loadQuestionFromList()
    {
        for(int i = 0; i < Util.getQuestionList().size(); i++)
        {
            mRecyclerView.addItemDecoration(divider);
            mAdapter.notifyItemInserted(i);
            mRecyclerView.scrollToPosition(0);
        }
    }

    private void stopQuestionListener()
    {
        //Util.getDatabase().getReference("Question").orderByChild("date").equalTo(Util.CURRENT_COURSE_KEY, "course_key").removeEventListener(questionListener);
    }
}
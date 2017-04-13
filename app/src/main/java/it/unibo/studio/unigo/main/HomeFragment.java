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
import java.util.ArrayList;
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
    private RecyclerView.LayoutManager mLayoutManager;
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
        mLayoutManager = new LinearLayoutManager(v.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(Util.questionList);
        mRecyclerView.setAdapter(mAdapter);
        divider = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divider, null));
        mAdapter.notifyDataSetChanged();
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

    private void startQuestionListener()
    {
        // Listener sull'inserimento di nuovi post riguardanti il corso dell'utente
        Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("questions").orderByValue().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Ottenuta la chiave di una domanda, vengono recuperate tutte le sue informazioni dalla tabella Question
                Util.getDatabase().getReference("Question").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        final Question q = dataSnapshot.getValue(Question.class);
                        // Viene recuperato l'utente che ha effettuato la domanda, in modo da caricare la sua foto profilo
                        Util.getDatabase().getReference("User").child(q.user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                Util.questionList.add(0, new QuestionAdapterItem(q, dataSnapshot.getValue(User.class).photoUrl));

                                mRecyclerView.addItemDecoration(divider);
                                mAdapter.notifyItemInserted(0);
                                mRecyclerView.scrollToPosition(0);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                // Listener sui cambiamenti del post appena inserito (commenti, like, ...)
                Util.getDatabase().getReference("Question").child(dataSnapshot.getKey()).addChildEventListener(new ChildEventListener() {
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

    private void stopQuestionListener()
    {
        //Util.getDatabase().getReference("Question").orderByChild("date").equalTo(Util.CURRENT_COURSE_KEY, "course_key").removeEventListener(questionListener);
    }
}
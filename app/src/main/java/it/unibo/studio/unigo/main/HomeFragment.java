package it.unibo.studio.unigo.main;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.fabtransitionactivity.SheetLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.QuestionAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Question;

public class HomeFragment extends Fragment
{
    private List<Question> questionList;
    private ChildEventListener questionListener;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        initComponents(v);

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
        questionList = new ArrayList<Question> ();

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewQuestion);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(v.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(questionList);
        mRecyclerView.setAdapter(mAdapter);

        startQuestionListener();
    }

    private void startQuestionListener()
    {
        questionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                Question q = dataSnapshot.getValue(Question.class);
                questionList.add(0, q);
                mAdapter.notifyItemInserted(0);
                mRecyclerView.scrollToPosition(0);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };

        //Util.getDatabase().getReference("Question").orderByChild("course_key").equalTo(Util.CURRENT_COURSE_KEY).addChildEventListener(questionListener);

        Util.getDatabase().getReference("Question").orderByChild("date").startAt(Util.CURRENT_COURSE_KEY, "course_key").addChildEventListener(questionListener);
    }

    private void stopQuestionListener()
    {
        Util.getDatabase().getReference("Question").orderByChild("course_key").equalTo(Util.CURRENT_COURSE_KEY).removeEventListener(questionListener);
    }
}
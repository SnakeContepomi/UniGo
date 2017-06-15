package it.unibo.studio.unigo.main.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.main.adapters.QuestionAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Question;

public class HomeFragment extends android.support.v4.app.Fragment
{
    private RecyclerView mRecyclerView;
    private LinearLayout wheel;
    private QuestionAdapter mAdapter;
    private List<QuestionAdapterItem> questionList;
    private ChildEventListener questionListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        initComponents(v);
        return v;
    }

    @Override
    public void onDestroyView()
    {
        Util.getDatabase().getReference("Question").orderByKey().removeEventListener(questionListener);
        super.onDestroyView();
    }

    private void initComponents(View v)
    {
        questionList = new ArrayList<>();

        wheel = (LinearLayout) v.findViewById(R.id.homeWheelLayout);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewHome);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(questionList, getActivity());
        mRecyclerView.setAdapter(mAdapter);

        questionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                if (getQuestionPosition(dataSnapshot.getKey()) == -1)
                {
                    questionList.add(0, new QuestionAdapterItem(dataSnapshot.getValue(Question.class), dataSnapshot.getKey()));
                    mAdapter.notifyItemInserted(0);
                    setRecyclerViewVisibility(true);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                if (getQuestionPosition(dataSnapshot.getKey()) != -1)
                {
                    QuestionAdapterItem newQItem = new QuestionAdapterItem(dataSnapshot.getValue(Question.class), dataSnapshot.getKey());
                    questionList.set(getQuestionPosition(dataSnapshot.getKey()), newQItem);
                    mAdapter.refreshQuestion(newQItem);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };

        Util.getDatabase().getReference("Question").orderByKey().addChildEventListener(questionListener);

        new CountDownTimer(3000, 3000)
        {
            public void onTick(long millisUntilFinished) { }

            public void onFinish()
            {
                if (questionList.isEmpty())
                    wheel.setVisibility(View.GONE);
            }
        }.start();
    }

    // Metodo utilizzato per nascondere/mostrare la recyclerview
    private void setRecyclerViewVisibility(boolean b)
    {
        if (b)
        {
            mRecyclerView.scrollToPosition(0);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        else
            mRecyclerView.setVisibility(View.GONE);
    }

    // Metodo che restituisce la posizione della domanda nella lista, in base alla chiave fornita
    private int getQuestionPosition(String questionKey)
    {
        for(int i = 0; i < questionList.size(); i++)
            if (questionList.get(i).getQuestionKey().equals(questionKey))
                return i;
        return -1;
    }

    // Metodo per aggiornare la GUI Favorite della domanda passata come parametro
    public void refreshFavorite(String questionKey)
    {
        mAdapter.refreshFavorite(questionKey);
    }

    // Metodo per filtrare la ricerca di domande, in base al titolo, descrizione o materia
    public void filterResults(String filterConstraint)
    {
        mAdapter.getFilter().filter(filterConstraint);
    }

    // Metodo per riempire la lista con tutte le domande presenti nella lista in Util
    public void resetFilter()
    {
        mAdapter.resetFilter(questionList);
    }
}
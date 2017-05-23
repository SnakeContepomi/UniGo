package it.unibo.studio.unigo.main.fragments;

import android.app.Fragment;
import android.os.Bundle;
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
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.main.adapters.FavoriteAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Question;

public class FavoriteFragment extends Fragment
{
    private RecyclerView mRecyclerView;
    private FavoriteAdapter mAdapter;
    private List<QuestionAdapterItem> favoriteList;
    private ValueEventListener changeListener;
    private ChildEventListener favoriteListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_favorite, container, false);
        initComponents(v);
        return v;
    }

    @Override
    public void onDestroyView()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").orderByKey().removeEventListener(favoriteListener);
        for(QuestionAdapterItem item : favoriteList)
            removeQuestionListener(item.getQuestionKey());
        super.onDestroyView();
    }

    private void initComponents(View v)
    {
        favoriteList = new ArrayList<>();

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewFavorite);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        // Inizializzazione adapter della lista delle domande
        mAdapter = new FavoriteAdapter(favoriteList, getActivity());
        mRecyclerView.setAdapter(mAdapter);

        changeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (getQuestionPosition(dataSnapshot.getKey()) != -1)
                {
                    QuestionAdapterItem newQItem = new QuestionAdapterItem(dataSnapshot.getValue(Question.class), dataSnapshot.getKey());

                    favoriteList.set(getQuestionPosition(dataSnapshot.getKey()), newQItem);
                    mAdapter.refreshQuestion(newQItem);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };

        favoriteListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                Util.getDatabase().getReference("Question").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        favoriteList.add(0, new QuestionAdapterItem(dataSnapshot.getValue(Question.class), dataSnapshot.getKey()));
                        mAdapter.addToBackupList(new QuestionAdapterItem(dataSnapshot.getValue(Question.class), dataSnapshot.getKey()));
                        addQuestionListener(dataSnapshot.getKey());
                        mAdapter.notifyItemInserted(0);
                        setRecyclerViewVisibility(true);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot)
            {
                removeQuestionListener(dataSnapshot.getKey());
                mAdapter.removeQuestion(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };

        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").orderByKey().addChildEventListener(favoriteListener);
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
        for(int i = 0; i < favoriteList.size(); i++)
            if (favoriteList.get(i).getQuestionKey().equals(questionKey))
                return i;
        return -1;
    }

    // Metodo per filtrare la ricerca di domande, in base al titolo, descrizione o materia
    public void filterResults(String filterConstraint)
    {
        mAdapter.getFilter().filter(filterConstraint);
    }

    // Metodo per riempire la lista con tutte le domande presenti nella lista in Util
    public void resetFilter()
    {
        mAdapter.resetFilter();
    }

    private void addQuestionListener(String questionKey)
    {
        Util.getDatabase().getReference("Question").child(questionKey).addValueEventListener(changeListener);
    }

    private void removeQuestionListener(String questionKey)
    {
        Util.getDatabase().getReference("Question").child(questionKey).removeEventListener(changeListener);
    }
}
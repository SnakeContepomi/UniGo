package it.unibo.studio.unigo.main.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.main.adapters.QuestionAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Question;

public class HomeFragment extends Fragment
{
    private RecyclerView mRecyclerView;
    private QuestionAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        initComponents(v);
        return v;
    }

    @Override
    public void onPause()
    {
        Util.setHomeFragmentVisibility(false);
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Se ci sono aggiornamenti effettuati in background, vengono applicati alla lista Utils
        // e a quella utilizzata dall'Adapater
        for(QuestionAdapterItem qitem : Util.getQuestionsToUpdate())
            Util.getHomeFragment().refreshQuestion(qitem.getQuestionKey(), qitem.getQuestion());
        Util.getQuestionsToUpdate().clear();
        Util.setHomeFragmentVisibility(true);
    }

    private void initComponents(View v)
    {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewQuestion);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(Util.getQuestionList(), getActivity());
        mRecyclerView.setAdapter(mAdapter);
        loadQuestionFromList();
    }

    // Metodo utilizzato per caricare le domande già recuperate dal database e presenti nella lista questionList
    public void loadQuestionFromList()
    {
        if (Util.getQuestionList().size() != 0)
        {
            for(int i = 0; i < Util.getQuestionList().size(); i++)
                mAdapter.notifyItemInserted(i);
            setRecyclerViewVisibility(true);
        }
    }

    // Metodo utilizzato per aggiornare l'aggiunta dell'elemento in posizione "position" nella recyclerview
    public void updateElement(int position)
    {
        mAdapter.updateElement(position);
        mRecyclerView.scrollToPosition(position);
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

    // Metodo per aggiornare la GUI della domanda passata come parametro (i tre campi Rating, Favorite e Answer)
    // e aggiornare i valori della lista di domande in Util
    public void refreshQuestion(String questionKey, Question question)
    {
        if (Util.getQuestionPosition(questionKey) != -1)
            Util.updateElementAt(Util.getQuestionPosition(questionKey), new QuestionAdapterItem(question, questionKey));
        mAdapter.refreshQuestion(new QuestionAdapterItem(question, questionKey));
    }

    // Metodo per aggiornare la GUI di favorite della domanda passata come parametro
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
        mAdapter.resetFilter();
    }

    // Metodo utilizzato per modificare lo stato della ricerca (attiva o non attiva)
    public void setFilterState(boolean state)
    {
        mAdapter.setFilterState(state);
    }
}
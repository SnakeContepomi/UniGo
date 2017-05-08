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
        Util.setHomeFragmentVisibility(true);
        for(QuestionAdapterItem qitem : Util.getQuestionsToUpdate())
            refreshQuestion(qitem.getQuestionKey(), qitem.getQuestion());
        Util.getQuestionsToUpdate().clear();
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

    // Metodo utilizzato per aggiornare l'elemento in posizione "position" nella recyclerview
    public void updateElement(int position)
    {
        mAdapter.notifyItemInserted(position);
        mRecyclerView.scrollToPosition(0);
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

    // Metodo per aggiornare la GUI della domanda passata come parametro
    public void refreshQuestion(String questionKey, Question question)
    {
        if (Util.getQuestionPosition(questionKey) != -1)
            mAdapter.refreshQuestion(Util.getQuestionPosition(questionKey), new QuestionAdapterItem(question, questionKey));
    }

    // Metodo per aggiornare la GUI di favorite della domanda passata come parametro
    public void refreshFavorite(String questionKey)
    {
        if (Util.getQuestionPosition(questionKey) != -1)
            mAdapter.refreshFavorite(Util.getQuestionPosition(questionKey));
    }
}
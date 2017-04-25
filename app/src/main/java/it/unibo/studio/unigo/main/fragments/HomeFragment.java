package it.unibo.studio.unigo.main.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.QuestionAdapter;
import it.unibo.studio.unigo.utils.Util;

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
    }

    private void initComponents(View v)
    {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewQuestion);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(Util.getQuestionList());
        mRecyclerView.setAdapter(mAdapter);
        divider = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(v.getContext().getDrawable(R.drawable.item_divider));
        loadQuestionFromList();
    }

    // Metodo utilizzato per caricare le domande già recuperate dal database e presenti nella lista questionList
    public void loadQuestionFromList()
    {
        if (Util.getQuestionList().size() != 0)
        {
            for(int i = 0; i < Util.getQuestionList().size(); i++)
            {
                mRecyclerView.addItemDecoration(divider);
                mAdapter.notifyItemInserted(i);
            }
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
}
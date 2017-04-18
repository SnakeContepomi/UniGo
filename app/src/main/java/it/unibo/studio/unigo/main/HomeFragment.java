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
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.QuestionAdapter;
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
        //loadQuestionFromList();
        Util.setHomeFragmentVisibility(true);
    }

    private void initComponents(View v)
    {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewQuestion);
        mRecyclerView.setVisibility(View.GONE);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(Util.getQuestionList());
        mRecyclerView.setAdapter(mAdapter);
        divider = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divider, null));

        loadQuestionFromList();
        Util.setHomeFragmentVisibility(true);
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
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    public void updateElement(int position)
    {
        mAdapter.notifyItemInserted(position);
        mRecyclerView.scrollToPosition(0);
    }
}
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
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.QuestionAdapter;

public class HomeFragment extends Fragment
{
    private FloatingActionButton fab;
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
    public void onStop()
    {
        super.onStop();
        fab.hide();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        fab.show();
    }

    private void initComponents(View v)
    {
        fab = (FloatingActionButton) getActivity().findViewById(R.id.fabHome);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                ((SheetLayout) getActivity().findViewById(R.id.bottom_sheet)).expandFab();
            }
        });

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewQuestion);

        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(v.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Inizializzazione adapter della lista delle domande
        mAdapter = new QuestionAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);

    }
}
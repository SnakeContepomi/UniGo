package it.unibo.studio.unigo.main.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.main.adapters.FavoriteAdapter;
import it.unibo.studio.unigo.utils.Util;

public class ChatFragment extends android.support.v4.app.Fragment
{
    private RecyclerView mRecyclerView;
    private LinearLayout wheel;
    private FavoriteAdapter mAdapter;
    private List<QuestionAdapterItem> chatList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        initComponents(v);
        return v;
    }

    private void initComponents(View v)
    {
        chatList = new ArrayList<>();

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewChat);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        wheel = (LinearLayout) v.findViewById(R.id.chatWheelLayout);

        // Inizializzazione adapter della lista delle domande
        mAdapter = new FavoriteAdapter(chatList, getActivity());
        mRecyclerView.setAdapter(mAdapter);

        //Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").orderByKey().addChildEventListener(favoriteListener);

        new CountDownTimer(3000, 3000)
        {
            public void onTick(long millisUntilFinished) { }

            public void onFinish()
            {
                if (chatList.isEmpty())
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
}
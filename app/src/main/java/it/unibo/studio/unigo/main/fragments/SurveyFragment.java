package it.unibo.studio.unigo.main.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.SurveyAdapterItem;
import it.unibo.studio.unigo.main.adapters.SurveyAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Survey;

public class SurveyFragment extends android.support.v4.app.Fragment
{
    private RecyclerView rvSurv;
    private LinearLayout wheel;
    private ChildEventListener surveyListener;
    private SurveyAdapter survAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_survey, container, false);
        initComponents(v);
        return v;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Util.getDatabase().getReference("Survey").removeEventListener(surveyListener);
    }

    private void initComponents(View v)
    {
        wheel = (LinearLayout) v.findViewById(R.id.favoriteWheelLayout);

        rvSurv = (RecyclerView) v.findViewById(R.id.rvSurv);
        rvSurv.setHasFixedSize(true);
        rvSurv.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        survAdapter = new SurveyAdapter(getActivity());
        rvSurv.setAdapter(survAdapter);
        setRecyclerViewVisibility(false);

        surveyListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Se il sondaggio appartiene allo stesso corso a cui è iscritto l'utente, viene inserito
                if (dataSnapshot.getValue(Survey.class).course_key.equals(Util.CURRENT_COURSE_KEY))
                {
                    // Se il sondaggio non è ancora stato inserito nella lista, viene inserito
                    if (survAdapter.getSurveyPosition(dataSnapshot.getKey()) == -1)
                    {
                        survAdapter.addElement(new SurveyAdapterItem(dataSnapshot.getKey(), dataSnapshot.getValue(Survey.class)));
                        setRecyclerViewVisibility(true);
                    }
                }
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

        new CountDownTimer(3000, 3000)
        {
            public void onTick(long millisUntilFinished) { }

            public void onFinish()
            {
                if (survAdapter.getItemCount() == 0)
                    wheel.setVisibility(View.GONE);
            }
        }.start();

        Util.getDatabase().getReference("Survey").orderByKey().addChildEventListener(surveyListener);
    }

    // Metodo utilizzato per nascondere/mostrare la recyclerview
    private void setRecyclerViewVisibility(boolean b)
    {
        if (b)
        {
            rvSurv.scrollToPosition(0);
            rvSurv.setVisibility(View.VISIBLE);
        }
        else
            rvSurv.setVisibility(View.GONE);
    }
}
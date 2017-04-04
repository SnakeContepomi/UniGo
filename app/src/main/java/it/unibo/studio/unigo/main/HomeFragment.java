package it.unibo.studio.unigo.main;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import it.unibo.studio.unigo.R;

public class HomeFragment extends Fragment
{
    private FloatingActionButton fab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        initComponents();

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

    private void initComponents()
    {
        fab = (FloatingActionButton) getActivity().findViewById(R.id.fabHome);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(getActivity(), PostActivity.class);
                startActivity(intent);
            }
        });
    }
}
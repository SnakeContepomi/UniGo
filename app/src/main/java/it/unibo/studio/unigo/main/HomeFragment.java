package it.unibo.studio.unigo.main;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import it.unibo.studio.unigo.R;

public class HomeFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        initComponents(v);

        return v;
    }

    private void initComponents(View v)
    {

    }
}
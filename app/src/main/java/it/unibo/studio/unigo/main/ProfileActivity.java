package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import it.unibo.studio.unigo.R;

public class ProfileActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        //overridePendingTransition(entrante, uscente)
        overridePendingTransition(R.anim.target_activity_creation, R.anim.main_activity_dissolve);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
    }

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.main_activity_creation, R.anim.target_activity_dissolve);
        super.onPause();
    }
}
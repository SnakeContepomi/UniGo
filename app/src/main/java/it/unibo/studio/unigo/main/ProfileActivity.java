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
        //overridePendingTransition(Animazione_activity_entrante, Animazione_activity_uscente)
        overridePendingTransition(R.anim.swap_in_bottom, R.anim.swap_out_bottom);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
    }

    @Override
    protected void onPause()
    {
        //overridePendingTransition(R.anim.activity_no_animation, R.anim.swap_out_bottom);
        super.onPause();
    }
}
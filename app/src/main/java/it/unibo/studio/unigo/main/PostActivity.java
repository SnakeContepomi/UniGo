package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import it.unibo.studio.unigo.R;

public class PostActivity extends AppCompatActivity
{
    private LinearLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        initComponents();
    }

    private void initComponents()
    {
        rootLayout = (LinearLayout) findViewById(R.id.profileLayout);

        // Inizializzazione Toolbar
        Toolbar mActionBar = (Toolbar) findViewById(R.id.PostToolbar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
    }
}
package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import it.unibo.studio.unigo.R;

public class PostActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        initComponents();
    }

    private void initComponents()
    {
        // Inizializzazione Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.PostToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.post_activity_toolbar);
        toolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.post_toolbar_send:
                Toast.makeText(this, "Favorite", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }
}
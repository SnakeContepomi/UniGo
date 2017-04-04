package it.unibo.studio.unigo.main;

import android.animation.Animator;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import it.unibo.studio.unigo.R;

public class PostActivity extends AppCompatActivity
{
    LinearLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Sovrascrittura della transizione di default
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_post);

        rootLayout = (LinearLayout) findViewById(R.id.profileLayout);

        if (savedInstanceState == null)
        {
            //rootLayout.setVisibility(View.INVISIBLE);
            ViewTreeObserver viewTreeObserver = rootLayout.getViewTreeObserver();
            if (viewTreeObserver.isAlive())
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout()
                    {
                        circularRevealActivity();
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                            rootLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                         else
                            rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
        }
    }

    // Animazione per caricare l'activity tramite circular reveal
    private void circularRevealActivity()
    {
        // Coordinate inizio animazione
        int cx = rootLayout.getRight();
        int cy = rootLayout.getBottom();

        // Raggio animazione
        float finalRadius = Math.max(rootLayout.getWidth(), rootLayout.getHeight());

        // Animator view
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, cx, cy, 0, finalRadius);

        // Inizio animazione
        //rootLayout.setVisibility(View.VISIBLE);
        circularReveal.start();
    }
}
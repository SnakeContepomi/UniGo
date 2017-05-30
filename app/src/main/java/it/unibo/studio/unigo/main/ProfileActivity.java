package it.unibo.studio.unigo.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;

import de.hdodenhof.circleimageview.CircleImageView;
import it.unibo.studio.unigo.R;

public class ProfileActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR  = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS     = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION              = 200;

    private boolean mIsTheTitleVisible          = false;
    private boolean mIsTheTitleContainerVisible = true;

    private AppBarLayout appbar;
    private CollapsingToolbarLayout collapsing;
    private ImageView coverImage;
    private FrameLayout framelayoutTitle;
    private LinearLayout linearlayoutTitle;
    private Toolbar toolbar;
    private TextView textviewTitle;
    private CircleImageView avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //overridePendingTransition(entrante, uscente)
        overridePendingTransition(R.anim.target_activity_creation, R.anim.main_activity_dissolve);
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_profile);
        initComponents();

        toolbar.setTitle("");
        appbar.addOnOffsetChangedListener(this);

        //setSupportActionBar(toolbar);
        startAlphaAnimation(textviewTitle, 0, View.INVISIBLE);

        //set avatar and cover
        //avatar.setImageURI(imageUri);
        coverImage.setImageResource(R.drawable.cover);
    }

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.main_activity_creation, R.anim.target_activity_dissolve);
        super.onPause();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    private void initComponents()
    {
        appbar = (AppBarLayout)findViewById( R.id.appbar );
        collapsing = (CollapsingToolbarLayout)findViewById( R.id.collapsing );
        coverImage = (ImageView)findViewById( R.id.imageview_placeholder );
        framelayoutTitle = (FrameLayout)findViewById( R.id.framelayout_title );
        linearlayoutTitle = (LinearLayout)findViewById( R.id.linearlayout_title );
        toolbar = (Toolbar)findViewById( R.id.toolbar );
        textviewTitle = (TextView)findViewById( R.id.textview_title );
        avatar = (CircleImageView) findViewById(R.id.avatar);

        toolbar.inflateMenu(R.menu.menu_profile);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                inviaEmail();
                return false;
            }
        });
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {

            if(!mIsTheTitleVisible) {
                startAlphaAnimation(textviewTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
            }

        } else {

            if (mIsTheTitleVisible) {
                startAlphaAnimation(textviewTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if(mIsTheTitleContainerVisible) {
                startAlphaAnimation(linearlayoutTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(linearlayoutTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    public static void startAlphaAnimation (View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }

    // Metodo che permette all'utente di scegliere con quale client inviare la mail
    // (tutto il testo già scritto verrà passato al client scelto)
    protected void inviaEmail()
    {
        Intent email = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
        // Vengono visualizzati solamente i client email installati
        email.setType("message/rfc822");
        // Viene inserito il destinatario, l'ogetto e il corpo principale della mail dentro l'intent
        email.putExtra(Intent.EXTRA_EMAIL, "concar92@gmail.com");

        // Viene chiesto all'utente quale client email utilizzare
        try
        {
            startActivity(Intent.createChooser(email, "Selezionare il client email"));
        }
        // Se non vengono trovati client email, verrà visualizzato un alertDialog per informare l'utente
        catch (android.content.ActivityNotFoundException ex)
        {
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
            adBuilder.setTitle("Errore");
            adBuilder.setMessage("Nessun client email installato sul dispositivo.\nImpossibile inviare la mail.");
            //adBuilder.setIcon(...);
            adBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.dismiss();
                }
            });
            AlertDialog alert = adBuilder.create();
            alert.show();
        }
    }
}


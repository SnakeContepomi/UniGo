package it.unibo.studio.unigo.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class ProfileActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener
{
    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR  = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS     = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION              = 200;

    private boolean mIsTheTitleVisible          = false;
    private boolean mIsTheTitleContainerVisible = true;

    private LinearLayout linearlayoutTitle, xpBarDone, xpBarLeft;
    private Toolbar toolbar;
    private TextView txtToolbarTitle, txtName, txtLevel,
                     txtBarLvl, txtBarExp,
                     txtEmail, txtCourse, txtCity, txtPhone, txtSubscribe,
                     txtXP, txtnQuestions, txtNAnswers, txtNComments, txtNCredits;
    private CircleImageView avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //overridePendingTransition(entrante, uscente)
        overridePendingTransition(R.anim.target_activity_creation, R.anim.main_activity_dissolve);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initComponents();
        retrieveUserInfo();
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
        AppBarLayout appbar = (AppBarLayout) findViewById(R.id.appbar);
        linearlayoutTitle = (LinearLayout) findViewById( R.id.linearlayout_title );
        txtName = (TextView) findViewById(R.id.txtProfileName);
        txtLevel = (TextView) findViewById(R.id.txtProfileLevel);
        xpBarDone = (LinearLayout) findViewById(R.id.xpBarDone);
        xpBarLeft = (LinearLayout) findViewById(R.id.xpBarLeft);
        txtBarLvl = (TextView) findViewById(R.id.txtBarLvl);
        txtBarExp = (TextView) findViewById(R.id.txtBarExp);
        toolbar = (Toolbar) findViewById( R.id.toolbar );
        txtToolbarTitle = (TextView) findViewById( R.id.txtProfileToolbarTitle);
        avatar = (CircleImageView) findViewById(R.id.avatar);
        txtEmail = (TextView) findViewById(R.id.txtProfileEmail);
        txtCourse = (TextView) findViewById(R.id.txtProfileCourse);
        txtCity = (TextView) findViewById(R.id.txtProfileCity);
        txtPhone = (TextView) findViewById(R.id.txtProfilePhone);
        txtSubscribe = (TextView) findViewById(R.id.txtProfileSubscribe);
        txtXP = (TextView) findViewById(R.id.txtProfileXP);
        txtnQuestions = (TextView) findViewById(R.id.txtProfileNQuestion);
        txtNAnswers = (TextView) findViewById(R.id.txtProfileNAnswer);
        txtNComments = (TextView) findViewById(R.id.txtProfileNComments);
        txtNCredits = (TextView) findViewById(R.id.txtProfileNCredits);

        this.getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorDarkPurple));

        // Se il profilo dell'utente è quello dell'utilizzatore, sarà possibile modificare
        // le proprie informazioni, altrimenti sarà possibile inviare una mail all'utente destinatario
        toolbar.inflateMenu(R.menu.toolbar_profile);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                sendEmail(Util.decodeEmail(getIntent().getStringExtra("user_key")));
                return false;
            }
        });
        if (getIntent().getStringExtra("user_key").equals(Util.getCurrentUser().getEmail()))
            toolbar.getMenu().getItem(0).setVisible(false);

        appbar.addOnOffsetChangedListener(this);
        startAlphaAnimation(txtToolbarTitle, 0, View.INVISIBLE);
    }

    private void handleToolbarTitleVisibility(float percentage)
    {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR)
        {
            if(!mIsTheTitleVisible)
            {
                startAlphaAnimation(txtToolbarTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
                toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
                avatar.setBorderWidth(0);
                this.getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            }
        }
        else
        {
            if (mIsTheTitleVisible)
            {
                startAlphaAnimation(txtToolbarTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
                toolbar.setBackgroundColor(Color.TRANSPARENT);
                avatar.setBorderWidth((int) (2 * getResources().getDisplayMetrics().density + 0.5f));
                this.getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorDarkPurple));
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

    // Vengono recuperate le informazioni personali dell'utente
    private void retrieveUserInfo()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(getIntent().getStringExtra("user_key"))).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                User user = dataSnapshot.getValue(User.class);
                int userLvl, expPreviousLvl, expNextLvl;
                userLvl = Util.getUserLevel(user.exp);

                // Caricamento dell'immagine utente
                Picasso.with(avatar.getContext()).load(user.photoUrl).placeholder(R.drawable.empty_profile_pic).fit().into(avatar);

                // Caricamento nome-cognome
                txtName.setText(user.name + " " + user.lastName);
                txtToolbarTitle.setText(user.name + " " + user.lastName);
                // Caricamento titolo, in base ai punti exp dell'utente
                txtLevel.setText(Util.getUserTitle(userLvl));

                // Caricamento livello attuale e punti necessari al prossimo livello
                txtBarLvl.setText(getResources().getString(R.string.profile_lvl, userLvl));
                expPreviousLvl = Util.getExpForNextLevel(userLvl - 1);
                expNextLvl = Util.getExpForNextLevel(userLvl);

                // Inizializzazione Layout punti esperienza (barra dei progressi + testo)
                if (userLvl == Util.MAX_LEVEL)
                {
                    xpBarDone.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
                    xpBarLeft.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 0f));
                    txtBarExp.setText(getResources().getString(R.string.profile_xp_max, Util.formatExp(user.exp)));
                }
                else
                {
                    float xpDone = (user.exp - expPreviousLvl) / (float) (expNextLvl - expPreviousLvl);
                    xpBarDone.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, xpDone));
                    xpBarLeft.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, (1-xpDone)));
                    txtBarExp.setText(getResources().getString(R.string.profile_xp, Util.formatExp(user.exp - expPreviousLvl), Util.formatExp(expNextLvl - expPreviousLvl)));
                }

                // Caricamento dell'email utente
                txtEmail.setText(Util.decodeEmail(dataSnapshot.getKey()));
                Util.getDatabase().getReference("Course").child(user.courseKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        txtCourse.setText(dataSnapshot.child("name").getValue(String.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
                // Caricamento della città dell'utente
                if (user.city.equals(""))
                    txtCity.setText("-");
                else
                    txtCity.setText(user.city);
                // Caricamento del numero di telefono dell'utente
                if (user.phone.equals(""))
                    txtPhone.setText("-");
                else
                    txtPhone.setText(user.phone);
                txtSubscribe.setText(setDate(user.subscribeDate));


                txtXP.setText(Util.formatExp(user.exp));

                if (user.questions != null)
                    txtnQuestions.setText(String.valueOf(user.questions.size()));
                else
                    txtnQuestions.setText("0");
                if (user.answers != null)
                    txtNAnswers.setText(String.valueOf(user.answers.size()));
                else
                    txtNAnswers.setText("0");
                if (user.comments != null)
                    txtNComments.setText(String.valueOf(user.comments.size()));
                else
                    txtNComments.setText("0");
                txtNCredits.setText(String.valueOf(user.credits));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che permette all'utente di scegliere con quale client inviare la mail
    // (tutto il testo già scritto verrà passato al client scelto)
    private void sendEmail(String mail)
    {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL  , new String[]{mail});
        try
        {
            startActivity(Intent.createChooser(i, "Selezionare il client email"));
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
            adBuilder.setTitle("Errore");
            adBuilder.setMessage("Nessun client email installato sul dispositivo.\nImpossibile inviare la mail.");
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

    private String setDate(String date)
    {
        String[] splittedDate = (date.substring(0, 10)).split("/");

        return splittedDate[2] + " " + Util.getMonthName(splittedDate[1]) + " " + splittedDate[0];
    }
}


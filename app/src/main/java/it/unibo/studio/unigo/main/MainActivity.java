package it.unibo.studio.unigo.main;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import com.github.fabtransitionactivity.SheetLayout;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableBadgeDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.Picasso;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.BackgroundService;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements SheetLayout.OnFabAnimationEndListener
{
    private final int REQUEST_CODE_POST = 1;
    private final String FRAGMENT_HOME = "home";
    private final String FRAGMENT_QUESTION = "question";
    private final String FRAGMENT_FAVORITE = "favorite";
    private final String FRAGMENT_SOCIAL = "social";
    private final String FRAGMENT_SETTINGS = "settings";
    private final String FRAGMENT_INFO = "info";

    private boolean firstTime;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private HomeFragment fragmentHome;
    private FavoriteFragment fragmentFavorite;
    private QuestionFragment fragmentQuestion;
    private SocialFragment fragmentSocial;
    private SettingsFragment fragmentSettings;
    private InfoFragment fragmentInfo;

    private Toolbar toolbar;
    private ProfileDrawerItem profile;
    private AccountHeader header;
    private Drawer navDrawer;
    private FloatingActionButton fab;
    private SheetLayout sheetLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();


        // Background Service
        if (!BackgroundService.isRunning)
        {
            Intent intent = new Intent(this, BackgroundService.class);
            startService(intent);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

        // Avvio dell'utente loggato con attivazione del listener sullo stato della connessione
        if (firstTime)
        {
            Snackbar.make(findViewById(R.id.drawerLayout), getResources().getString(R.string.snackbar_login_message) + Util.getCurrentUser().getEmail(), Snackbar.LENGTH_LONG)
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        // Inizializzazione del listener che monitora lo stato della connessione
                        ReactiveNetwork.observeInternetConnectivity()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean isConnectedToInternet) {
                                    if (!isConnectedToInternet)
                                        Snackbar
                                                .make(findViewById(R.id.drawerLayout), R.string.snackbar_no_internet_connection, Snackbar.LENGTH_LONG)
                                                .show();
                                    else {
                                        profile.withIcon(Util.getCurrentUser().getPhotoUrl());
                                        header.updateProfile(profile);
                                    }
                                }
                            });
                    }
                })
                .show();
            firstTime = false;
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (mAuthListener != null)
            mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onBackPressed()
    {
        if ((navDrawer != null) && (navDrawer.isDrawerOpen()))
            navDrawer.closeDrawer();
        else
        {
            HomeFragment home = (HomeFragment) getFragmentManager().findFragmentByTag(FRAGMENT_HOME);
            if ((home == null) || (!home.isVisible()))
                navDrawer.setSelection(navDrawer.getDrawerItem(1));
            else
                super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_POST)
            sheetLayout.contractFab();
    }

    // Creazione della nuova activity con animazione
    @Override
    public void onFabAnimationEnd()
    {
        Intent intent = new Intent(this, PostActivity.class);
        startActivityForResult(intent, REQUEST_CODE_POST);
    }

    private void initComponents()
    {
        // Inizializzazione dei componenti utilizzati per l'animazione del fab
        sheetLayout = (SheetLayout) findViewById(R.id.bottom_sheet);
        fab = (FloatingActionButton) findViewById(R.id.fabHome);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                ((SheetLayout) findViewById(R.id.bottom_sheet)).expandFab();
            }
        });
        sheetLayout.setFab(fab);
        sheetLayout.setFabAnimationEndListener(this);

        firstTime = true;
        mAuth = FirebaseAuth.getInstance();

        // Componente che permette di caricare nelle view immagini recuperate via url (grazie a Picasso)
        DrawerImageLoader.init(new AbstractDrawerImageLoader()
        {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder)
            {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }
            @Override
            public void cancel(ImageView imageView)
            {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }
        });

        // Inizializzazione NavDrawer
        initNavDrawer();

        // Gestione Logout
        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                // L'utente si è disconnesso
                if (Util.getCurrentUser() == null)
                {

                }
            }
        };
    }

    // Inizializzazione della Toolbar e del NavDrawer
    private void initNavDrawer()
    {
        toolbar = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);

        fragmentHome = new HomeFragment();
        fragmentFavorite = new FavoriteFragment();
        fragmentQuestion = new QuestionFragment();
        fragmentSocial = new SocialFragment();
        fragmentSettings = new SettingsFragment();
        fragmentInfo = new InfoFragment();

        Util.setHomeFragment(fragmentHome);

        // Inizializzazione del profilo utente presente nel navDrawer
        profile = new ProfileDrawerItem()
                .withName(Util.getCurrentUser().getDisplayName())
                .withEmail(Util.getCurrentUser().getEmail())
                .withIcon(R.drawable.empty_profile_pic);
        if (Util.isNetworkAvailable(getApplicationContext()))
            profile.withIcon(Util.getCurrentUser().getPhotoUrl());

        // Inizializzazione dell'header del navDrawer
        header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header_background)
                .withDividerBelowHeader(false)
                .withSelectionListEnabled(false)
                .addProfiles(profile)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current)
                    {
                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                        return false;
                    }
                })
                .build();

        // Inizializzazione delle voci del navDrawer
        PrimaryDrawerItem nav_home = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_tutte).withLevel(2).withIcon(R.drawable.ic_inbox_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_favorite  = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.drawer_preferiti).withLevel(2).withIcon(R.drawable.ic_star_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_question = new PrimaryDrawerItem().withIdentifier(3).withName(R.string.drawer_domande).withLevel(2).withIcon(R.drawable.ic_label_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_social = new PrimaryDrawerItem().withIdentifier(4).withName(R.string.drawer_social).withIcon(R.drawable.ic_group_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_settings  = new PrimaryDrawerItem().withIdentifier(5).withName(R.string.drawer_impostazioni).withIcon(R.drawable.ic_settings_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_info  = new PrimaryDrawerItem().withIdentifier(6).withName(R.string.drawer_guida).withIcon(R.drawable.ic_info_black_24dp).withIconTintingEnabled(true);
        ExpandableBadgeDrawerItem nav_expandable = new ExpandableBadgeDrawerItem()
                .withName(R.string.drawer_principale)
                .withIcon(R.drawable.ic_home_black_24dp)
                .withIconTintingEnabled(true)
                .withSelectable(false)
                .withIsExpanded(true)
                .withSubItems(
                        nav_home,
                        nav_favorite,
                        nav_question
                );

        // Creazione del navDrawer con le varie caratteristiche sopra definite
        navDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(header)
                // --- Badge ---
                // withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700)).withBadge("100").
                .addDrawerItems(
                        nav_expandable,
                        nav_social,
                        new DividerDrawerItem(),
                        nav_settings,
                        nav_info )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem)
                    {
                        switch ((int) drawerItem.getIdentifier())
                        {
                            case 1:
                                loadFragment(fragmentHome, FRAGMENT_HOME);
                                getSupportActionBar().setTitle(R.string.drawer_tutte);
                                navDrawer.closeDrawer();
                                showFab();
                                break;
                            case 2:
                                loadFragment(fragmentFavorite, FRAGMENT_FAVORITE);
                                getSupportActionBar().setTitle(R.string.drawer_preferiti);
                                navDrawer.closeDrawer();
                                showFab();
                                break;
                            case 3:
                                loadFragment(fragmentQuestion, FRAGMENT_QUESTION);
                                getSupportActionBar().setTitle(R.string.drawer_domande);
                                navDrawer.closeDrawer();
                                showFab();
                                break;
                            case 4:
                                loadFragment(fragmentSocial, FRAGMENT_SOCIAL);
                                getSupportActionBar().setTitle(R.string.drawer_social);
                                navDrawer.closeDrawer();
                                hideFab();
                                break;
                            case 5:
                                loadFragment(fragmentSettings, FRAGMENT_SETTINGS);
                                getSupportActionBar().setTitle(R.string.drawer_impostazioni);
                                navDrawer.closeDrawer();
                                hideFab();
                                break;
                            case 6:
                                loadFragment(fragmentInfo, FRAGMENT_INFO);
                                getSupportActionBar().setTitle(R.string.drawer_guida);
                                navDrawer.closeDrawer();
                                hideFab();
                                break;
                        }
                        return true;
                    }
                })
                .build();

        // Viene caricato il Fragment 'Home' all'avvio dell'Activity
        navDrawer.setSelection(navDrawer.getDrawerItem(1));
    }

    // Metodo per caricare un fragment nella Main Activity
    private void loadFragment(Fragment fragment, String tag)
    {
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit();
    }

    private void showFab()
    {
        if (!fab.isShown())
            fab.show();
    }

    private void hideFab()
    {
        if (fab.isShown())
            fab.hide();
    }
}
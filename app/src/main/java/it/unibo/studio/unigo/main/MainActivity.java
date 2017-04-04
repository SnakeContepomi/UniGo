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
import com.google.firebase.database.DatabaseReference;
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
import it.unibo.studio.unigo.utils.Course;
import it.unibo.studio.unigo.utils.School;
import it.unibo.studio.unigo.utils.University;
import it.unibo.studio.unigo.utils.Util;
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
    private final String FRAGMENT_PROFILE = "profile";

    private boolean firstTime;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private DatabaseReference database;

    private ProfileFragment fragmentProfile;
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
        //fillUniversity();
        //fillSchool();
        //fillCourse();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

        // Avvio dell'utente loggato con attivazione del listener sullo stato della connessione
        if (firstTime)
        {
            Snackbar.make(findViewById(R.id.drawerLayout), getResources().getString(R.string.snackbar_login_message) + user.getEmail(), Snackbar.LENGTH_LONG)
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
                                        profile.withIcon(user.getPhotoUrl());
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
        sheetLayout.setFab(fab);
        sheetLayout.setFabAnimationEndListener(this);

        firstTime = true;
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        database = Util.getDatabase().getReference();

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
                FirebaseUser user = firebaseAuth.getCurrentUser();
                // L'utente si è disconnesso
                if (user == null)
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

        fragmentProfile = new ProfileFragment();
        fragmentHome = new HomeFragment();
        fragmentFavorite = new FavoriteFragment();
        fragmentQuestion = new QuestionFragment();
        fragmentSocial = new SocialFragment();
        fragmentSettings = new SettingsFragment();
        fragmentInfo = new InfoFragment();

        // Inizializzazione del profilo utente presente nel navDrawer
        profile = new ProfileDrawerItem()
                .withName(user.getDisplayName())
                .withEmail(user.getEmail())
                .withIcon(R.drawable.empty_profile_pic);
        if (Util.isNetworkAvailable(getApplicationContext()))
            profile.withIcon(user.getPhotoUrl());

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
                        loadFragment(fragmentProfile, FRAGMENT_PROFILE);
                        getSupportActionBar().setTitle(R.string.drawer_profilo);
                        return false;
                    }
                })
                .build();

        // Inizializzazione delle voci del navDrawer
        PrimaryDrawerItem nav_home = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_tutte).withLevel(2).withIcon(R.drawable.ic_inbox_black_24dp).withIconTintingEnabled(true);
        final PrimaryDrawerItem nav_favorite  = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.drawer_preferiti).withLevel(2).withIcon(R.drawable.ic_star_black_24dp).withIconTintingEnabled(true);
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
                                break;
                            case 2:
                                loadFragment(fragmentFavorite, FRAGMENT_FAVORITE);
                                getSupportActionBar().setTitle(R.string.drawer_preferiti);
                                navDrawer.closeDrawer();
                                break;
                            case 3:
                                loadFragment(fragmentQuestion, FRAGMENT_QUESTION);
                                getSupportActionBar().setTitle(R.string.drawer_domande);
                                navDrawer.closeDrawer();
                                break;
                            case 4:
                                loadFragment(fragmentSocial, FRAGMENT_SOCIAL);
                                getSupportActionBar().setTitle(R.string.drawer_social);
                                navDrawer.closeDrawer();
                                break;
                            case 5:
                                loadFragment(fragmentSettings, FRAGMENT_SETTINGS);
                                getSupportActionBar().setTitle(R.string.drawer_impostazioni);
                                navDrawer.closeDrawer();
                                break;
                            case 6:
                                loadFragment(fragmentInfo, FRAGMENT_INFO);
                                getSupportActionBar().setTitle(R.string.drawer_guida);
                                navDrawer.closeDrawer();
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

    // Metodo che ritorna il Fragment attualmente caricato
    private Fragment getCurrentFragment()
    {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment instanceof ProfileFragment)
            return fragmentProfile;
        if (fragment instanceof HomeFragment)
            return fragmentHome;
        if (fragment instanceof SocialFragment)
            return fragmentSocial;
        if (fragment instanceof SettingsFragment)
            return fragmentSettings;
        if (fragment instanceof InfoFragment)
            return fragmentInfo;

        return null;
    }

    private void fillUniversity()
    {
        // Abruzzo
        database.child("University").push().setValue(new University("Abruzzo", "Università degli Studi \"Gabriele D'Annunzio\""));
        database.child("University").push().setValue(new University("Abruzzo", "Università degli Studi di L'Aquila"));
        database.child("University").push().setValue(new University("Abruzzo", "Università degli Studi di Teramo"));

        // Basilicata
        database.child("University").push().setValue(new University("Basilicata", "Università degli Studi della Basilicata"));

        // Calabria
        database.child("University").push().setValue(new University("Calabria", "Università degli Studi \"Magna Graecia\" di Catanzaro"));
        database.child("University").push().setValue(new University("Calabria", "Università degli Studi Mediterranea di Reggio Calabria"));
        database.child("University").push().setValue(new University("Calabria", "Università della Calabria"));

        // Campania
        database.child("University").push().setValue(new University("Campania", "Istituto Universitario \"Suor Orsola Benincasa\""));
        database.child("University").push().setValue(new University("Campania", "Seconda Università degli Studi di Napoli"));
        database.child("University").push().setValue(new University("Campania", "Università degli Studi del Sannio"));
        database.child("University").push().setValue(new University("Campania", "Università degli Studi di Napoli \"Federico II\""));
        database.child("University").push().setValue(new University("Campania", "Università degli Studi di Napoli \"L'Orientale\""));
        database.child("University").push().setValue(new University("Campania", "Università degli Studi di Napoli \"Partenophe\""));
        database.child("University").push().setValue(new University("Campania", "Università degli Studi di Salerno"));

        // Emilia Romagna
        database.child("University").push().setValue(new University("Emilia Romagna", "Università degli Studi di Bologna"));
        database.child("University").push().setValue(new University("Emilia Romagna", "Università degli Studi di Ferrara"));
        database.child("University").push().setValue(new University("Emilia Romagna", "Università degli Studi di Modena e Reggio Emilia"));
        database.child("University").push().setValue(new University("Emilia Romagna", "Università degli Studi di Parma"));

        // Friuli Venezia Giulia
        database.child("University").push().setValue(new University("Friuli Venezia Giulia", "SISSA - Scuola Internazionale Superiore di Studi Avanzati"));
        database.child("University").push().setValue(new University("Friuli Venezia Giulia", "Università degli Studi di Trieste"));
        database.child("University").push().setValue(new University("Friuli Venezia Giulia", "Università degli Studi di Udine"));

        // Lazio
        database.child("University").push().setValue(new University("Lazio", "IUSM - Università degli Studi di Roma \"Foro Italico\""));
        database.child("University").push().setValue(new University("Lazio", "Libera Università degli Studi \"San Pio V\""));
        database.child("University").push().setValue(new University("Lazio", "LUISS - Libera Università Internazionale degli Studi Sociali Guido Carli"));
        database.child("University").push().setValue(new University("Lazio", "LUMSA - Libera Università \"Maria Ss. Assunta\""));
        database.child("University").push().setValue(new University("Lazio", "Università \"Campus Bio-Medico\" di Roma"));
        database.child("University").push().setValue(new University("Lazio", "Università degli Studi della Tuscia"));
        database.child("University").push().setValue(new University("Lazio", "Università degli Studi di Cassino"));
        database.child("University").push().setValue(new University("Lazio", "Università degli Studi di Roma \"La Sapienza\""));
        database.child("University").push().setValue(new University("Lazio", "Università degli Studi di Roma \"Tor Vergata\""));
        database.child("University").push().setValue(new University("Lazio", "Università degli Studi Europea di Roma"));
        database.child("University").push().setValue(new University("Lazio", "Università degli Studi \"Roma Tre\""));

        // Liguria
        database.child("University").push().setValue(new University("Liguria", "Università degli Studi di Genova"));

        // Lombardia
        database.child("University").push().setValue(new University("Lombardia", "IULM - Libera Università di Lingue e Comunicazione"));
        database.child("University").push().setValue(new University("Lombardia", "Politecnico di Milano"));
        database.child("University").push().setValue(new University("Lombardia", "Università Carlo Cattaneo - LIUC"));
        database.child("University").push().setValue(new University("Lombardia", "Università Cattolica del Sacro Cuore"));
        database.child("University").push().setValue(new University("Lombardia", "Università Commerciale Luigi Bocconi"));
        database.child("University").push().setValue(new University("Lombardia", "Università degli Studi dell'Insubria Varese-Como"));
        database.child("University").push().setValue(new University("Lombardia", "Università degli Studi di Bergamo"));
        database.child("University").push().setValue(new University("Lombardia", "Università degli Studi di Brescia"));
        database.child("University").push().setValue(new University("Lombardia", "Università degli Studi di Milano"));
        database.child("University").push().setValue(new University("Lombardia", "Università degli Studi di Milano-Bicocca"));
        database.child("University").push().setValue(new University("Lombardia", "Università degli Studi di Pavia"));
        database.child("University").push().setValue(new University("Lombardia", "Università Vita-Salute San Raffaele"));

        // Marche
        database.child("University").push().setValue(new University("Marche", "Università Politecnica delle Marche"));
        database.child("University").push().setValue(new University("Marche", "Università degli Studi di Camerino"));
        database.child("University").push().setValue(new University("Marche", "Università degli Studi di Macerata"));
        database.child("University").push().setValue(new University("Marche", "Università degli Studi di Urbino Carlo Bo"));

        // Molise
        database.child("University").push().setValue(new University("Molise", "Università degli Studi del Molise"));

        // Piemonte
        database.child("University").push().setValue(new University("Piemonte", "Politecnico di Torino"));
        database.child("University").push().setValue(new University("Piemonte", "Università degli Studi del Piemonte Orientale \"Amedeo Avogadro\""));
        database.child("University").push().setValue(new University("Piemonte", "Università degli Studi di Torino"));
        database.child("University").push().setValue(new University("Piemonte", "Università di Scienze Gastronomiche"));

        // Puglia
        database.child("University").push().setValue(new University("Puglia", "LUM - Libera Università Mediterranea \"Jean Monnet\""));
        database.child("University").push().setValue(new University("Puglia", "Politecnico di Bari"));
        database.child("University").push().setValue(new University("Puglia", "Università degli Studi di Bari"));
        database.child("University").push().setValue(new University("Puglia", "Università degli Studi di Foggia"));
        database.child("University").push().setValue(new University("Puglia", "Università degli Studi del Salento"));

        // Sardegna
        database.child("University").push().setValue(new University("Sardegna", "Università degli Studi di Cagliari"));
        database.child("University").push().setValue(new University("Sardegna", "Università degli Studi di Sassari"));

        // Sicilia
        database.child("University").push().setValue(new University("Sicilia", "Università degli Studi di Catania"));
        database.child("University").push().setValue(new University("Sicilia", "Università degli Studi di Messina"));
        database.child("University").push().setValue(new University("Sicilia", "Università degli Studi di Palermo"));

        // Toscana
        database.child("University").push().setValue(new University("Toscana", "Scuola Normale Superiore - Pisa"));
        database.child("University").push().setValue(new University("Toscana", "Scuola Superiore di Studi Universitari e di Perfezionamento \"Sant'Anna\" - Pisa"));
        database.child("University").push().setValue(new University("Toscana", "Università degli Studi di Firenze"));
        database.child("University").push().setValue(new University("Toscana", "Università degli Studi di Pisa"));
        database.child("University").push().setValue(new University("Toscana", "Università degli Studi di Siena"));
        database.child("University").push().setValue(new University("Toscana", "Università per Stranieri di Siena"));

        // Trentino Alto Adige
        database.child("University").push().setValue(new University("Trentino Alto Adige", "Libera Università di Bolzano"));
        database.child("University").push().setValue(new University("Trentino Alto Adige", "Università degli Studi di Trento"));

        // Umbria
        database.child("University").push().setValue(new University("Umbria", "Università degli Studi di Perugia"));
        database.child("University").push().setValue(new University("Umbria", "Università per Stranieri di Perugia"));

        // Valle D'Aosta
        database.child("University").push().setValue(new University("Valle D'Aosta", "Università della Valle d'Aosta"));

        // Veneto
        database.child("University").push().setValue(new University("Veneto", "Università Iuav di Venezia"));
        database.child("University").push().setValue(new University("Veneto", "Università \"Ca' Foscari\" di Venezia"));
        database.child("University").push().setValue(new University("Veneto", "Università degli Studi di Padova"));
        database.child("University").push().setValue(new University("Veneto", "Università degli Studi di Verona"));
    }

    private void fillSchool()
    {
        String key;

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Agraria e Medicina Veterinaria", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Economia, Management e Statistica", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Farmacia, Biotecnologie e Scienze motorie", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Giurisprudenza", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Ingegneria e Architettura", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Lettere e Beni culturali", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Lingue e Letterature, Traduzione e Interpretazione", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Medicina e Chirurgia", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Psicologia e Scienze della Formazione", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Scienze", "-KfgKbZcCGX6mjaTO6qD"));

        key = database.child("School").push().getKey();
        database.child("School").child(key).setValue(new School("Scuola di Scienze politiche", "-KfgKbZcCGX6mjaTO6qD"));
    }

    private void fillCourse()
    {
        String key;

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria aerospaziale", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria biomedica", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria chimica e biochimica", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria civile", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria dei processi e dei sistemi edilizi", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria dell'automazione", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria dell'energia elettrica", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria e scienze informatiche", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria edile", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria elettronica e telecomunicazioni", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria elettronica per l'energia e l'informazione", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria energetica", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria gestionale", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria informatica", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria meccanica", "-KflQTKu20u6hE6taLsE"));

        key = database.child("Course").push().getKey();
        database.child("Course").child(key).setValue(new Course("Ingegneria per l'ambiente e il territorio", "-KflQTKu20u6hE6taLsE"));
    }
}
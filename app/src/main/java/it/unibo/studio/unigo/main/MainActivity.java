package it.unibo.studio.unigo.main;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import com.github.fabtransitionactivity.SheetLayout;
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.google.firebase.auth.FirebaseAuth;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
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
import it.unibo.studio.unigo.LoginActivity;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.fragments.ChatRoomFragment;
import it.unibo.studio.unigo.main.fragments.FavoriteFragment;
import it.unibo.studio.unigo.main.fragments.HomeFragment;
import it.unibo.studio.unigo.main.fragments.InfoFragment;
import it.unibo.studio.unigo.main.fragments.MyQuestionFragment;
import it.unibo.studio.unigo.main.fragments.SettingsFragment;
import it.unibo.studio.unigo.main.fragments.SocialFragment;
import it.unibo.studio.unigo.main.fragments.SurveyFragment;
import it.unibo.studio.unigo.utils.BackgroundService;
import it.unibo.studio.unigo.utils.Util;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements SheetLayout.OnFabAnimationEndListener
{
    public static final int REQUEST_CODE_NORMAL = 1;
    public static final int REQUEST_CODE_DETAIL = 2;
    private final String FRAGMENT_HOME = "home";
    private final String FRAGMENT_QUESTION = "question";
    private final String FRAGMENT_FAVORITE = "favorite";
    private final String FRAGMENT_SURVEY = "survey";
    private final String FRAGMENT_CHAT = "chat";
    private final String FRAGMENT_SOCIAL = "social";
    private final String FRAGMENT_SETTINGS = "settings";
    private final String FRAGMENT_INFO = "info";

    private boolean firstTime;
    private int subItemSelection;
    private FirebaseAuth mAuth;
    private Intent serviceIntent;

    private HomeFragment fragmentHome;
    private FavoriteFragment fragmentFavorite;
    private MyQuestionFragment fragmentQuestion;
    private SurveyFragment fragmentSurvey;
    private ChatRoomFragment fragmentChat;
    private SocialFragment fragmentSocial;
    private SettingsFragment fragmentSettings;
    private InfoFragment fragmentInfo;

    private Toolbar toolbar;
    private MaterialSearchView searchView;
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
        serviceIntent = new Intent(this, BackgroundService.class);

        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).keepSynced(true);

        // Background Service
        if (!BackgroundService.isRunning)
            startService(serviceIntent);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

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
    public void onBackPressed()
    {
        if ((navDrawer != null) && (navDrawer.isDrawerOpen()))
            navDrawer.closeDrawer();
        else if (searchView.isSearchOpen())
            searchView.closeSearch();
        else
        {
            HomeFragment home = (HomeFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_HOME);
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
        switch (requestCode)
        {
            case REQUEST_CODE_NORMAL:
                sheetLayout.contractFab();
                break;

            // Alla chiusura dell'activity Detail, viene aggiornato il campo "favorite della domanda interessata"
            case REQUEST_CODE_DETAIL:
                if (getCurrentFragment() instanceof HomeFragment)
                    Util.getHomeFragment().refreshFavorite(data.getStringExtra("question_key"));
                break;
        }
    }

    // Creazione della nuova activity con animazione
    @Override
    public void onFabAnimationEnd()
    {
        if (getCurrentFragment() instanceof ChatRoomFragment)
            startActivityForResult(new Intent(this, ContactActivity.class), REQUEST_CODE_NORMAL);
        else if (getCurrentFragment() instanceof  SurveyFragment)
            startActivityForResult(new Intent(this, NewSurveyActivity.class), REQUEST_CODE_NORMAL);
        // HomeFragment, FavoriteFragment e MyQuestionFragment
        else
            startActivityForResult(new Intent(this, NewPostActivity.class), REQUEST_CODE_NORMAL);
    }

    private void initComponents()
    {
        searchView = (MaterialSearchView) findViewById(R.id.search_view_main);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                if (getCurrentFragment() instanceof HomeFragment)
                    ((HomeFragment) getCurrentFragment()).filterResults(newText);
                if (getCurrentFragment() instanceof FavoriteFragment)
                    ((FavoriteFragment) getCurrentFragment()).filterResults(newText);
                if (getCurrentFragment() instanceof MyQuestionFragment)
                    ((MyQuestionFragment) getCurrentFragment()).filterResults(newText);
                if (getCurrentFragment() instanceof  SocialFragment)
                    ((SocialFragment) getCurrentFragment()).filterResults(newText);

                return false;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() { }

            // Alla chiusura della SearchView, viene resettata la lista delle domande
            @Override
            public void onSearchViewClosed()
            {
                searchView.setQuery("", false);
            }
        });

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
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).fit().into(imageView);
            }
            @Override
            public void cancel(ImageView imageView)
            {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }
        });

        // Inizializzazione NavDrawer
        initNavDrawer();

        if (getIntent().getBooleanExtra("open_chatroom_fragment", false))
        {
            // Viene caricato il Fragment 'ChatRoom' quando viene selezionata una notifica relativa ad una ChatRoom
            navDrawer.setSelection(navDrawer.getDrawerItem(5));
            subItemSelection = 4;
        }
        else if (getIntent().getBooleanExtra("open_survey_fragment", false))
        {
            // Viene caricato il Fragment 'Survey' quando viene aperta una notifica relativa ai sondaggi
            navDrawer.setSelection(navDrawer.getDrawerItem(4));
            subItemSelection = 3;
        }
    }

    // Inizializzazione della Toolbar e del NavDrawer
    private void initNavDrawer()
    {
        toolbar = (Toolbar) findViewById(R.id.toolbarMain);
        toolbar.inflateMenu(R.menu.menu_item_search);
        toolbar.inflateMenu(R.menu.menu_item_option);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_exit  )
                {
                    mAuth.signOut();
                    stopService(serviceIntent);
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
                return false;
            }
        });
        searchView.setMenuItem(toolbar.getMenu().getItem(0));

        fragmentHome = new HomeFragment();
        fragmentFavorite = new FavoriteFragment();
        fragmentQuestion = new MyQuestionFragment();
        fragmentSurvey = new SurveyFragment();
        fragmentChat = new ChatRoomFragment();
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
                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class).putExtra("user_key", Util.getCurrentUser().getEmail()));
                        return false;
                    }
                })
                .build();

        // Inizializzazione delle voci del navDrawer
        PrimaryDrawerItem nav_home = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.drawer_tutte).withLevel(2).withIcon(R.drawable.ic_inbox_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_favorite  = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.drawer_preferiti).withLevel(2).withIcon(R.drawable.ic_star_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_question = new PrimaryDrawerItem().withIdentifier(3).withName(R.string.drawer_domande).withLevel(2).withIcon(R.drawable.ic_label_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_survey = new PrimaryDrawerItem().withIdentifier(4).withName(R.string.drawer_sondaggi).withIcon(R.drawable.ic_chart_pie).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_chat = new PrimaryDrawerItem().withIdentifier(5).withName(R.string.drawer_chat).withIcon(R.drawable.ic_chat_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_social = new PrimaryDrawerItem().withIdentifier(6).withName(R.string.drawer_social).withIcon(R.drawable.ic_group_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_settings  = new PrimaryDrawerItem().withIdentifier(7).withName(R.string.drawer_impostazioni).withIcon(R.drawable.ic_settings_black_24dp).withIconTintingEnabled(true);
        PrimaryDrawerItem nav_info  = new PrimaryDrawerItem().withIdentifier(8).withName(R.string.drawer_guida).withIcon(R.drawable.ic_info_black_24dp).withIconTintingEnabled(true);
        final ExpandableBadgeDrawerItem nav_expandable = new ExpandableBadgeDrawerItem()
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
        nav_expandable.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem)
            {
                if (subItemSelection <= 2)
                    nav_expandable.getSubItems().get(subItemSelection).withSetSelected(true);
                return false;
            }
        });

        // Creazione del navDrawer con le varie caratteristiche sopra definite
        navDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(header)
                // --- Badge ---
                // withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700)).withBadge("100").
                .addDrawerItems(
                        nav_expandable,
                        nav_survey,
                        nav_chat,
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
                                toolbar.setTitle(R.string.drawer_tutte);
                                toolbar.getMenu().getItem(0).setVisible(true);
                                subItemSelection = 0;
                                navDrawer.closeDrawer();
                                fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_black_24dp));
                                showFab();
                                break;
                            case 2:
                                loadFragment(fragmentFavorite, FRAGMENT_FAVORITE);
                                toolbar.setTitle(R.string.drawer_preferiti);
                                toolbar.getMenu().getItem(0).setVisible(true);
                                subItemSelection = 1;
                                navDrawer.closeDrawer();
                                fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_black_24dp));
                                showFab();
                                break;
                            case 3:
                                loadFragment(fragmentQuestion, FRAGMENT_QUESTION);
                                toolbar.setTitle(R.string.drawer_domande);
                                toolbar.getMenu().getItem(0).setVisible(true);
                                subItemSelection = 2;
                                navDrawer.closeDrawer();
                                fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_black_24dp));
                                showFab();
                                break;
                            case 4:
                                loadFragment(fragmentSurvey, FRAGMENT_SURVEY);
                                toolbar.setTitle(R.string.drawer_sondaggi);
                                toolbar.getMenu().getItem(0).setVisible(false);
                                subItemSelection = 3;
                                navDrawer.closeDrawer();
                                fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_black_24dp));
                                showFab();
                                break;
                            case 5:
                                loadFragment(fragmentChat, FRAGMENT_CHAT);
                                toolbar.setTitle(R.string.drawer_chat);
                                toolbar.getMenu().getItem(0).setVisible(false);
                                subItemSelection = 4;
                                navDrawer.closeDrawer();
                                fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_chat_black_24dp));
                                showFab();
                                break;
                            case 6:
                                loadFragment(fragmentSocial, FRAGMENT_SOCIAL);
                                toolbar.setTitle(R.string.drawer_social);
                                toolbar.getMenu().getItem(0).setVisible(true);
                                subItemSelection = 5;
                                navDrawer.closeDrawer();
                                hideFab();
                                break;
                            case 7:
                                loadFragment(fragmentSettings, FRAGMENT_SETTINGS);
                                toolbar.setTitle(R.string.drawer_impostazioni);
                                toolbar.getMenu().getItem(0).setVisible(false);
                                subItemSelection = 6;
                                navDrawer.closeDrawer();
                                hideFab();
                                break;
                            case 8:
                                loadFragment(fragmentInfo, FRAGMENT_INFO);
                                toolbar.setTitle(R.string.drawer_guida);
                                toolbar.getMenu().getItem(0).setVisible(false);
                                subItemSelection = 7;
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
        subItemSelection = 0;
    }

    // Metodo per caricare un fragment nella Main Activity
    private void loadFragment(Fragment fragment, String tag)
    {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit();
    }

    // Metodo che ritorna il Fragment attualmente caricato
    @Nullable
    public Fragment getCurrentFragment()
    {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment instanceof HomeFragment)
            return fragmentHome;
        if (fragment instanceof FavoriteFragment)
            return fragmentFavorite;
        if (fragment instanceof MyQuestionFragment)
            return fragmentQuestion;
        if (fragment instanceof SurveyFragment)
            return fragmentSurvey;
        if (fragment instanceof ChatRoomFragment)
            return fragmentChat;
        if (fragment instanceof SocialFragment)
            return fragmentSocial;
        if (fragment instanceof SettingsFragment)
            return fragmentSettings;
        if (fragment instanceof InfoFragment)
            return fragmentInfo;

        return null;
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

    public boolean isSearchViewShown()
    {
        return searchView.isSearchOpen();
    }
}
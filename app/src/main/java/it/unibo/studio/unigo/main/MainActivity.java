package it.unibo.studio.unigo.main;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Course;
import it.unibo.studio.unigo.utils.School;
import it.unibo.studio.unigo.utils.University;

import static android.R.attr.fragment;

public class MainActivity extends AppCompatActivity
{
    private FirebaseUser user;
    private DatabaseReference database;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
        initNavigationDrawer();
        //fillUniversity();
        //fillSchool();
        //fillCourse();
    }

    private void initComponents()
    {
        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance().getReference();
        toolbar = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);
        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
    }

    // Inizializzazione menu laterale
    private void initNavigationDrawer()
    {
        // Elementi del menu
        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_drawer);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int id = menuItem.getItemId();

                switch (id)
                {
                    case R.id.navItemSocial:
                        finish();
                    case R.id.navItemPrincipale:
                        Toast.makeText(getApplicationContext(),"Home",Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.navItemDomande:
                        Toast.makeText(getApplicationContext(),"Settings",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.navItemPreferiti:
                        Toast.makeText(getApplicationContext(),"Trash",Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.navItemImpostazioni:
                        finish();
                    case R.id.navItemInfo:
                        finish();
                }
                return true;
            }
        });

        // Header del menu con dati relativi all'utente attualmente connesso
        View header = navigationView.getHeaderView(0);
        ImageView imgNav = (ImageView)header.findViewById(R.id.imgNav);
        TextView txtNavUser = (TextView)header.findViewById(R.id.txtNavUser);
        TextView txtNavMail = (TextView)header.findViewById(R.id.txtNavMail);

        // Cliccando l'icona del profilo verrà aperta l'Activity relativa alla modifica dei dati personali
        imgNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //Toast.makeText(getApplicationContext(), "Goto -> MyProfile", Toast.LENGTH_SHORT).show();
                ProfileFragment fragment = new ProfileFragment();
                fragmentTransaction.add(R.id.fragment_container, fragment);
                fragmentTransaction.commit();

                drawerLayout.closeDrawers();
            }
        });

        //ToDo: recuperare foto dal server
        if (user != null)
        {
            txtNavUser.setText(user.getDisplayName());
            txtNavMail.setText(user.getEmail());
            //Toast.makeText(getApplicationContext(), user.getPhotoUrl().toString(), Toast.LENGTH_LONG).show();
            Picasso.with(this).load(user.getPhotoUrl().toString()).into(imgNav);
            //imgNav.setImageBitmap(BitmapFactory.decodeFile(user.getPhotoUrl().toString()));
        }


        drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.drawer_open,R.string.drawer_close)
        {
            @Override
            public void onDrawerClosed(View v){
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
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
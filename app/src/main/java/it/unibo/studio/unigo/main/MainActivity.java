package it.unibo.studio.unigo.main;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.School;
import it.unibo.studio.unigo.utils.University;

public class MainActivity extends AppCompatActivity
{
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        database = FirebaseDatabase.getInstance().getReference();
        //fillUniversity();
        //fillSchool();
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
}
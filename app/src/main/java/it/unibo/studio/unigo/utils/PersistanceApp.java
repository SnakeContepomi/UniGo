package it.unibo.studio.unigo.utils;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

// Classe che permette di utilizzare il database Firebase in modalità offline all'avvio dell'app
public class PersistanceApp extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}

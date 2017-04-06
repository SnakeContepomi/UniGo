package it.unibo.studio.unigo.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

import static android.os.Build.VERSION_CODES.M;

public class Util
{
    public static String CURRENT_USER_KEY, CURRENT_COURSE_KEY;
    public static final int EXP_START = 0;
    public static final int EXP_ANSWER = 10;
    public static final int EXP_LIKE = 2;
    public static final int CREDITS_START = 50;
    public static final int CREDITS_ANSWER = 5;
    public static final int CREDITS_LIKE = 1;
    // Crediti richiesti per effettuare una domanda
    public static final int CREDITS_QUESTION = 10;


    private static FirebaseDatabase database;

    // Riferimento al database di Firebase
    public static FirebaseDatabase getDatabase()
    {
        if (database == null)
            database = FirebaseDatabase.getInstance();
        return database;
    }

    // Ritorna true il dispositivo è connesso ad internet, false altrimenti
    public static boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static String getDate()
    {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.MONTH) + "/" + c.get(Calendar.YEAR) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
    }
}

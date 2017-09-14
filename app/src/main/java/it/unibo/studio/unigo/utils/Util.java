package it.unibo.studio.unigo.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.fragments.HomeFragment;

public class Util
{
    public static String CURRENT_COURSE_KEY;
    public static final int EXP_START = 0;
    public static final int EXP_ANSWER = 10;
    public static final int EXP_LIKE = 2;
    public static final int CREDITS_START = 50;
    public static final int CREDITS_ANSWER = 5;
    public static final int CREDITS_LIKE = 1;
    // Crediti richiesti per effettuare una domanda
    public static final int CREDITS_QUESTION = 10;
    // Moltiplicatore di exp utilizzato nella formula per calcolare l'exp necessaria per il livelo successivo
    public static final int EXP_MULTIPLIER = 3;
    public static final int MAX_LEVEL = 30;
    // Costante che indica la presenza di nuovi messaggi all'interno di una conversazione (ChatRoom)
    public static final int NEW_MSG = 1;

    private static FirebaseDatabase database;
    private static FirebaseUser user;
    private static HomeFragment homeFragment;
    private static DecimalFormat mFormat= new DecimalFormat("00");

    // Riferimento al database di Firebase
    public static FirebaseDatabase getDatabase()
    {
        if (database == null)
            database = FirebaseDatabase.getInstance();
        return database;
    }

    // Metodo per recupererare le informazioni dell'utente loggato
    public static FirebaseUser getCurrentUser()
    {
        if (user == null)
            user = FirebaseAuth.getInstance().getCurrentUser();
        return user;
    }

    public static HomeFragment getHomeFragment()
    {
        return homeFragment;
    }

    public static void setHomeFragment(HomeFragment fragment)
    {
        homeFragment = fragment;
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

        return c.get(Calendar.YEAR) +
                "/" + mFormat.format(Double.valueOf(c.get(Calendar.MONTH))) +
                "/" + mFormat.format(Double.valueOf(c.get(Calendar.DAY_OF_MONTH))) +
                " " + mFormat.format(Double.valueOf(c.get(Calendar.HOUR_OF_DAY))) +
                ":" + mFormat.format(Double.valueOf(c.get(Calendar.MINUTE))) +
                ":" + mFormat.format(Double.valueOf(c.get(Calendar.SECOND)));
    }

    // Metodo per restituire la data in un formato che dipende dalla distanza temporale
    public static String formatDate(String date)
    {
        Calendar c = Calendar.getInstance();

        // Se la data passata è quella odierna, viene restituito solamente l'orario
        if( date.substring(0, 4).equals(String.valueOf(c.get(Calendar.YEAR))) &&
            date.substring(5, 7).equals(mFormat.format(Double.valueOf(c.get(Calendar.MONTH)))) &&
            date.substring(8, 10).equals(mFormat.format(Double.valueOf(c.get(Calendar.DAY_OF_MONTH)))) )
            return date.substring(11, 16);
        // Se la data passata risulta nello stesso anno, viene restituito giorno + mese
        else if (date.substring(0, 4).equals(String.valueOf(c.get(Calendar.YEAR))))
            return date.substring(8,10) + " " + getMonthName(date.substring(5,7));
        // Altrimenti viene restituita la data completa
        else
            return date.substring(8, 10) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);
    }

    public static String getMonthName(String month)
    {
        switch (month)
        {
            case "00":
                return "Gen";
            case "01":
                return "Feb";
            case "02":
                return "Mar";
            case "03":
                return "Apr";
            case "04":
                return "Mag";
            case "05":
                return "Giu";
            case "06":
                return "Lug";
            case "07":
                return "Ago";
            case "08":
                return "Set";
            case "09":
                return "Ott";
            case "10":
                return "Nov";
            case "11":
                return "Dic";
            default:
                return "";
        }
    }

    // Metodo per memorizzare l'indirizzo email in Firebase rimpiazzando i punti con un carattere consentito
    public static String encodeEmail(String email)
    {
        return email.replace(".", "%2E");
    }

    // Metodo che restituisce l'indirizzo email rimpiazzando i caratteri speciali con "."
    public static String decodeEmail(String email)
    {
        return email.replace("%2E", ".");
    }

    // Metodo per ottenere il colore di sfondo per la lettera material, scelto in base alla prima lettera della stringa passata
    public static int getLetterBackgroundColor(Context context, String s)
    {
        int[] colors = context.getResources().getIntArray(R.array.colors);

        s.toUpperCase();
        switch (s.charAt(0))
        {
            case 'A':
                return colors[0];
            case 'B':
                return colors[2];
            case 'C':
                return colors[4];
            case 'D':
                return colors[6];
            case 'E':
                return colors[8];
            case 'F':
                return colors[10];
            case 'G':
                return colors[12];
            case 'H':
                return colors[1];
            case 'I':
                return colors[3];
            case 'J':
                return colors[5];
            case 'K':
                return colors[7];
            case 'L':
                return colors[9];
            case 'M':
                return colors[11];
            case 'N':
                return colors[0];
            case 'O':
                return colors[2];
            case 'P':
                return colors[4];
            case 'Q':
                return colors[6];
            case 'R':
                return colors[8];
            case 'S':
                return colors[10];
            case 'T':
                return colors[12];
            case 'U':
                return colors[1];
            case 'V':
                return colors[3];
            case 'W':
                return colors[5];
            case 'X':
                return colors[7];
            case 'Y':
                return colors[9];
            case 'Z':
                return colors[11];
        }
        return 0;
    }

    // Metodo utilizzato per recuperare il titolo dell'utente, relativo al suo livello
    public static String getUserTitle(int level)
    {
        switch (level)
        {
            case 1:
                return "Desktop Technician";
            case 2:
                return "Web Developer";
            case 3:
                return "Junior Programmer";
            case 4:
                return "Web Designer";
            case 5:
                return "Web Master";
            case 6:
                return "Advanced Programmer";
            case 7:
                return "Database Administrator";
            case 8:
                return "Network Administrator";
            case 9:
                return "System Administrator";
            case 10:
                return "Security Specialist";
            case 11:
                return "Hardware Engineer";
            case 12:
                return "Software Testing Engineer";
            case 13:
                return "Telecommunications Engeneer";
            case 14:
                return "Network Engineer";
            case 15:
                return "Application Engineer";
            case 16:
                return "Analyst";
            case 17:
                return "Hardware and Information System Manager";
            case 18:
                return "Information Technology Manager";
            case 19:
                return "Network Architect";
            case 20:
                return "Senior Database Administrator";
            case 21:
                return "Senior Engineer";
            case 22:
                return "Senior Manager - Programming";
            case 23:
                return "Telecommunications Manager";
            case 24:
                return "Project Manager";
            case 25:
                return "Project Leader";
            case 26:
                return "Chief Technical Officer";
            case 27:
                return "Chief Information Officer";
            case 28:
                return "Information Technology Director";
            case 29:
                return "MIS Director";
            case 30:
                return "Senior Manager - Project Planner";
            default:
                return "Senior Manager - Project Planner";
        }
    }

    // Metodo utilizzato per ottenere il livelo dell'utente, dati i suoi punti exp
    public static int getUserLevel(int exp)
    {
        int level;
        int cumulativeExp = 0;

        for (level = 1; level <= MAX_LEVEL; level++)
        {
            cumulativeExp += (level -1) * EXP_MULTIPLIER * EXP_ANSWER;
            if (exp < cumulativeExp)
                return level - 1;
        }
        return MAX_LEVEL;
    }

    // Metodo che restituisce l'exp necessaria per salire al livelo successivo
    public static int getExpForNextLevel(int startLvl)
    {
        int cumulativeExp = 0;

        for (int i = 1; i <= startLvl + 1; i++)
            cumulativeExp += (i -1) * EXP_MULTIPLIER * EXP_ANSWER;
        return cumulativeExp;
    }

    // Metodo utilizzato per convertire un intero (exp) in una stringa contenente una virgola dopo ogni multiplo di 1000
    public static String formatExp(int exp)
    {
        String strExp = String.valueOf(exp);

        // Se i punti exp son meno di 1000, l'exp viene restituita così com'è, sotto forma di stringa
        if (exp < 1000)
            return strExp;
        // Altrimenti viene creata una stringa in cui, dopo ogni 3 cifre, viene aggiunta una virgola
        else
        {
            ArrayList<String> newExp = new ArrayList<String>();
            int counter = 0;

            // Aggiunta di una virgola ogni 3 cifre
            for (int i = strExp.length() - 1; i >= 0; i--)
            {
                counter++;
                newExp.add(String.valueOf(strExp.charAt(i)));
                if ((counter % 3 == 0) && i != 0)
                {
                    newExp.add(",");
                    counter = 0;
                }
            }
            // La nuova stringa viene invertita
            Collections.reverse(newExp);

            // Viene convertito l'ArrayList sotto forma di stringa
            StringBuilder strBuilder = new StringBuilder();
            for (String s : newExp)
                strBuilder.append(s);

            return strBuilder.toString();
        }
    }

    // Metodo per attivare l'animazione "bounce" della view passata
    public static void startBounceAnimation(Activity activity, View view)
    {
        final Animation myAnim = AnimationUtils.loadAnimation(activity, R.anim.bounce);
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);
        myAnim.setInterpolator(interpolator);
        view.startAnimation(myAnim);
    }
}
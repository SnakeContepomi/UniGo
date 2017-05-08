package it.unibo.studio.unigo.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.fragments.HomeFragment;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.utils.firebase.Question;

public class Util
{
    static final String MY_PREFERENCES = "my_pref";
    static final String LAST_QUESTION_READ = "last_key";
    public static String CURRENT_COURSE_KEY;
    public static final int EXP_START = 0;
    public static final int EXP_ANSWER = 10;
    public static final int EXP_LIKE = 2;
    public static final int CREDITS_START = 50;
    public static final int CREDITS_ANSWER = 5;
    public static final int CREDITS_LIKE = 1;
    // Crediti richiesti per effettuare una domanda
    public static final int CREDITS_QUESTION = 10;

    private static FirebaseDatabase database;
    private static FirebaseUser user;
    private static HomeFragment homeFragment;
    private static boolean isHomeFragmentVisible;
    private static List<QuestionAdapterItem> questionList;
    private static List<QuestionAdapterItem> questiosnToUpdate;
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

    // Metodo per recupererare l'elenco delle domande presenti nel corso corrente
    public static List<QuestionAdapterItem> getQuestionList()
    {
        if (questionList == null)
            questionList = new ArrayList<>();
        return questionList;
    }

    // Metodo per recupererare l'elenco delle domande da aggiornare al riavvio dell'app
    public static List<QuestionAdapterItem> getQuestionsToUpdate()
    {
        if (questiosnToUpdate == null)
            questiosnToUpdate = new ArrayList<>();
        return questiosnToUpdate;
    }

    // Metodo per mettere in coda gli aggiornamenti grafici da effettuare, una volta riesumata l'app
    public static void addToUpdate(String questionKey, Question question)
    {
        getQuestionsToUpdate().add(new QuestionAdapterItem(question, questionKey));
    }

    // Metodo che controlla se la domanda passata è presente nella lista
    public static boolean questionExists(String key)
    {
        for(QuestionAdapterItem item : questionList)
            if (item.getQuestionKey().equals(key))
                return true;
        return false;
    }

    // Data una chiave di una domanda, viene restituita la posizione della stessa all'interno della lista
    public static int getQuestionPosition(String questionKey)
    {
        for (int i = 0; i < questionList.size(); i++)
            if (questionKey.equals(questionList.get(i).getQuestionKey()))
                return i;
        return -1;
    }

    public static boolean isHomeFragmentVisible()
    {
        return (isHomeFragmentVisible) ? true : false;
    }

    public static void setHomeFragmentVisibility(boolean visibiliy)
    {
        isHomeFragmentVisible = visibiliy;
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

    private static String getMonthName(String month)
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

    // Metodo per ottenere il colore di sfondo per la lettera material, scelto in base alla prima lettera della stringa passata
    public static int getLetterBackgroundColor(Context context, String s)
    {
        int[] colors = context.getResources().getIntArray(R.array.colors);

        s.toLowerCase();
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
}
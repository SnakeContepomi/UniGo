package it.unibo.studio.unigo.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.PostActivity;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Comment;
import it.unibo.studio.unigo.utils.firebase.Question;

// Servizio in background che viene fatto partire al boot del telefono o all'avvio dell'app, che recupera
// i cambiamenti da Firebase
public class BackgroundService extends Service
{
    public static boolean isRunning = false;
    private static List<String> questionList = new ArrayList<>();
    private static int questionCount = 0;
    /*
        Tipi di Notifiche in base al valore della variabile notifyID:
        1 - QUESTION: nuova domanda inserita
        2 - ANSWER: nuova risposta alla domanda dell'utente
        3 - COMMENT_QUESTION: nuovo commento generico ad una domanda dell'utente
        4 - COMMENT_ANSWER: nuovo commento riguardante una risposta effettuata dall'utente
        5 - RATING: aumento di importanza di una domanda dell'utente
        6 - LIKE: like ricevuto ad una risposta del'utente
     */
    private enum NotificationType {QUESTION, ANSWER, COMMENT_QUESTION, COMMENT_ANSWER, RATING, LIKE}

    // Avvio del servizio in background
    @Override
    public void onCreate()
    {
        super.onCreate();
        isRunning = true;
        initBackgroundService();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        isRunning = false;
    }

    // Memorizzazione utente corrente per poter effettuare operazioni anche in modalità offline
    private void initBackgroundService()
    {
        // Se la chiave del corso non è ancora stata recuperata:
        // - Viene eseguita una query per recuperarla, conoscendo l'email dell'utente
        // - Viene eseguita una query per recuperare tutte le chiavi delle domande che fanno parte di quel corso,
        //   ordinate cronologicamente (sulla chiave push)
        // - Per ogni chiave di domanda recuperata, viene eseguita una query sulla tabella "Question"
        //   per ottenerne i dettagli e aggiungerla alla lista presente in Util.
        //   Letta l'ultima domanda, viene fatto partire il listener che si occupa di notificare l'inserimento delle nuove domande
        // - Il listener sul campo questions della tabella "Course" permette di avvisare l'utente riguardo l'inserimento di nuove domande
        //   tramite delle notifiche
        // - Ad ogni domanda del corso, viene infine agganciato un listener che ne permette di gestire eventuali modifiche
        if (Util.CURRENT_COURSE_KEY == null)
        {
            // Query per recuperare la chiave del corso
            Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("courseKey")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {
                            Util.CURRENT_COURSE_KEY = dataSnapshot.getValue(String.class);

                            Util.getQuestionList().clear();
                            // Query per recuperare tutte le chiavi delle domande che fanno parte di quel corso, ordinate cronologicamente
                            Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("questions").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                                    DataSnapshot child;
                                    // Per ogni chiave recuperata, viene eseguita una query sulla tabella "Question" al fine di ottenerne
                                    // i dettagli. Ogni domanda viene quindi inserita nella lista presente in Utils ed infine
                                    // viene inizializzato il listener per sull'inserimento delle nuove domande
                                    while (iterator.hasNext()) {
                                        child = iterator.next();
                                        if (iterator.hasNext())
                                            getQuestionAndAddToList(child.getKey(), false);
                                            // Dopo l'ultima domanda letta viene avviato il listener sulle nuove domande
                                        else
                                            getQuestionAndAddToList(child.getKey(), true);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) { }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });
        }
        // Se la chiave del corso è già presente, viene semplicemente fatto partire il listener sulle nuove domande, escludendo
        // tutte quelle con chiave precedente a quella presente nelle shared preferences
        else
            startQuestionListener();

        // Listener sui like ricevuti sulle risposte dell'utente
        // + Listener sui commenti effettuati relativi alle risposte dell'utente
        addLikeCommentListener();
    }

    // Metodo che, data una chiave di una domanda, ne recupera i relativi dettaglia dalla tabella "Question" e la aggiunge alla lista
    // presente in Util. Inoltre se il parametro startListenerOnNewData è "true", viene avviato il listener sulle nuove domande
    private void getQuestionAndAddToList(String question_key, final boolean startListenerOnNewData)
    {
        Util.getDatabase().getReference("Question").child(question_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                addQuestionIntoList(dataSnapshot.getValue(Question.class), dataSnapshot.getKey(), startListenerOnNewData);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che inizializza un listener sul campo questions della tabella "Course", in modo da poter gestire con delle notifiche
    // l'aggiunta di nuove domande
    private void startQuestionListener()
    {
        // Il listener viene agganciato al campo questions, ma solamente agli elementi con chiave "maggiore" a quella presente nelle
        // shared preferences, in modo da gestire solo gli eventi riguardanti le nuove domande inserite, ed evitando quindi
        // quelli relativi alle domande già presenti
        Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("questions").orderByKey().startAt(getLastQuestionRead()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                Util.getDatabase().getReference("Question").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        // Se la domanda inserita non è presente nella lista in Utils, allora essa viene aggiunta e viene avvisato
                        // l'utente con un notifica
                        if (!Util.questionExists(dataSnapshot.getKey()))
                        {
                            addQuestionIntoList(dataSnapshot.getValue(Question.class), dataSnapshot.getKey(), false);
                            // La notifica viene attivata solo se la nuova domanda è stata scritta da un utente diverso
                            // da quelo loggato
                            if (!dataSnapshot.getValue(Question.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                            {
                                Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Question.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        if (!questionList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                            questionList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                        questionCount++;
                                        new getBitmapFromUrl(NotificationType.QUESTION).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) { }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per aggiungere alla lista presente in Util la domanda passata, ed aggiungerla alla RecyclerView
    // se il fragment Home risulta visibile
    private void addQuestionIntoList(final Question question, final String questionKey, final boolean execStartQuestionListener)
    {
        // Aggiunta della domanda alla lista in Util
        Util.getQuestionList().add(0, new QuestionAdapterItem(question, questionKey));

        // Aggiornamento della recyclerview di HomeFragment
        if (Util.getHomeFragment() != null)
            Util.getHomeFragment().updateElement(0);
        // Viene memorizzata la chiave della domanda nella shared preferences per evitare che, al prossimo avvio dell'app,
        // quest'ultima non triggheri le notifiche
        updateLastQuestionRead(questionKey);
        // Viene agganciata alla domanda un listener su eventuali modifiche
        addListenerForNotification(question, questionKey);
        if (execStartQuestionListener)
        {
            startQuestionListener();
            if (Util.isHomeFragmentVisible())
                Util.getHomeFragment().loadQuestionFromList();
        }
    }

    // Metodo per agganciare ad una domanda il listener sui suoi cambiamenti
    private void addListenerForNotification(Question question, String questionKey)
    {
        // Avvio Listener per le notifiche relative alle domande effettuate dall'utente loggato
        // - Listener sulle nuove risposte
        // - Listener sul rating delle domande
        // - Listener sui commenti riguardanti qualsiasi risposta relativa alla domanda dell'utente (escluse le risposte personali)
        if (question.user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
        {
            addNewAnswerListener(question, questionKey);
            addRatingListener(question, questionKey);
            addGenericCommentListener(question, questionKey);
        }

        // Listener sui cambiamenti generici, per aggiornare in real time la lista dele domande
        Util.getDatabase().getReference("Question").child(questionKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Se HomeFragment è visibile, l'aggiornamento viene effettuato in tempo reale
                if (Util.isHomeFragmentVisible())
                    Util.getHomeFragment().refreshQuestion(dataSnapshot.getKey(), dataSnapshot.getValue(Question.class));
                    // Altrimenti gli aggiornamenti vengono messi in coda finche non viene riesumato il fragment
                else
                    Util.addToUpdate(dataSnapshot.getKey(), dataSnapshot.getValue(Question.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per memorizzare nelle shared preferences la chiave dell'ultima domanda caricata
    private void updateLastQuestionRead(String key)
    {
        SharedPreferences prefs = getSharedPreferences(Util.MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Util.LAST_QUESTION_READ, key);
        editor.apply();
    }

    // Metodo per recuperare la chiave dell'ultima domanda memorizzata
    private String getLastQuestionRead()
    {
        SharedPreferences prefs = getSharedPreferences(Util.MY_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(Util.LAST_QUESTION_READ, "");
    }

    // Listener per notificare le risposte alle domande dell'utente loggato
    private void addNewAnswerListener(Question question, final String questionKey)
    {
        // Se la domanda contiene delle risposte, vengono notificati tutti i nuovi inserimenti ad esclusione
        // di queli già presenti
        if (question.answers != null)
        {
            // Recupero della risposta più recente: questa e quelle più vecchie non verranno notificate
            String lastAnswerRead = null;
            for (String key : question.answers.keySet())
            {
                if (lastAnswerRead == null)
                    lastAnswerRead = key;
                else if (key.compareTo(lastAnswerRead) > 0)
                    lastAnswerRead = key;
            }

            final String toAvoid = lastAnswerRead;

            Util.getDatabase().getReference("Question").child(questionKey).child("answers").orderByKey().startAt(lastAnswerRead).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)
                {
                    // Vengono evitate le risposte già presenti e quella scritta dall'utente stesso
                    if (!dataSnapshot.getKey().equals(toAvoid) && !dataSnapshot.getValue(Answer.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                        sendNotification(NotificationType.ANSWER, null);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) { }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
        // Altrimenti se la domanda non ha nessuna risposta, vengono notificate direttamente tutti i nuovi inserimenti
        else
            Util.getDatabase().getReference("Question").child(questionKey).child("answers").addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)
                {
                    // Viene evitata la risposta scritta dall'utente stesso
                    if (!dataSnapshot.getValue(Answer.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                        sendNotification(NotificationType.ANSWER, null);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) { }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
    }

    // Listener per notificare il rating alle domande dell'utente loggato
    private void addRatingListener(final Question question, String questionKey)
    {
        Util.getDatabase().getReference("Question").child(questionKey).child("ratings").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Non vengono notificate le votazioni effettuate dall'utente stesso sulla propria domanda
                if (!dataSnapshot.getKey().equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                {
                    if (question.ratings == null)
                        sendNotification(NotificationType.RATING, null);
                    // Vengono notificate tutti rating ad esclusione di quelli gia presenti al momento del caricamento
                    else if (!question.ratings.keySet().contains(dataSnapshot.getKey()))
                        sendNotification(NotificationType.RATING, null);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Listener per notificare i like/commenti relativi alle risposte dell'utente loggato
    private void addLikeCommentListener()
    {
        // Listener su tutte le risposte effettuate dall'utente loggato (presenti e future)
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("answers").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                final String questionKey = dataSnapshot.getValue(String.class);

                // Per ogni risposta data, ne vengono recuperate le informazioni
                Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        final Answer answer = dataSnapshot.getValue(Answer.class);

                        // Listener sul campo "likes" di ogni risposta: ogni volta che qualcuno mette "mi piace" alla risposta,
                        // viene visualizzata una notifica (vengono esclusi tutti i "mi piace" già presenti del database)
                        Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(dataSnapshot.getKey()).child("likes").addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s)
                            {
                                if (answer.likes == null)
                                    sendNotification(NotificationType.LIKE, null);
                                else if (!answer.likes.keySet().contains(dataSnapshot.getKey()))
                                    sendNotification(NotificationType.LIKE, null);
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) { }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });

                        // Listener sul campo "comments" di ogni risposta: ogni volta che qualcuno scrive un commento alla risposta,
                        // viene visualizzata una notifica (vengono esclusi tutti i commenti già presenti del database)
                        Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(dataSnapshot.getKey()).child("comments").addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s)
                            {
                                if (!dataSnapshot.getValue(Comment.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                                {
                                    if (answer.comments == null)
                                        sendNotification(NotificationType.COMMENT_ANSWER, null);
                                    else if (!answer.comments.keySet().contains(dataSnapshot.getKey()))
                                        sendNotification(NotificationType.COMMENT_ANSWER, null);
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) { }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Listener per notificare i commenti relativi alle domande dell'utente loggato (ma non sulle proprie risposte
    // dal momento che se ne occupa già il metodo addLikeCommentListener)
    private void addGenericCommentListener(final Question question, final String questionKey)
    {
        // Listener su tutte le risposte della domanda
        Util.getDatabase().getReference("Question").child(questionKey).child("answers").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                final String answerKey = dataSnapshot.getKey();
                // Se la risposta non è stata scritta dall'utente stesso, viene agganciato un listener
                // su tutti i suoi commenti
                if (!dataSnapshot.getValue(Answer.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                    Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(dataSnapshot.getKey()).child("comments").addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s)
                        {
                            // Se il commento non è stato scritto dall'utente stesso, viene inviata una notifica
                            // (vengono ignorati i commenti già presenti nel database)
                            if (!dataSnapshot.getValue(Comment.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                            {
                                // Notifica dal primo commento di tutta la domanda
                                if (question.answers == null)
                                    sendNotification(NotificationType.COMMENT_QUESTION, null);
                                // Notifica dal primo commento di tutta la risposta
                                else if (question.answers.get(answerKey).comments == null)
                                    sendNotification(NotificationType.COMMENT_QUESTION, null);
                                // Notifica del commento che non è già presente sul database
                                else if (question.answers.get(answerKey).comments != null && !question.answers.get(answerKey).comments.keySet().contains(dataSnapshot.getKey()))
                                    sendNotification(NotificationType.COMMENT_QUESTION, null);
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) { }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private class getBitmapFromUrl extends AsyncTask<String, Void, Bitmap>
    {
        private NotificationType type;

        getBitmapFromUrl(NotificationType type)
        {
            this.type = type;
        }

        protected Bitmap doInBackground(String... urls)
        {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try
            {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result)
        {
            sendNotification(type, result);
        }
    }

    // Metodo per creare la notifica in base al tipo passato
    private void sendNotification(NotificationType type, Bitmap profilePic)
    {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setColor(Color.RED)
                .setSmallIcon(R.drawable.ic_school_black_24dp)
                //.setSubText("5")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                //{Delay Iniziale, Durata Vibrazione 1, Pausa 1, ...}
                .setVibrate(new long[]{0, 300, 200, 300})
                .setLights(Color.RED, 800, 4000);

        switch (type)
        {
            case QUESTION:
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                {
                    // Numero nuove domande
                    if (questionCount == 1)
                        mBuilder.setContentTitle("1 nuova domanda");
                    else
                        mBuilder.setContentTitle(questionCount + " nuove domande");

                    // Gestione degli autori delle domande
                    if ((questionList.size() == 1) && (questionCount == 1))
                    {
                        mBuilder.setContentText("Postata da " + questionList.get(0));
                        mBuilder.setLargeIcon(profilePic);
                    }
                    else if ((questionList.size() == 1) && (questionCount > 1))
                    {
                        mBuilder.setContentText("Postate da " + questionList.get(0));
                        mBuilder.setLargeIcon(profilePic);
                    }
                    else if (questionList.size() == 2)
                    {
                        mBuilder.setContentText("Postate da " + questionList.get(0) + " e " + questionList.get(1));
                        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    }
                    else
                    {
                        mBuilder.setContentText("Postate da " + questionList.get(0) + " e altre " + (questionList.size() - 1) + " persone");
                        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    }
                }
                // Versione precedente a Nougat
                else
                {
                    mBuilder.setContentTitle(getResources().getString(R.string.app_name));
                    // Gestione degli autori delle domande
                    if ((questionList.size() == 1) && (questionCount == 1))
                    {
                        mBuilder.setContentText(questionCount + " nuova domanda postata da " + questionList.get(0));
                        mBuilder.setLargeIcon(profilePic);
                    }
                    else if ((questionList.size() == 1) && (questionCount > 1))
                    {
                        mBuilder.setContentText(questionCount + " nuove domande postate da " + questionList.get(0));
                        mBuilder.setLargeIcon(profilePic);
                    }
                    else if (questionList.size() == 2)
                    {
                        mBuilder.setContentText( questionCount + " nuove domande postate da " + questionList.get(0) + " e " + questionList.get(1));
                        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    }
                    else
                    {
                        mBuilder.setContentText(questionCount + " nuove domande postate da " + questionList.get(0) + " e altre " + (questionList.size() - 1) + " persone");
                        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    }
                }
                break;
            default:
                break;
        }

        Intent resultIntent = new Intent(BackgroundService.this, PostActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addParentStack(PostActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(getNotificationID(type), mBuilder.build());
        //ToDo: custom notification based on question, answer, etc...
    }

    // Metodo per restituire l'identificativo della notifica, dato il tipo
    private int getNotificationID(NotificationType type)
    {
        switch (type)
        {
            case QUESTION:
                return 1;
            case ANSWER:
                return 2;
            case COMMENT_QUESTION:
                return 3;
            case COMMENT_ANSWER:
                return 4;
            case RATING:
                return 5;
            case LIKE:
                return 6;
            default:
                return 0;
        }
    }

    public static void resetNotification()
    {
        questionList.clear();
        questionCount = 0;
    }
}
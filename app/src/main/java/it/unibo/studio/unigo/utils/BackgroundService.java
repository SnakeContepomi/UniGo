package it.unibo.studio.unigo.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.preference.PreferenceManager;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.ChatActivity;
import it.unibo.studio.unigo.main.QuestionDetailActivity;
import it.unibo.studio.unigo.main.MainActivity;
import it.unibo.studio.unigo.main.fragments.SettingsFragment;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.ChatRoom;
import it.unibo.studio.unigo.utils.firebase.Comment;
import it.unibo.studio.unigo.utils.firebase.Message;
import it.unibo.studio.unigo.utils.firebase.Question;
import it.unibo.studio.unigo.utils.firebase.Survey;

// Servizio in background che viene fatto partire al boot del telefono o all'avvio dell'app, che recupera
// i cambiamenti da Firebase
public class BackgroundService extends Service
{
    static final String CURRENT_COURSE_KEY = "course_key";
    static final String LAST_QUESTION_READ = "last_key";
    static final String LAST_SURVEY_READ = "last_survey";

    public static boolean isRunning = false;
    // Liste dei nomi degli utenti che hanno generato una notifica di ciascun tipo
    private static List<String> questionList = new ArrayList<>();
    private static List<String> answerList = new ArrayList<>();
    private static List<String> commentQuestionList = new ArrayList<>();
    private static List<String> commentAnswerList = new ArrayList<>();
    private static List<String> ratingList = new ArrayList<>();
    private static List<String> likeList = new ArrayList<>();
    private static List<String> chatRoomList = new ArrayList<>();
    private static List<String> surveyList = new ArrayList<>();
    // Numero di notifiche di ciascun tipo
    private static int questionCount = 0;
    private static int answerCount = 0;
    private static int commentQuestionCount = 0;
    private static int commentAnswerCount = 0;
    private static int ratingCount = 0;
    private static int likeCount = 0;
    // Per separare il numero di messaggi contenuti in ciascuna chatroom, occorre utilizzare una lista
    private static List<Integer> chatRoomCount = new ArrayList<>();
    private static int surveyCount = 0;
    private SharedPreferences prefs;
    /*
        Tipi di Notifiche in base al valore della variabile notifyID:
        1 - QUESTION: nuova domanda inserita
        2 - ANSWER: nuova risposta alla domanda dell'utente
        3 - COMMENT_QUESTION: nuovo commento generico ad una domanda dell'utente
        4 - COMMENT_ANSWER: nuovo commento riguardante una risposta effettuata dall'utente
        5 - RATING: aumento di importanza di una domanda dell'utente
        6 - LIKE: like ricevuto ad una risposta del'utente
        7 - CHATROOM: nuovo messaggio all'interno di una ChatRoom
     */
    private enum NotificationType {QUESTION, ANSWER, COMMENT_QUESTION, COMMENT_ANSWER, RATING, LIKE, CHATROOM, SURVEY}

    // Avvio del servizio in background
    @Override
    public void onCreate()
    {
        super.onCreate();
        isRunning = true;
        /*
        DEBUG FOR LOGOUT FUNCTION
        Log.d("prova", "BackgroundService, user: " + FirebaseAuth.getInstance().getCurrentUser());
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                if (FirebaseAuth.getInstance().getCurrentUser() == null)
                    Log.d("prova", "Logged out");
                else
                    Log.d("prova", "Logged in");
            }
        });*/
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
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Se la chiave del corso non è ancora stata recuperata:
        // - Viene eseguita una query per recuperarla, conoscendo l'email dell'utente
        // - Viene eseguita una query per recuperare tutte le chiavi delle domande che fanno parte di quel corso,
        //   ordinate cronologicamente (sulla chiave push)
        //   Letta l'ultima domanda, viene fatto partire il listener che si occupa di notificare l'inserimento delle nuove domande
        // - Il listener sul campo questions della tabella "Course" permette di avvisare l'utente riguardo l'inserimento di nuove domande
        //   tramite delle notifiche
        // - Ad ogni domanda del corso, viene infine agganciato un listener che ne permette di gestire eventuali modifiche
        if (prefs.getString(CURRENT_COURSE_KEY, "").equals(""))
        {
            // Query per recuperare la chiave del corso
            Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("courseKey")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {
                            // Memorizzazione nelle SharedPreferences della chiave del corso
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(CURRENT_COURSE_KEY, dataSnapshot.getValue(String.class));
                            editor.apply();
                            Util.CURRENT_COURSE_KEY = dataSnapshot.getValue(String.class);

                            // Recupero delle domande esistenti nel corso
                            Util.getDatabase().getReference("Course").child(dataSnapshot.getValue(String.class)).child("questions").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot)
                                {
                                    // Se il corso ha almeno una domanda associata, le scorro tutte per recuperare l'ultima
                                    // domanda inserita e memorizzarla nella SharedPreference LAST_QUESTION_READ
                                    // (utilizzata per evitare di notificare l'utente per le domande già presenti)
                                    if (dataSnapshot.getChildrenCount() > 0)
                                    {
                                        Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                                        DataSnapshot child;
                                        // Per ogni chiave recuperata, viene eseguita una query sulla tabella "Question" al fine di ottenerne
                                        // i dettagli. Ogni domanda viene quindi inserita nella lista presente in Utils ed infine
                                        // viene inizializzato il listener per l'inserimento delle nuove domande
                                        while (iterator.hasNext())
                                        {
                                            child = iterator.next();
                                            updateLastQuestionRead(child.getKey());

                                            if (!iterator.hasNext())
                                                addNewQuestionListener();
                                        }
                                    }
                                    else
                                        addNewQuestionListener();
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
        {
            Util.CURRENT_COURSE_KEY = prefs.getString(CURRENT_COURSE_KEY, "");
            addNewQuestionListener();
        }

        // Vengono inizializzati i listener relativi alle domande effettuate (listener su nuove risposte, commenti o like ricevuti)
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("questions").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                Util.getDatabase().getReference("Question").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        addRatingListener(dataSnapshot.getValue(Question.class), dataSnapshot.getKey());
                        addNewAnswerListener(dataSnapshot.getValue(Question.class), dataSnapshot.getKey());
                        addGenericCommentListener(dataSnapshot.getValue(Question.class), dataSnapshot.getKey());
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

        // Listener sui like ricevuti sulle risposte dell'utente
        // + Listener sui commenti effettuati relativi alle risposte dell'utente
        addLikeCommentListener();

        addChatRoomListener();
        initSurveyListener();
    }

    // Listener per notificare tutte le nuove domande inserite nel corso dell'utente
    // (evitando di notificare quelle già presenti)
    private void addNewQuestionListener()
    {
        Util.getDatabase()
            .getReference("Question")
            .orderByChild("course_key")
            .equalTo(PreferenceManager.getDefaultSharedPreferences(this).getString(CURRENT_COURSE_KEY, ""))
            .addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)
                {
                    final String questionKey = dataSnapshot.getKey();
                    // Vengono evitati gli eventi relativi all'aggiunta di domande già presenti nel database e
                    // l'aggiunta delle domande dell'utente loggato
                    if (questionKey.compareTo(prefs.getString(LAST_QUESTION_READ, "")) > 0
                        && !dataSnapshot.child("user_key").getValue(String.class).equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                    {
                        updateLastQuestionRead(dataSnapshot.getKey());
                        // Recupero delle informazioni dell'utente e invio di una notifica
                        Util.getDatabase().getReference("User").child(dataSnapshot.child("user_key").getValue(String.class)).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (!questionList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                    questionList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                questionCount++;
                                new getBitmapFromUrl(NotificationType.QUESTION, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
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

    // Metodo per memorizzare nelle shared preferences la chiave dell'ultima domanda caricata
    private void updateLastQuestionRead(String key)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_QUESTION_READ, key);
        editor.apply();
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
                        Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Answer.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (!answerList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                    answerList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                answerCount++;
                                new getBitmapFromUrl(NotificationType.ANSWER, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
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
        // Altrimenti se la domanda non ha nessuna risposta, vengono notificate direttamente tutti i nuovi inserimenti
        else
            Util.getDatabase().getReference("Question").child(questionKey).child("answers").addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)
                {
                    // Viene evitata la risposta scritta dall'utente stesso
                    if (!dataSnapshot.getValue(Answer.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                        Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Answer.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (!answerList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                    answerList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                answerCount++;
                                new getBitmapFromUrl(NotificationType.ANSWER, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
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

    // Listener per notificare il rating alle domande dell'utente loggato
    private void addRatingListener(final Question question, final String questionKey)
    {
        Util.getDatabase().getReference("Question").child(questionKey).child("ratings").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Non vengono notificate le votazioni effettuate dall'utente stesso sulla propria domanda
                if (!dataSnapshot.getKey().equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                {
                    if (question.ratings == null)
                        Util.getDatabase().getReference("User").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (!ratingList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                    ratingList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                ratingCount++;
                                new getBitmapFromUrl(NotificationType.RATING, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
                    // Vengono notificate tutti rating ad esclusione di quelli gia presenti al momento del caricamento
                    else if (!question.ratings.keySet().contains(dataSnapshot.getKey()))
                        Util.getDatabase().getReference("User").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (!ratingList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                    ratingList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                ratingCount++;
                                new getBitmapFromUrl(NotificationType.RATING, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
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
                                    Util.getDatabase().getReference("User").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (!likeList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                                likeList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                            likeCount++;
                                            new getBitmapFromUrl(NotificationType.LIKE, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) { }
                                    });
                                else if (!answer.likes.keySet().contains(dataSnapshot.getKey()))
                                    Util.getDatabase().getReference("User").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (!likeList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                                likeList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                            likeCount++;
                                            new getBitmapFromUrl(NotificationType.LIKE, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
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

                        // Listener sul campo "comments" di ogni risposta: ogni volta che qualcuno scrive un commento alla risposta,
                        // viene visualizzata una notifica (vengono esclusi tutti i commenti già presenti del database)
                        Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(dataSnapshot.getKey()).child("comments").addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s)
                            {
                                if (!dataSnapshot.getValue(Comment.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                                {
                                    if (answer.comments == null)
                                        Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Comment.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot)
                                            {
                                                if (!commentAnswerList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                                    commentAnswerList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                                commentAnswerCount++;
                                                new getBitmapFromUrl(NotificationType.COMMENT_ANSWER, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) { }
                                        });
                                    else if (!answer.comments.keySet().contains(dataSnapshot.getKey()))
                                        Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Comment.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot)
                                            {
                                                if (!commentAnswerList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                                    commentAnswerList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                                commentAnswerCount++;
                                                new getBitmapFromUrl(NotificationType.COMMENT_ANSWER, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) { }
                                        });
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
                    Util.getDatabase().getReference("Question").child(questionKey).child("answers").child(answerKey).child("comments").addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s)
                        {
                            // Se il commento non è stato scritto dall'utente stesso, viene inviata una notifica
                            // (vengono ignorati i commenti già presenti nel database)
                            if (!dataSnapshot.getValue(Comment.class).user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                            {
                                // Notifica dal primo commento di tutta la domanda
                                if (question.answers == null)
                                    Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Comment.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (!commentQuestionList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                                commentQuestionList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                            commentQuestionCount++;
                                            new getBitmapFromUrl(NotificationType.COMMENT_QUESTION, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) { }
                                    });
                                // Notifica dal primo commento di tutta la risposta
                                else if (question.answers.get(answerKey).comments == null)
                                    Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Comment.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (!commentQuestionList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                                commentQuestionList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                            commentQuestionCount++;
                                            new getBitmapFromUrl(NotificationType.COMMENT_QUESTION, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) { }
                                    });
                                // Notifica del commento che non è già presente sul database
                                else if (question.answers.get(answerKey).comments != null && !question.answers.get(answerKey).comments.keySet().contains(dataSnapshot.getKey()))
                                    Util.getDatabase().getReference("User").child(dataSnapshot.getValue(Comment.class).user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (!commentQuestionList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                                commentQuestionList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                            commentQuestionCount++;
                                            new getBitmapFromUrl(NotificationType.COMMENT_QUESTION, questionKey).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) { }
                                    });
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

    private void addChatRoomListener()
    {
        // Per ogni conversazione viene avviato un listener che rileva i cambiamenti su "last_message_id"
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("chat_rooms").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                final String chatId = dataSnapshot.getKey();

                Util.getDatabase().getReference("ChatRoom").child(chatId).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) { }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s)
                    {
                        if (dataSnapshot.getKey().equals("last_message_id"))
                        {
                            final String msgId = dataSnapshot.getValue(String.class);

                            // Se è il primo messaggio della conversazione, viene memorizzato il riferimento di essa nelle SharedPrefernce,
                            // e viene notificato il messaggio solo se questo proviene dal destinatario
                            if (prefs.getString(chatId, "").equals(""))
                            {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(chatId, dataSnapshot.getValue(String.class));
                                editor.apply();

                                Util.getDatabase().getReference("ChatRoom").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        final ChatRoom chatRoom = dataSnapshot.getValue(ChatRoom.class);

                                        // Se il messaggio proviene dal destinatario, viene notificato
                                        if (!chatRoom.messages.get(msgId).sender_id.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                                        {
                                            final String mail;
                                            String name;

                                            // Viene recuperato il nome del mittente del messagggio
                                            // (se l'utilizzatore dell'app è 'utente_1', allora il mittente del messaggio sarà 'utente_2' e viceversa
                                            if (chatRoom.name_1.equals(Util.getCurrentUser().getDisplayName()))
                                            {
                                                name = chatRoom.name_2;
                                                mail = chatRoom.id_2;
                                            }
                                            else
                                            {
                                                name = chatRoom.name_1;
                                                mail = chatRoom.id_1;
                                            }

                                            if (!chatRoomList.contains(name))
                                                chatRoomList.add(name);
                                            int pos = chatRoomList.indexOf(name);
                                            // Se la condizione è vera, è stato aggiunto un altro mittente, quindi
                                            // bisogna creare l'elemento anche in chatRoomCount, altrmenti si modifica
                                            // quello esistente
                                            if (chatRoomCount.size() < chatRoomList.size())
                                                chatRoomCount.add(1);
                                            else
                                                chatRoomCount.set(pos, chatRoomCount.get(pos) + 1);

                                            Util.getDatabase().getReference("User").child(mail).child("photoUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot)
                                                {
                                                    new getBitmapFromUrl(NotificationType.CHATROOM, mail, chatRoom.messages.get(msgId)).execute(dataSnapshot.getValue(String.class));
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) { }
                                            });
                                        }

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) { }
                                });
                            }
                            else
                            {
                                // Se esiste già una SharedPreference associata alla ChatRoom,
                                // viene controllato se il messaggio letto è più recente dell'ultimo notificato
                                // (questo controllo evita di notificare all'utente tutti i messaggi sin dall'inizio)
                                if (msgId.compareTo(prefs.getString(chatId, "")) > 0)
                                {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString(chatId, dataSnapshot.getValue(String.class));
                                    editor.apply();

                                    Util.getDatabase().getReference("ChatRoom").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            final ChatRoom chatRoom = dataSnapshot.getValue(ChatRoom.class);

                                            // Se il messaggio proviene dal destinatario, viene notificato
                                            if (!chatRoom.messages.get(msgId).sender_id.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                                            {
                                                final String mail;
                                                String name;

                                                // Viene recuperato il nome del mittente del messagggio
                                                // (se l'utilizzatore dell'app è 'utente_1', allora il mittente del messaggio sarà 'utente_2' e viceversa
                                                if (chatRoom.name_1.equals(Util.getCurrentUser().getDisplayName()))
                                                {
                                                    name = chatRoom.name_2;
                                                    mail = chatRoom.id_2;
                                                }
                                                else
                                                {
                                                    name = chatRoom.name_1;
                                                    mail = chatRoom.id_1;
                                                }

                                                if (!chatRoomList.contains(name))
                                                    chatRoomList.add(name);
                                                int pos = chatRoomList.indexOf(name);
                                                // Se la condizione è vera, è stato aggiunto un altro mittente, quindi
                                                // bisogna creare l'elemento anche in chatRoomCount, altrmenti si modifica
                                                // quello esistente
                                                if (chatRoomCount.size() < chatRoomList.size())
                                                    chatRoomCount.add(1);
                                                else
                                                    chatRoomCount.set(pos, chatRoomCount.get(pos) + 1);

                                                Util.getDatabase().getReference("User").child(mail).child("photoUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot)
                                                    {
                                                        new getBitmapFromUrl(NotificationType.CHATROOM, mail, chatRoom.messages.get(msgId)).execute(dataSnapshot.getValue(String.class));
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) { }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) { }
                                    });
                                }
                            }
                        }
                    }

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

    private void initSurveyListener()
    {
        Util.getDatabase().getReference("Survey").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getChildrenCount() > 0)
                {
                    Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                    DataSnapshot child;

                    while (iterator.hasNext())
                    {
                        child = iterator.next();
                        updateLastSurveyRead(child.getKey());

                        if (!iterator.hasNext())
                            addSurveyListener();
                    }
                }
                else
                    addSurveyListener();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per memorizzare nelle shared preferences la chiave dell'ultimo sondaggio letto
    private void updateLastSurveyRead(String key)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_SURVEY_READ, key);
        editor.apply();
    }

    private void addSurveyListener()
    {
        Util.getDatabase().getReference("Survey").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Sondaggio appartenente al proprio corso universitario
                if (dataSnapshot.getValue(Survey.class).course_key.equals(Util.CURRENT_COURSE_KEY))
                    // Nuovo sondaggio
                    if (dataSnapshot.getKey().compareTo(prefs.getString(LAST_SURVEY_READ, "")) > 0)
                    {
                        updateLastSurveyRead(dataSnapshot.getKey());

                        final Survey survey = dataSnapshot.getValue(Survey.class);
                        if (!survey.user_key.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                            Util.getDatabase().getReference("User").child(survey.user_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot)
                                {
                                    if (!surveyList.contains(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class)))
                                        surveyList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                                    surveyCount++;
                                    new getBitmapFromUrl(survey.user_key).execute(dataSnapshot.child("photoUrl").getValue(String.class));
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) { }
                            });
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

    // Metodo per scaricare in background l'immagine profilo di un utente e inviare successivamente una notifica
    // del tipo desiderato
    private class getBitmapFromUrl extends AsyncTask<String, Void, Bitmap>
    {
        private NotificationType type;
        private String questionKey, mail;
        private Message msg;

        getBitmapFromUrl(NotificationType type, String questionKey)
        {
            this.type = type;
            this.questionKey = questionKey;
        }

        getBitmapFromUrl(NotificationType type, String mail, Message msg)
        {
            this.type = type;
            this.mail = mail;
            this.msg = msg;
        }

        // Costruttore per SurveyNotifications
        getBitmapFromUrl(String mail)
        {
            this.type = NotificationType.SURVEY;
            this.mail = mail;
        }

        protected Bitmap doInBackground(String... urls)
        {
            String urldisplay = urls[0];

            // Se l'utente non ha un immagine profilo, viene ritornata quella di default
            if (urldisplay.equals(getResources().getString(R.string.empty_profile_pic_url)))
                return BitmapFactory.decodeResource(getResources(), R.drawable.empty_profile_pic);

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
            return getCircleBitmap(mIcon11);
        }

        protected void onPostExecute(Bitmap result)
        {
            if (type == NotificationType.CHATROOM)
                sendNotification(type, result, mail, msg);
            else if (type == NotificationType.SURVEY)
                    sendNotification(type, result);
            else
                sendNotification(type, result, questionKey);
        }
    }

    // Metodo per creare la notifica in base al tipo passato
    private void sendNotification(NotificationType type, Bitmap profilePic, String questionKey)
    {
        // Le notifiche vengono create solamente se è abilitata l'opzione in SettingsFragment (SwitchPreference)
        // e l'utente desidera essere informato di quel particolare evento (MultiSelectListPreference)
        if (prefs.getBoolean(SettingsFragment.KEY_PREF_NOTIF, getResources().getBoolean(R.bool.pref_notification_defVal)) && isNotificationEventSelected(type))
        {
            Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
                    .setColor(Color.RED)
                    .setSmallIcon(R.drawable.ic_school_black_24dp)
                    .setPriority(getNotificationPriority())
                    //{Delay Iniziale, Durata Vibrazione 1, Pausa 1, ...}
                    .setVibrate(getNotificationVibration())
                    .setLights(getNotificationColor(), 800, 4000)
                    .setSound(getNotificationRingtone())
                    .setAutoCancel(true);

            switch (type)
            {
                // Notifica di una nuova domanda inserita
                case QUESTION:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        // Numero nuove domande
                        if (questionCount == 1)
                            mBuilder.setContentTitle("1 nuova domanda");
                        else
                            mBuilder.setContentTitle(questionCount + " nuove domande");

                        // Gestione degli autori delle domande
                        if (questionList.size() == 1) {
                            mBuilder.setContentText(questionList.get(0) + " ha aggiunto una nuova domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(questionList.get(0) + " ha aggiunto una nuova domanda"));
                        } else if (questionList.size() == 2) {
                            mBuilder.setContentText(questionList.get(0) + " e " + questionList.get(1) + " hanno aggiunto una nuova domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(questionList.get(0) + " e " + questionList.get(1) + " hanno aggiunto una nuova domanda"));
                        } else {
                            mBuilder.setContentText(questionList.get(0) + " e altre " + (questionList.size() - 1) + " persone hanno aggiunto una nuova domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(questionList.get(0) + " e altre " + (questionList.size() - 1) + " persone hanno aggiunto una nuova domanda"));
                        }
                    }
                    // Versione precedente a Nougat
                    else {
                        mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                        // Numero nuove domande
                        if (questionCount == 1)
                            mBuilder.setSubText("1 nuova domanda");
                        else
                            mBuilder.setSubText(questionCount + " nuove domande");

                        // Gestione degli autori delle domande
                        if (questionList.size() == 1) {
                            mBuilder.setContentText(questionList.get(0) + " ha aggiunto una nuova domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(questionList.get(0) + " ha aggiunto una nuova domanda"));
                        } else if (questionList.size() == 2) {
                            mBuilder.setContentText(questionList.get(0) + " e " + questionList.get(1) + " hanno aggiunto una nuova domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(questionList.get(0) + " e " + questionList.get(1) + " hanno aggiunto una nuova domanda"));
                        } else {
                            mBuilder.setContentText(questionList.get(0) + " e altre " + (questionList.size() - 1) + " persone hanno aggiunto una nuova domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(questionList.get(0) + " e altre " + (questionList.size() - 1) + " persone hanno aggiunto una nuova domanda"));
                        }
                    }
                    break;

                // Notifica di una nuova risposta inserita
                case ANSWER:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        // Numero nuove risposte
                        if (answerCount == 1)
                            mBuilder.setContentTitle("1 nuova risposta");
                        else
                            mBuilder.setContentTitle(answerCount + " nuove risposte");

                        // Gestione degli autori delle risposte
                        if (answerList.size() == 1) {
                            mBuilder.setContentText(answerList.get(0) + " ha risposto ad una tua domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(answerList.get(0) + " ha risposto ad una tua domanda"));
                        } else if (answerList.size() == 2) {
                            mBuilder.setContentText(answerList.get(0) + " e " + answerList.get(1) + " hanno risposto ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(answerList.get(0) + " e " + answerList.get(1) + " hanno risposto ad una tua domanda"));
                        } else {
                            mBuilder.setContentText(answerList.get(0) + " e altre " + (answerList.size() - 1) + " persone hanno risposto ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(answerList.get(0) + " e altre " + (answerList.size() - 1) + " persone hanno risposto ad una tua domanda"));
                        }
                    }
                    // Versione precedente a Nougat
                    else {
                        mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                        // Numero nuove risposte
                        if (answerCount == 1)
                            mBuilder.setSubText("1 nuova risposta");
                        else
                            mBuilder.setSubText(answerCount + " nuove risposte");

                        // Gestione degli autori delle risposte
                        if (answerList.size() == 1) {
                            mBuilder.setContentText(answerList.get(0) + " ha risposto ad una tua domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(answerList.get(0) + " ha risposto ad una tua domanda"));
                        } else if (answerList.size() == 2) {
                            mBuilder.setContentText(answerList.get(0) + " e " + answerList.get(1) + " hanno risposto ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(answerList.get(0) + " e " + answerList.get(1) + " hanno risposto ad una tua domanda"));
                        } else {
                            mBuilder.setContentText(answerList.get(0) + " e altre " + (answerList.size() - 1) + " persone hanno risposto ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(answerList.get(0) + " e altre " + (answerList.size() - 1) + " persone hanno risposto ad una tua domanda"));
                        }
                    }
                    break;

                // Notifica di un nuovo commento ad una domanda scritta dall'utente
                case COMMENT_QUESTION:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        // Numero nuovi commenti
                        if (commentQuestionCount == 1)
                            mBuilder.setContentTitle("1 nuovo commento");
                        else
                            mBuilder.setContentTitle(commentQuestionCount + " nuovi commenti");

                        // Gestione degli autori dei commenti
                        if (commentQuestionList.size() == 1) {
                            mBuilder.setContentText(commentQuestionList.get(0) + " ha aggiunto un commento relativo ad una tua domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentQuestionList.get(0) + " ha aggiunto un commento relativo ad una tua domanda"));
                        } else if (commentQuestionList.size() == 2) {
                            mBuilder.setContentText(commentQuestionList.get(0) + " e " + commentQuestionList.get(1) + " hanno aggiunto un commento relativo ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentQuestionList.get(0) + " e " + commentQuestionList.get(1) + " hanno aggiunto un commento relativo ad una tua domanda"));
                        } else {
                            mBuilder.setContentText(commentQuestionList.get(0) + " e altre " + (commentQuestionList.size() - 1) + " persone hanno aggiunto un commento relativo ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentQuestionList.get(0) + " e altre " + (commentQuestionList.size() - 1) + " persone hanno aggiunto un commento relativo ad una tua domanda"));
                        }
                    }
                    // Versione precedente a Nougat
                    else {
                        mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                        // Numero nuovi commenti
                        if (commentQuestionCount == 1)
                            mBuilder.setSubText("1 nuovo commento");
                        else
                            mBuilder.setSubText(commentQuestionCount + " nuovi commenti");

                        // Gestione degli autori dei commenti
                        if (commentQuestionList.size() == 1) {
                            mBuilder.setContentText(commentQuestionList.get(0) + " ha aggiunto un commento relativo ad una tua domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentQuestionList.get(0) + " ha aggiunto un commento relativo ad una tua domanda"));
                        } else if (commentQuestionList.size() == 2) {
                            mBuilder.setContentText(commentQuestionList.get(0) + " e " + commentQuestionList.get(1) + " hanno aggiunto un commento relativo ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentQuestionList.get(0) + " e " + commentQuestionList.get(1) + " hanno aggiunto un commento relativo ad una tua domanda"));
                        } else {
                            mBuilder.setContentText(commentQuestionList.get(0) + " e altre " + (commentQuestionList.size() - 1) + " persone hanno aggiunto un commento relativo ad una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentQuestionList.get(0) + " e altre " + (commentQuestionList.size() - 1) + " persone hanno aggiunto un commento relativo ad una tua domanda"));
                        }
                    }
                    break;

                // Notifica di un nuovo commento ad una risposta scritta dall'utente
                case COMMENT_ANSWER:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        // Numero nuovi commenti
                        if (commentAnswerCount == 1)
                            mBuilder.setContentTitle("1 nuovo commento");
                        else
                            mBuilder.setContentTitle(commentAnswerCount + " nuovi commenti");

                        // Gestione degli autori dei commenti
                        if (commentAnswerList.size() == 1) {
                            mBuilder.setContentText(commentAnswerList.get(0) + " ha commentato una tua risposta");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentAnswerList.get(0) + " ha commentato una tua risposta"));
                        } else if (commentAnswerList.size() == 2) {
                            mBuilder.setContentText(commentAnswerList.get(0) + " e " + commentAnswerList.get(1) + " hanno commentato una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentAnswerList.get(0) + " e " + commentAnswerList.get(1) + " hanno commentato una tua risposta"));
                        } else {
                            mBuilder.setContentText(commentAnswerList.get(0) + " e altre " + (commentAnswerList.size() - 1) + " persone hanno commentato una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentAnswerList.get(0) + " e altre " + (commentAnswerList.size() - 1) + " persone hanno commentato una tua risposta"));
                        }
                    }
                    // Versione precedente a Nougat
                    else {
                        mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                        // Numero nuovi commenti
                        if (commentAnswerCount == 1)
                            mBuilder.setSubText("1 nuovo commento");
                        else
                            mBuilder.setSubText(commentAnswerCount + " nuovi commenti");

                        // Gestione degli autori dei commenti
                        if (commentAnswerList.size() == 1) {
                            mBuilder.setContentText(commentAnswerList.get(0) + " ha commentato una tua risposta");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentAnswerList.get(0) + " ha commentato una tua risposta"));
                        } else if (commentAnswerList.size() == 2) {
                            mBuilder.setContentText(commentAnswerList.get(0) + " e " + commentAnswerList.get(1) + " hanno commentato una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentAnswerList.get(0) + " e " + commentAnswerList.get(1) + " hanno commentato una tua risposta"));
                        } else {
                            mBuilder.setContentText(commentAnswerList.get(0) + " e altre " + (commentAnswerList.size() - 1) + " persone hanno commentato una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(commentAnswerList.get(0) + " e altre " + (commentAnswerList.size() - 1) + " persone hanno commentato una tua risposta"));
                        }
                    }
                    break;

                // Notifica di un nuovo voto ottenuto su una domanda dell'utente
                case RATING:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        // Numero nuovi voti
                        if (ratingCount == 1)
                            mBuilder.setContentTitle("1 nuovo voto");
                        else
                            mBuilder.setContentTitle(ratingCount + " nuovi voti");

                        // Gestione degli autori dei voti
                        if (ratingList.size() == 1) {
                            mBuilder.setContentText(ratingList.get(0) + " trova interessante una tua domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(ratingList.get(0) + " trova interessante una tua domanda"));
                        } else if (ratingList.size() == 2) {
                            mBuilder.setContentText(ratingList.get(0) + " e " + ratingList.get(1) + " trovano interessante una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(ratingList.get(0) + " e " + ratingList.get(1) + " trovano interessante una tua domanda"));
                        } else {
                            mBuilder.setContentText(ratingList.get(0) + " e altre " + (ratingList.size() - 1) + " persone trovano interessante una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(ratingList.get(0) + " e altre " + (ratingList.size() - 1) + " persone trovano interessante una tua domanda"));
                        }
                    }
                    // Versione precedente a Nougat
                    else {
                        mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                        // Numero nuovi voti
                        if (ratingCount == 1)
                            mBuilder.setSubText("1 nuovo voto");
                        else
                            mBuilder.setSubText(ratingCount + " nuovi voti");

                        // Gestione degli autori dei voti
                        if (ratingList.size() == 1) {
                            mBuilder.setContentText(ratingList.get(0) + " trova interessante una tua domanda");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(ratingList.get(0) + " trova interessante una tua domanda"));
                        } else if (ratingList.size() == 2) {
                            mBuilder.setContentText(ratingList.get(0) + " e " + ratingList.get(1) + " trovano interessante una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(ratingList.get(0) + " e " + ratingList.get(1) + " trovano interessante una tua domanda"));
                        } else {
                            mBuilder.setContentText(ratingList.get(0) + " e altre " + (ratingList.size() - 1) + " persone trovano interessante una tua domanda");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText(ratingList.get(0) + " e altre " + (ratingList.size() - 1) + " persone trovano interessante una tua domanda"));
                        }
                    }
                    break;

                // Notifica di un nuovo like ottenuto su una risposta dell'utente
                case LIKE:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        // Numero nuovi like
                        if (likeCount == 1)
                            mBuilder.setContentTitle("1 nuovo like");
                        else
                            mBuilder.setContentTitle(likeCount + " nuovi like");

                        // Gestione degli autori dei like
                        if (likeList.size() == 1) {
                            mBuilder.setContentText("A " + likeList.get(0) + " piace una tua risposta");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText("A " + likeList.get(0) + " piace una tua risposta"));
                        } else if (likeList.size() == 2) {
                            mBuilder.setContentText("A " + likeList.get(0) + " e " + likeList.get(1) + " piace una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText("A " + likeList.get(0) + " e " + likeList.get(1) + " piace una tua risposta"));
                        } else {
                            mBuilder.setContentText("A " + likeList.get(0) + " e altre " + (likeList.size() - 1) + " persone piace una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText("A " + likeList.get(0) + " e altre " + (likeList.size() - 1) + " persone piace una tua risposta"));
                        }
                    }
                    // Versione precedente a Nougat
                    else {
                        mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                        // Numero nuovi like
                        if (likeCount == 1)
                            mBuilder.setSubText("1 nuovo like");
                        else
                            mBuilder.setSubText(likeCount + " nuovi like");

                        // Gestione degli autori dei like
                        if (likeList.size() == 1) {
                            mBuilder.setContentText("A " + likeList.get(0) + " piace una tua risposta");
                            mBuilder.setLargeIcon(profilePic);
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText("A " + likeList.get(0) + " piace una tua risposta"));
                        } else if (likeList.size() == 2) {
                            mBuilder.setContentText("A " + likeList.get(0) + " e " + likeList.get(1) + " piace una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText("A " + likeList.get(0) + " e " + likeList.get(1) + " piace una tua risposta"));
                        } else {
                            mBuilder.setContentText("A " + likeList.get(0) + " e altre " + (likeList.size() - 1) + " persone piace una tua risposta");
                            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                            mBuilder.setStyle(new Notification.BigTextStyle()
                                    .bigText("A " + likeList.get(0) + " e altre " + (likeList.size() - 1) + " persone piace una tua risposta"));
                        }
                    }
                    break;

                default:
                    break;
            }

            Intent resultIntent = new Intent(BackgroundService.this, QuestionDetailActivity.class);
            resultIntent.putExtra("question_key", questionKey);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(QuestionDetailActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(getNotificationID(type), mBuilder.build());
        }
    }

    // Metodo per creare notifiche relative alla parte della Chat (non avendo questionKey, funzionano in modo diverso)
    private void sendNotification(NotificationType type, Bitmap profilePic, String senderId, Message msg)
    {
        // Le notifiche vengono create solamente se è abilitata l'opzione in SettingsFragment (SwitchPreference)
        // e l'utente desidera essere informato di quel particolare evento (MultiSelectListPreference), in questo caso messaggi dalla chat
        if (prefs.getBoolean(SettingsFragment.KEY_PREF_NOTIF, getResources().getBoolean(R.bool.pref_notification_defVal)) && isNotificationEventSelected(type))
        {
            Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
                    .setColor(Color.RED)
                    .setSmallIcon(R.drawable.ic_school_black_24dp)
                    .setPriority(getNotificationPriority())
                    //{Delay Iniziale, Durata Vibrazione 1, Pausa 1, ...}
                    .setVibrate(getNotificationVibration())
                    .setLights(getNotificationColor(), 800, 4000)
                    .setSound(getNotificationRingtone())
                    .setAutoCancel(true);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            {
                // Numero di utenti che hanno inviato almeno un messaggio
                if (chatRoomList.size() == 1)
                {
                    mBuilder.setContentTitle(chatRoomList.get(0));
                    if (chatRoomCount.get(0) == 1)
                    {
                        mBuilder.setContentText(msg.message);
                        mBuilder.setStyle(new Notification.BigTextStyle()
                                .bigText(msg.message));
                    }
                    else
                    {
                        mBuilder.setContentText(chatRoomCount.get(0) + " nuovi messaggi");
                        mBuilder.setStyle(new Notification.BigTextStyle()
                                .bigText(chatRoomCount.get(0) + " nuovi messaggi"));
                    }
                    mBuilder.setLargeIcon(profilePic);
                }
                else
                {
                    // somma del numero di messaggi non letti nelle varie conversazioni
                    int msgCount = 0;
                    for(int msgPerChat : chatRoomCount)
                        msgCount += msgPerChat;

                    mBuilder.setContentTitle(getResources().getString(R.string.app_name));
                    mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    mBuilder.setContentText(msgCount + " nuovi messaggi in " + chatRoomList.size() + " conversazioni");
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(msgCount + " nuovi messaggi in " + chatRoomList.size() + " conversazioni"));
                }
            }
            // Versione precedente a Nougat
            else
            {
                mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                // Numero di utenti che hanno inviato almeno un messaggio
                if (chatRoomList.size() == 1)
                {
                    mBuilder.setContentTitle(chatRoomList.get(0));
                    if (chatRoomCount.get(0) == 1)
                    {
                        mBuilder.setContentText(msg.message);
                        mBuilder.setStyle(new Notification.BigTextStyle()
                                .bigText(msg.message));
                    }
                    else
                    {
                        mBuilder.setContentText(chatRoomCount.get(0) + " nuovi messaggi");
                        mBuilder.setStyle(new Notification.BigTextStyle()
                                .bigText(chatRoomCount.get(0) + " nuovi messaggi"));
                    }
                    mBuilder.setLargeIcon(profilePic);
                }
                else
                {
                    // somma del numero di messaggi non letti nelle varie conversazioni
                    int msgCount = 0;
                    for(int msgPerChat : chatRoomCount)
                        msgCount += msgPerChat;

                    mBuilder.setContentTitle(getResources().getString(R.string.app_name));
                    mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    mBuilder.setContentText(msgCount + " nuovi messaggi in " + chatRoomList.size() + " conversazioni");
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(msgCount + " nuovi messaggi in " + chatRoomList.size() + " conversazioni"));
                }
            }

            Intent resultIntent;
            if (chatRoomList.size() == 1)
            {
                if (!senderId.equals(Util.CURRENT_CHAT_KEY))
                {
                    resultIntent = new Intent(BackgroundService.this, ChatActivity.class);
                    resultIntent.putExtra("user_key", senderId);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                    stackBuilder.addParentStack(ChatActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(getNotificationID(type), mBuilder.build());
                }
            }
            // Se si ricevono messaggi da più di una chat
            else
            {
                if (!senderId.equals(Util.CURRENT_CHAT_KEY))
                {
                    resultIntent = new Intent(BackgroundService.this, MainActivity.class);
                    resultIntent.putExtra("open_chatroom_fragment", true);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                    stackBuilder.addParentStack(MainActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(getNotificationID(type), mBuilder.build());
                }
            }
        }
    }

    // Metodo per creare notifiche relative alla parte dei sondaggi (non avendo questionKey o messageKey, funzionano in modo diverso)
    private void sendNotification(NotificationType type, Bitmap profilePic)
    {
        // Le notifiche vengono create solamente se è abilitata l'opzione in SettingsFragment (SwitchPreference)
        // e l'utente desidera essere informato di quel particolare evento (MultiSelectListPreference)
        if (prefs.getBoolean(SettingsFragment.KEY_PREF_NOTIF, getResources().getBoolean(R.bool.pref_notification_defVal)) && isNotificationEventSelected(type))
        {
            Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
                    .setColor(Color.RED)
                    .setSmallIcon(R.drawable.ic_school_black_24dp)
                    .setPriority(getNotificationPriority())
                    //{Delay Iniziale, Durata Vibrazione 1, Pausa 1, ...}
                    .setVibrate(getNotificationVibration())
                    .setLights(getNotificationColor(), 800, 4000)
                    .setSound(getNotificationRingtone())
                    .setAutoCancel(true);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            {
                // Numero nuovi sondaggi
                if (surveyCount == 1)
                    mBuilder.setContentTitle("1 nuovo sondaggio");
                else
                    mBuilder.setContentTitle(surveyCount + " nuovi sondaggi");

                // Gestione degli autori dei sondaggi
                if (surveyList.size() == 1)
                {
                    mBuilder.setContentText(surveyList.get(0) + " ha creato un nuovo sondaggio");
                    mBuilder.setLargeIcon(profilePic);
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(surveyList.get(0) + " ha creato un nuovo sondaggio"));
                }
                else if (surveyList.size() == 2)
                {
                    mBuilder.setContentText(surveyList.get(0) + " e " + surveyList.get(1) + " hanno creato " +  surveyCount + " sondaggi");
                    mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(surveyList.get(0) + " e " + surveyList.get(1) + " hanno creato " + surveyCount + " sondaggi"));
                }
                else
                {
                    mBuilder.setContentText(surveyList.get(0) + " e altre " + (surveyList.size() - 1) + " persone hanno creato " + surveyCount + " sondaggi");
                    mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(surveyList.get(0) + " e altre " + (surveyList.size() - 1) + " persone hanno creato " + surveyCount + " sondaggi"));
                }
            }
            // Versione precedente a Nougat
            else
            {
                mBuilder.setContentTitle(getResources().getString(R.string.app_name));

                // Numero nuove domande
                if (surveyCount == 1)
                    mBuilder.setSubText("1 nuovo sondaggio");
                else
                    mBuilder.setSubText(surveyCount + " nuovi sondaggi");

                // Gestione degli autori delle domande
                if (surveyList.size() == 1)
                {
                    mBuilder.setContentText(surveyList.get(0) + " ha creato un nuovo sondaggio");
                    mBuilder.setLargeIcon(profilePic);
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(surveyList.get(0) + " ha creato un nuovo sondaggio"));
                }
                else if (surveyList.size() == 2)
                {
                    mBuilder.setContentText(surveyList.get(0) + " e " + surveyList.get(1) + " hanno creato " + surveyCount + " sondaggi");
                    mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(surveyList.get(0) + " e " + surveyList.get(1) + " hanno creato " + surveyCount + " sondaggi"));
                }
                else
                {
                    mBuilder.setContentText(surveyList.get(0) + " e altre " + (surveyList.size() - 1) + " persone hanno creato " + surveyCount + " sondaggi");
                    mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                    mBuilder.setStyle(new Notification.BigTextStyle()
                            .bigText(surveyList.get(0) + " e altre " + (surveyList.size() - 1) + " persone hanno creato " + surveyCount + " sondaggi"));
                }
            }

            Intent resultIntent = new Intent(BackgroundService.this, MainActivity.class);
            resultIntent.putExtra("open_survey_fragment", true);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(getNotificationID(type), mBuilder.build());
        }
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
            case CHATROOM:
                return 7;
            case SURVEY:
                return 8;
            default:
                return 0;
        }
    }

    // Metodo per resettare i dettagli delle notifiche una volta lette
    public static void resetNotification()
    {
        questionList.clear();
        answerList.clear();
        commentQuestionList.clear();
        commentAnswerList.clear();
        ratingList.clear();
        likeList.clear();

        questionCount = 0;
        answerCount = 0;
        commentQuestionCount = 0;
        commentAnswerCount = 0;
        ratingCount = 0;
        likeCount = 0;
    }

    public static void resetChatNotification(String recipientName)
    {
        int pos = chatRoomList.indexOf(recipientName);

        if (pos != -1)
        {
            chatRoomList.remove(pos);
            chatRoomCount.remove(pos);
        }
    }

    // Metodo per modificare la forma dell'immagine utente, rendendola circolare
    private Bitmap getCircleBitmap(Bitmap bitmap)
    {
        Bitmap output;
        Rect srcRect, dstRect;
        float r;
        final int larghezza = bitmap.getWidth();
        final int altezza = bitmap.getHeight();

        if (larghezza > altezza)
        {
            output = Bitmap.createBitmap(altezza, altezza, Bitmap.Config.ARGB_8888);
            int left = (larghezza - altezza) / 2;
            int right = left + altezza;
            srcRect = new Rect(left, 0, right, altezza);
            dstRect = new Rect(0, 0, altezza, altezza);
            r = altezza / 2;
        }
        else
        {
            output = Bitmap.createBitmap(larghezza, larghezza, Bitmap.Config.ARGB_8888);
            int top = (altezza - larghezza)/2;
            int bottom = top + larghezza;
            srcRect = new Rect(0, top, larghezza, bottom);
            dstRect = new Rect(0, 0, larghezza, larghezza);
            r = larghezza / 2;
        }

        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(r, r, r, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);

        bitmap.recycle();

        return output;
    }

    // Metodo che restituisce true se il tipo di evento è presente nelle shared preferences (MultiSelectListPreference):
    // true --> l'evento deve essere notificato
    // false --> l'evento viene scartato (non produce nessuna notifica)
    private boolean isNotificationEventSelected(NotificationType type)
    {
        String[] array = getResources().getStringArray(R.array.pref_notificationEvents_values);
        Set<String> set = new HashSet<>();
        Collections.addAll(set, array);

        Set<String> value = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getStringSet(SettingsFragment.KEY_PREF_NOTIF_EVENTS, set);

        switch (type)
        {
            case QUESTION:
                return (value.contains("0"));
            case ANSWER:
                return (value.contains("1"));
            case COMMENT_QUESTION:
                return (value.contains("2"));
            case COMMENT_ANSWER:
                return (value.contains("3"));
            case RATING:
                return (value.contains("4"));
            case LIKE:
                return (value.contains("5"));
            case CHATROOM:
                return (value.contains("6"));
            case SURVEY:
                return (value.contains("7"));
            default:
                return false;
        }
    }

    // Metodo per recuperare il colore del led di notifica dalle sharedPreferences
    private int getNotificationColor()
    {
        switch (PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsFragment.KEY_PREF_NOTIF_COLOR, getResources().getString(R.string.pref_notificationColor_defVal)))
        {
            case "0":
                return Color.TRANSPARENT;
            case "1":
                return Color.WHITE;
            case "2":
                return Color.RED;
            case "3":
                return Color.YELLOW;
            case "4":
                return Color.GREEN;
            case "5":
                return Color.CYAN;
            case "6":
                return Color.BLUE;
            case "7":
                return Color.MAGENTA;
            default:
                return Color.RED;
        }
    }

    // Metodo per recuperare la priorità delle notifiche dalle sharedPreferences
    private int getNotificationPriority()
    {
        switch (PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsFragment.KEY_PREF_NOTIF_PRIORITY, getResources().getString(R.string.pref_notificationPriority_defVal)))
        {
            case "0":
                return Notification.PRIORITY_MIN;
            case "1":
                return Notification.PRIORITY_DEFAULT;
            case "2":
                return Notification.PRIORITY_MAX;
            default:
                return Notification.PRIORITY_MAX;
        }
    }

    // Metodo per recuperare il tipo di vibrazione delle notifiche dalle sharedPreferences
    private long[] getNotificationVibration()
    {
        switch (PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsFragment.KEY_PREF_NOTIF_VIBRATION, getResources().getString(R.string.pref_notificationVibration_defVal)))
        {
            case "0":
                return new long[]{0};
            case "1":
                return new long[]{0, 300, 200, 300};
            case "2":
                return new long[]{0, 300, 200, 300};
            case "3":
                return new long[]{0, 700, 300, 700};
            default:
                return new long[]{0, 300, 200, 300};
        }
    }

    // Metodo per recuperare il tono delle notifiche dalle sharedPreferences
    private Uri getNotificationRingtone()
    {
        return Uri.parse(PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsFragment.KEY_PREF_NOTIF_RINGTONE, getResources().getString(R.string.pref_notificationRingtone_defVal)));
    }
}
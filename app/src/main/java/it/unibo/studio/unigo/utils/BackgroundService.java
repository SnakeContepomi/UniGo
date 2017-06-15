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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.DetailActivity;
import it.unibo.studio.unigo.main.fragments.SettingsFragment;
import it.unibo.studio.unigo.utils.firebase.Answer;
import it.unibo.studio.unigo.utils.firebase.Comment;
import it.unibo.studio.unigo.utils.firebase.Question;

// Servizio in background che viene fatto partire al boot del telefono o all'avvio dell'app, che recupera
// i cambiamenti da Firebase
public class BackgroundService extends Service
{
    static final String CURRENT_COURSE_KEY = "course_key";
    static final String LAST_QUESTION_READ = "last_key";

    public static boolean isRunning = false;
    // Liste dei nomi degli utenti che hanno generato una notifica di ciascun tipo
    private static List<String> questionList = new ArrayList<>();
    private static List<String> answerList = new ArrayList<>();
    private static List<String> commentQuestionList = new ArrayList<>();
    private static List<String> commentAnswerList = new ArrayList<>();
    private static List<String> ratingList = new ArrayList<>();
    private static List<String> likeList = new ArrayList<>();
    // Numero di notifiche di ciascun tipo
    private static int questionCount = 0;
    private static int answerCount = 0;
    private static int commentQuestionCount = 0;
    private static int commentAnswerCount = 0;
    private static int ratingCount = 0;
    private static int likeCount = 0;
    private SharedPreferences prefs;
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
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
                                        // viene inizializzato il listener per sull'inserimento delle nuove domande
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

    // Metodo per scaricare in background l'immagine profilo di un utente e inviare successivamente una notifica
    // del tipo desiderato
    private class getBitmapFromUrl extends AsyncTask<String, Void, Bitmap>
    {
        private NotificationType type;
        private String questionKey;

        getBitmapFromUrl(NotificationType type, String questionKey)
        {
            this.type = type;
            this.questionKey = questionKey;
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
            sendNotification(type, result, questionKey);
        }
    }

    // Metodo per creare la notifica in base al tipo passato
    private void sendNotification(NotificationType type, Bitmap profilePic, String questionKey)
    {
        // Le notifiche vengono create solamente se è abilitata l'opzione in SettingsFragment (SwitchPreference)
        // e l'utente desidera essere informato di quel particolare evento (MultiSelectListPreference)
        if (prefs.getBoolean(SettingsFragment.KEY_PREF_NOTIF, getResources().getBoolean(R.bool.pref_notification_defVal)) && isNotificationEventSelected(type)) {
            Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
                    .setColor(Color.RED)
                    .setSmallIcon(R.drawable.ic_school_black_24dp)
                    .setPriority(getNotificationPriority())
                    //{Delay Iniziale, Durata Vibrazione 1, Pausa 1, ...}
                    .setVibrate(getNotificationVibration())
                    .setLights(getNotificationColor(), 800, 4000)
                    .setSound(getNotificationRingtone())
                    .setAutoCancel(true);

            switch (type) {
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

            Intent resultIntent = new Intent(BackgroundService.this, DetailActivity.class);
            resultIntent.putExtra("question_key", questionKey);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(DetailActivity.class);
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

    // Metodo per modificare la forma dell'immagine utente, rendendola circolare
    private Bitmap getCircleBitmap(Bitmap bitmap)
    {
        Bitmap output;
        Rect srcRect, dstRect;
        float r;
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        if (width > height)
        {
            output = Bitmap.createBitmap(height, height, Bitmap.Config.ARGB_8888);
            int left = (width - height) / 2;
            int right = left + height;
            srcRect = new Rect(left, 0, right, height);
            dstRect = new Rect(0, 0, height, height);
            r = height / 2;
        }
        else
        {
            output = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            int top = (height - width)/2;
            int bottom = top + width;
            srcRect = new Rect(0, top, width, bottom);
            dstRect = new Rect(0, 0, width, width);
            r = width / 2;
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
        for(String s : array)
            set.add(s);

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
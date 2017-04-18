package it.unibo.studio.unigo.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.MainActivity;
import it.unibo.studio.unigo.main.PostActivity;
import it.unibo.studio.unigo.utils.firebase.Question;
import it.unibo.studio.unigo.utils.firebase.User;

import static android.os.Build.VERSION_CODES.M;

// Servizio in background che viene fatto partire al boot del telefono o all'avvio dell'app, che recupera
// i cambiamenti da Firebase
public class BackgroundService extends Service
{
    private ServiceHandler mServiceHandler;
    // Identificativo del servizio in background
    private final int serviceId = 1;
    // ID che permette di aggiornare un particolare tipo di notifica
    private int notifyID = 1;
    private boolean avoidSyncEvent;
    public static boolean isRunning = false;

    // Handler per gestire i messaggi ricevuti
    private final class ServiceHandler extends Handler
    {
        private ServiceHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(notifyID, createNotification("Hello World!").build());

            /*
            try
            {
                // Updating the same notification if it's still alive
                Thread.sleep(10000);
                mNotificationManager.notify(notifyID, createNotification("Updating Notification 1").build());
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }*/
        }
    }

    // Avvio del servizio in background
    @Override
    public void onCreate()
    {
        Looper mServiceLooper;

        isRunning = true;
        //avoidSyncEvent = true;
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        // Inizializzazione dell'Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        retrieveUserInfo();
    }

    // Definizione dell'obiettivo del servizio (notificare i cambiamenti del database)
    // Viene richiamato dopo onCreate
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("PROVA", String.valueOf(startId));


        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy()
    {
        isRunning = false;
        avoidSyncEvent = true;
    }

    // Metodo per creare la notifica
    private NotificationCompat.Builder createNotification(String contentText)
    {

        Bitmap profilePic = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setColor(Color.CYAN)
                        .setSmallIcon(R.drawable.ic_home_black_24dp)
                        .setContentInfo("5") // Android 6 or below
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setVibrate(new long[] { 0, 300, 200, 300}) //{Initial Delay, Vibration Time 1, Delay, ...}
                        .setLargeIcon(profilePic)
                        .setLights(Color.MAGENTA, 200, 800)
                        .setContentTitle("My notification")
                        .setContentText(contentText);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(BackgroundService.this, PostActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        return mBuilder;
    }

    // Memorizzazione utente corrente per poter effettuare operazioni anche in modalità offline
    private void retrieveUserInfo()
    {
        if (Util.CURRENT_COURSE_KEY == null)
        {
            Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail()))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {
                            User u = dataSnapshot.getValue(User.class);
                            Util.CURRENT_COURSE_KEY = u.courseKey;
                            startQuestionListener();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });
        }
    }

    // Metodo che recupera gli id di tutte le domande del corso corrente.
    // Viene aggiunto un listener sulle domande recuperate per poter individuare le nuove domande/cambiamenti
    private void startQuestionListener()
    {
        // Listener sul campo "questions" della tabella Course per recuperare tutte le domande relative a quel corso
        // e per poter gestire gestire anche le domande che verranno inserite in futuro
        Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("questions").orderByValue().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Per ogni chiave del corso trovata, vengono recuperate le relative informazioni dalla tabella Question
                // e viene aggiunto un elemento nella RecyclerView
                Util.getDatabase().getReference("Question").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        addQuestionIntoList(dataSnapshot.getValue(Question.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });

                // Viene agganciata ad ogni domanda recuperata il listener che ne cattura gli eventuali cambiamenti
                addOnChangeListenerToQuestion(dataSnapshot.getKey());

                sendNotification("aaa");
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

    // Metodo per aggiungere alla RecyclerView la domanda passata come parametro
    private void addQuestionIntoList(final Question question)
    {
        // Viene recuperato l'utente che ha effettuato la domanda, in modo da caricare la sua foto profilo
        Util.getDatabase().getReference("User").child(question.user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Util.getQuestionList().add(0, new QuestionAdapterItem(question, dataSnapshot.getValue(User.class).photoUrl));
                if (Util.isHomeFragmentVisible())
                    Util.getHomeFragment().updateElement(0);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per agganciare ad una domanda il listener sui suoi cambiamenti
    private void addOnChangeListenerToQuestion(String question_key)
    {
        // Listener sui cambiamenti del post appena inserito (commenti, like, ...)
        Util.getDatabase().getReference("Question").child(question_key).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                sendNotification("bbb");
                //Toast.makeText(getActivity().getApplicationContext(), dataSnapshot.getKey() + ": " + dataSnapshot.getValue(String.class), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void sendNotification(String title)
    {
        Log.d("PROVA", "SyncEvent: " + avoidSyncEvent);

        if (!avoidSyncEvent)
        {
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = serviceId;
            mServiceHandler.sendMessage(msg);
        }
        else
            avoidSyncEvent = false;
    }
}
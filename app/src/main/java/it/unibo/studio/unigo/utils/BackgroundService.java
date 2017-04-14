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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.MainActivity;
import it.unibo.studio.unigo.main.PostActivity;

// Servizio in background che viene fatto partire al boot del telefono o all'avvio dell'app, che recupera
// i cambiamenti da Firebase
public class BackgroundService extends Service
{
    private ServiceHandler mServiceHandler;
    // ID che permette di aggiornare un particolare tipo di notifica
    int notifyID = 1;
    boolean avoidSyncEvent;

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

            try
            {
                // Updating the same notification if it's still alive
                Thread.sleep(10000);
                mNotificationManager.notify(notifyID, createNotification("Updating Notification 1").build());
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    // Avvio del servizio in background
    @Override
    public void onCreate()
    {
        Looper mServiceLooper;

        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        // Inizializzazione dell'Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    // Definizione dell'obiettivo del servizio (notificare i cambiamenti del database)
    // Viene richiamato dopo onCreate
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        DatabaseReference database;
        final int startId2 = startId;
        avoidSyncEvent = true;

        database = Util.getDatabase().getReference("Question");
        database.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (!avoidSyncEvent)
                {
                    // For each start request, send a message to start a job and deliver the
                    // start ID so we know which request we're stopping when we finish the job
                    Message msg = mServiceHandler.obtainMessage();
                    msg.arg1 = startId2;
                    mServiceHandler.sendMessage(msg);
                }
                else
                    avoidSyncEvent = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() { }

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
}
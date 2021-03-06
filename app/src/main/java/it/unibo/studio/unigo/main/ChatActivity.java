package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.MessageAdapter;
import it.unibo.studio.unigo.utils.BackgroundService;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.ChatRoom;
import it.unibo.studio.unigo.utils.firebase.Message;
import it.unibo.studio.unigo.utils.firebase.User;

public class ChatActivity extends AppCompatActivity
{
    private final int ID_1 = 1; // L'utente è 'id_1'
    private final int ID_2 = 2; // L'utente è 'id_2'

    private String recipientEmail;
    private User recipient;
    // Stringa che memorizza l'id della ChatRoom esistente tra due persone
    private String chatId;
    // Intero utilizzato per verificare se l'utente è 'id_1' o 'id_2' della chatRoom
    private int user_id = 0;
    // Intero utilizzato per avviare in maniera ritardata il listener sull'ultimo messaggio letto da parte del destinatario
    private int msgCounter = 0;
    // Boolean che indica se esiste la ChatRoom tra le persone in questione
    private boolean chatCreated = false;
    // Oggetto che consente la creazione di una nuova Chatroom
    private ChatRoom chatRoom;
    // Listener che permette l'aggiornamento della chat in tempo reale
    private ChildEventListener chatListener;
    // Listener che consente di mantenere aggiornato lo stato dell'ultimo messaggio letto
    // da parte del destinatario della conversazaione
    private ValueEventListener unreadMessageListener;
    private DatabaseReference userChatReference;
    private RecyclerView mRecyclerView;
    private MessageAdapter mAdapter;
    private Toolbar toolbar;
    private EditText txtMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initializeComponents();
        getRecipientUserInfo();
    }

    @Override
    protected  void onResume()
    {
        super.onResume();
        if (recipient != null)
        {
            BackgroundService.resetChatNotification(recipient.name + " " + recipient.lastName);
            Util.CURRENT_CHAT_KEY = recipientEmail;
        }
    }

    @Override
    protected void onPause()
    {
        BackgroundService.resetChatNotification(recipient.name + " " + recipient.lastName);
        Util.CURRENT_CHAT_KEY = "";
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").orderByKey().removeEventListener(chatListener);
        // Viene rimosso il listener che si occupa di recuperare il numero di messaggi non letti del destinatario,
        // utilizzato per mostrare un'icona sull'ultimo messaggio da lui letto
        userChatReference.removeEventListener(unreadMessageListener);
        userChatReference.keepSynced(false);
        super.onDestroy();
    }

    private void initializeComponents()
    {
        toolbar = (Toolbar) findViewById(R.id.chatToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.chatRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        ImageView chatSend = (ImageView) findViewById(R.id.chatSend);

        chatSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (!txtMessage.getText().toString().equals(""))
                {
                    // Se la ChatRoom esiste, viene semplicemente collegato il nuovo messaggio ad essa
                    if (chatCreated)
                        createMsg();
                    // Se la conversazione tra le due persone non è mai avvenuta, viene prima di tutto creata una nuova ChatRoom
                    else
                    {
                        // Creazione ChatRoom
                        Util.getDatabase().getReference("ChatRoom").child(chatId).setValue(chatRoom).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {
                                if (task.isSuccessful())
                                {
                                    chatCreated = true;
                                    // Collegamento delle persone alla ChatRoom
                                    Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("chat_rooms").child(chatId).setValue(true);
                                    Util.getDatabase().getReference("User").child(recipientEmail).child("chat_rooms").child(chatId).setValue(true);
                                    createMsg();
                                }
                            }
                        });
                    }
                }
            }
        });

        // Listener che permette di mantenere la chat e il numero di messaggi non letti aggiornati
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                mAdapter.addElement(dataSnapshot.getValue(Message.class), dataSnapshot.getKey());
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                Util.getDatabase().getReference("ChatRoom").child(chatId).child("msg_unread_" + user_id).setValue(0);

                /*
                    Avvio del listener su unread_msg_x in maniera ritardata:
                    *   Problema:
                    *    Se si avvia il listener dall'inizio, cercherà l'ultimo messaggio letto su una lista vuota,
                    *    e fino a nuova modifica del valore, non verrà più eseguito
                    *   Soluzione:
                    *    Quando viene aggiunto l'ultimo messaggio (i == childrencount, recuperato da una query),
                    *    avviare il listener
                */
                if (msgCounter > 0 )
                    msgCounter--;
                if (msgCounter == 0)
                {
                    msgCounter = -1;
                    userChatReference.addValueEventListener(unreadMessageListener);
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
        };

        // Listener per mantenere aggiornato lo stato dell'ultimo messaggio letto da parte del destinatario
        unreadMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Controllo obbligatorio in quanto la query viene eseguita *anche* quando il listener viene avviato,
                // quindi viene eseguita anche quando una conversazione tra due persone non esiste
                if (dataSnapshot.getValue() != null)
                    mAdapter.setLastMsgRead(dataSnapshot.getValue(Integer.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
    }

    // Metodo utilizzato per recuperare i dati del destinatario della conversazione
    private void getRecipientUserInfo()
    {
        recipientEmail = getIntent().getStringExtra("user_key");

        Util.getDatabase().getReference("User").child(recipientEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                recipient = dataSnapshot.getValue(User.class);
                BackgroundService.resetChatNotification(recipient.name + " " + recipient.lastName);
                Util.CURRENT_CHAT_KEY = recipientEmail;
                toolbar.setTitle(formatName(recipient.name, recipient.lastName));
                mAdapter = new MessageAdapter(recipient.photoUrl, recipient.name);
                mRecyclerView.setAdapter(mAdapter);
                getChat();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo utilizzato per recuperare l'eventuale conversazione esistente con il destinatario o,
    // nel caso di nuova conversazione, per creare il riferimento alla nuova chat
    private void getChat()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("chat_rooms").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Viene verificata l'esistenza di una corrispondenza del tipo 'mittente_destinatario'
                if (dataSnapshot.child(Util.encodeEmail(Util.getCurrentUser().getEmail()) + "_" + Util.encodeEmail(recipientEmail)).getValue() != null)
                {
                    chatId = dataSnapshot.child(Util.encodeEmail(Util.getCurrentUser().getEmail()) + "_" + Util.encodeEmail(recipientEmail)).getKey();
                    user_id = ID_1;
                    userChatReference = Util.getDatabase().getReference("ChatRoom").child(chatId).child("msg_unread_2");
                    chatCreated = true;
                }
                // In caso negativo, viene verificata l'esistenza dell'opposto 'destinatario_mittente'
                else if (dataSnapshot.child(Util.encodeEmail(recipientEmail) + "_" + (Util.encodeEmail(Util.getCurrentUser().getEmail()))).getValue() != null)
                {
                    chatId = dataSnapshot.child((Util.encodeEmail(recipientEmail)) + "_" + Util.encodeEmail(Util.getCurrentUser().getEmail())).getKey();
                    user_id = ID_2;
                    userChatReference = Util.getDatabase().getReference("ChatRoom").child(chatId).child("msg_unread_1");
                    chatCreated = true;
                }
                // Non esiste una chat tra le due persone, quindi viene inizializzato l'oggetto ChatRoom: se viene inviato almeno un messaggio,
                // ChatRoom verrà memorizzato effettivamente nel database, altrimenti verrà cancellato
                else
                {
                    // Viene prefissato l'id dell'utente che avvia una conversazione a *ID_1 (1)*
                    // L'identificativo della conversazione è composto da mail_id1_mail_id2, dove l'utente che la crea è sempre id_1
                    chatId = Util.encodeEmail(Util.getCurrentUser().getEmail()) + "_" + Util.encodeEmail(recipientEmail);
                    user_id = ID_1;
                    userChatReference = Util.getDatabase().getReference("ChatRoom").child(chatId).child("msg_unread_2");

                    chatRoom = new ChatRoom(Util.encodeEmail(Util.getCurrentUser().getEmail()),
                                                                Util.encodeEmail(recipientEmail),
                                                                Util.getCurrentUser().getDisplayName(),
                                                                recipient.name + " " + recipient.lastName,
                                                                Util.getCurrentUser().getPhotoUrl().toString(),
                                                                recipient.photoUrl);
                }

                getChatSize();

                userChatReference.keepSynced(true);
                // Recupero dei messaggi di una chat esistente e di mantenere la chat aggiornata in tempo reale
                Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").orderByKey().addChildEventListener(chatListener);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private String formatName(String name, String lastName)
    {
        return name + " " + String.valueOf(lastName.charAt(0)).toUpperCase() + ".";
    }

    // Metodo per l'inserimento di un messaggio nella ChatRoom +
    // Aggiornamento dell'ultimo messaggio presente nella conversazione (Testo e data) +
    // Aggiornamento dell'ultimo messaggio letto da parte del mittente
    private void createMsg()
    {
        final Message msg = new Message(Util.encodeEmail(Util.getCurrentUser().getEmail()), txtMessage.getText().toString(), Util.getDate());
        txtMessage.getText().clear();

        // Inserimento messaggio in Chat
        String msgKey = Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").push().getKey();
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").child(msgKey).setValue(msg);
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("last_message_id").setValue(msgKey);

        // Viene aggiornato il contatore di messaggi non letti
        userChatReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                userChatReference.setValue(dataSnapshot.getValue(Integer.class) + 1);

                // Aggiornamento dell'ultimo messaggio della ChatRoom
                Util.getDatabase().getReference("ChatRoom").child(chatId).child("last_message").setValue(msg.message);
                Util.getDatabase().getReference("ChatRoom").child(chatId).child("last_time").setValue(msg.date);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che recupera il numero di messaggi scambiati tra i due utenti (nel caso di conversazione già esistente),
    // utilizzato per la partenza ritardata del listener dell'ultimo messaggio letto
    private void getChatSize()
    {
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() != null)
                    msgCounter = (int) dataSnapshot.getChildrenCount();
                else msgCounter = 0;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}
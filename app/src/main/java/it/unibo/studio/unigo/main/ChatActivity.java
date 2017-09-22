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
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.MessageAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.ChatRoom;
import it.unibo.studio.unigo.utils.firebase.Message;
import it.unibo.studio.unigo.utils.firebase.User;

public class ChatActivity extends AppCompatActivity
{
    private final int ID_0 = 0; // Valore Default
    private final int ID_1 = 1; // L'utente è 'id_1'
    private final int ID_2 = 2; // L'utente è 'id_2'

    private DatabaseReference userChatReference;
    private String recipientEmail;
    private User recipient;
    // Stringa che memorizza l'id della ChatRoom esistente tra due persone
    private String chatId;
    // Intero utilizzato per verificare se l'utente è 'id_1' o 'id_2' della chatRoom
    private int user_id = 0;
    // Boolean che indica se esiste la ChatRoom tra le persone in questione
    private boolean chatCreated = false;
    // Oggetto che consente la creazione di una nuova Chatroom
    private ChatRoom chatRoom;
    // Listener che permette l'aggiornamento della chat in tempo reale
    private ChildEventListener chatListener;
    private List<Message> messageList;
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
        getUserInfo();
    }

    @Override
    protected void onDestroy()
    {
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").orderByKey().removeEventListener(chatListener);
        userChatReference.keepSynced(false);
        super.onDestroy();
    }

    private void initializeComponents()
    {
        messageList = new ArrayList<>();

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
        mLayoutManager.setReverseLayout(true);
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
                        chatCreated = true;
                        // Creazione ChatRoom
                        Util.getDatabase().getReference("ChatRoom").child(chatId).setValue(chatRoom).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {
                                if (task.isSuccessful())
                                {
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
                messageList.add(0, dataSnapshot.getValue(Message.class));
                mRecyclerView.smoothScrollToPosition(0);
                mAdapter.notifyItemInserted(0);
                Util.getDatabase().getReference("ChatRoom").child(chatId).child("msg_unread_" + user_id).setValue(0);
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
    }

    // Metodo utilizzato per recuperare i dati del destinatario della conversazione
    private void getUserInfo()
    {
        recipientEmail = getIntent().getStringExtra("user_key");

        Util.getDatabase().getReference("User").child(recipientEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                recipient = dataSnapshot.getValue(User.class);
                toolbar.setTitle(formatName(recipient.name, recipient.lastName));
                mAdapter = new MessageAdapter(messageList, recipient.photoUrl, recipient.name);
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
                    chatCreated = true;
                }
                // In caso negativo, viene verificata l'esistenza dell'opposto 'destinatario_mittente'
                else if (dataSnapshot.child(Util.encodeEmail(recipientEmail) + "_" + (Util.encodeEmail(Util.getCurrentUser().getEmail()))).getValue() != null)
                {
                    chatId = dataSnapshot.child((Util.encodeEmail(recipientEmail)) + "_" + Util.encodeEmail(Util.getCurrentUser().getEmail())).getKey();
                    chatCreated = true;
                }
                // Non esiste una chat tra le due persone, quindi viene inizializzato l'oggetto ChatRoom: se viene inviato almeno un messaggio, ChatRoom verrà
                // memorizzato effettivamente nel database, altrimenti verrà cancellato
                else
                {
                    chatId = Util.encodeEmail(Util.getCurrentUser().getEmail()) + "_" + Util.encodeEmail(recipientEmail);
                    // Viene prefissato l'id dell'utente che avvia una conversazione a ID_1 (1)
                    user_id = ID_1;
                    chatRoom = new ChatRoom(Util.encodeEmail(Util.getCurrentUser().getEmail()),
                                                                Util.encodeEmail(recipientEmail),
                                                                Util.getCurrentUser().getDisplayName(),
                                                                recipient.name + " " + recipient.lastName,
                                                                Util.getCurrentUser().getPhotoUrl().toString(),
                                                                recipient.photoUrl);
                }
                checkUserId();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che permette di capire se l'utente è colui che ha iniziato la chat (id_1) o colui che è stato contattato (id_2)
    private void checkUserId()
    {
        Util.getDatabase().getReference("ChatRoom").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Se l'ID è 0 (valore default) significa che è stata riscontrata una conversazione tra i due utenti,
                // quindi occorre stabilire se l'utente ha ID_1 o ID_2
                // In caso contrario, user_id è già stato impostato ad 1 quando la conversazione non esiste,
                // fissando il creatore della conversazione come 'id_1'.
                // Viene inoltre creato il riferimento al numero di messaggi non letti del destinatario
                if (user_id == ID_0)
                    if (dataSnapshot.child("id_1").getValue(String.class).equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                    {
                        user_id = ID_1;
                        userChatReference = Util.getDatabase().getReference("ChatRoom").child(chatId).child("msg_unread_2");
                    }
                    else
                    {
                        user_id = ID_2;
                        userChatReference = Util.getDatabase().getReference("ChatRoom").child(chatId).child("msg_unread_1");
                    }

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
        Message msg = new Message(Util.encodeEmail(Util.getCurrentUser().getEmail()), txtMessage.getText().toString(), Util.getDate());
        txtMessage.getText().clear();

        // Inserimento messaggio in Chat
        String msgKey = Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").push().getKey();
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("messages").child(msgKey).setValue(msg);

        // Viene aggiornato il contatore di messaggi non letti
        userChatReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                userChatReference.setValue(dataSnapshot.getValue(Integer.class) + 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Aggiornamento dell'ultimo messaggio della ChatRoom
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("last_message").setValue(msg.message);
        Util.getDatabase().getReference("ChatRoom").child(chatId).child("last_time").setValue(msg.date);
    }
}
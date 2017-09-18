package it.unibo.studio.unigo.main.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.ChatRoomAdapterItem;
import it.unibo.studio.unigo.main.adapters.ChatRoomAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.ChatRoom;

public class ChatRoomFragment extends android.support.v4.app.Fragment
{
    // Listener che permette di aggiungere una ChatRoom per ogni conversazione
    private ChildEventListener chatRoomCreationListener;
    // Lista contenente tutti i listener, utilizzata per disattivarli quando il fragment viene sostituito
    //private List<String> chatRoomActiveListener;
    private RecyclerView mRecyclerView;
    private LinearLayout wheel;
    private ChatRoomAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        initComponents(v);
        return v;
    }

    @Override
    public void onResume()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("chat_rooms").addChildEventListener(chatRoomCreationListener);
        super.onResume();
    }

    @Override
    public void onPause()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("chat_rooms").removeEventListener(chatRoomCreationListener);
        //for(String key: chatRoomActiveListener)
            //Util.getDatabase().getReference("ChatRoom").child(key).removeEventListener(chatRoomUpdateListener);
        super.onPause();
    }

    private void initComponents(View v)
    {
        // Lista di stringhe utilizzata per memorizzare tutte le DatabaseReference delle ChatRoom esistenti
        //chatRoomActiveListener = new ArrayList<>();

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewChat);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.item_divider));
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        wheel = (LinearLayout) v.findViewById(R.id.chatWheelLayout);

        // Inizializzazione adapter della lista delle domande
        mAdapter = new ChatRoomAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        // Listener che recupera tutte le conversazioni avvenute con l'utente
        chatRoomCreationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                // Quando viene aggiunta una voce in User/user@mail.com/chat_rooms, viene recuperata l'intera conversazione dalla tabella ChatRoom
                Util.getDatabase().getReference("ChatRoom").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (mAdapter.getPositionByKey(dataSnapshot.getKey()) == -1)
                        {
                            mAdapter.addElement(new ChatRoomAdapterItem(dataSnapshot.getValue(ChatRoom.class), dataSnapshot.getKey()));
                            addConversation(dataSnapshot.getKey());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });

                if (mRecyclerView.getVisibility() == View.GONE)
                    setRecyclerViewVisibility(true);
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

        // Se non sono presenti conversazioni con altri utenti, viene mostrato uno sfondo alternativo
        new CountDownTimer(3000, 3000)
        {
            public void onTick(long millisUntilFinished) { }

            public void onFinish()
            {
                if (mAdapter.getItemCount() == 0)
                    wheel.setVisibility(View.GONE);
            }
        }.start();
    }

    // Metodo che aggiunge un elemento ChatRoom alla recyclerView. Questo evento viene richiamato sia quando l'utente avvia una nuova
    // conversazione con un altro (A -> B), sia quando l'utente è il destinatario di una conversazione avviata da altri (B -> A).
    // Viene inoltre aggiunto un listener per ogni ChatRoom esistente, permettendo l'aggiornamento dell'ultimo messaggio inviato
    private void addConversation(final String chatKey)
    {
        ChildEventListener chatRoomUpdateListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // Se un evento fa scatenare 'onChildChanged', significa che un messaggio è stato aggiunto.
                // Al fine di evitare 'eventi multipli' (last_message, last_time, messages, msg_unread_1 e 2
                // scattano tutti allo stesso tempo), viene controllato solamente uno dei campi modificati
                if (dataSnapshot.getKey().equals("last_time"))
                    mAdapter.notifyItemChanged(mAdapter.getPositionByKey(chatKey), Util.NEW_MSG);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        // Viene memorizzato il riferimento al listener creato, in modo da poterlo disattivare una volta che il fragment non risulti più visibile
        //chatRoomActiveListener.add(chatKey);
        Util.getDatabase().getReference("ChatRoom").child(chatKey).addChildEventListener(chatRoomUpdateListener);
    }

    // Metodo utilizzato per nascondere/mostrare la recyclerview
    private void setRecyclerViewVisibility(boolean b)
    {
        if (b)
        {
            mRecyclerView.scrollToPosition(0);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        else
            mRecyclerView.setVisibility(View.GONE);
    }
}
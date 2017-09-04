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
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.ChatRoomAdapterItem;
import it.unibo.studio.unigo.main.adapters.ChatRoomAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.ChatRoom;

import static android.os.Build.VERSION_CODES.M;

public class ChatRoomFragment extends android.support.v4.app.Fragment
{
    public static final int CODE_NO_NEW_MESSAGE = 0;
    public static final int CODE_NEW_MESSAGE = 1;

    // Listener che permette di aggiungere una ChatRoom per ogni conversazione
    private ChildEventListener chatRoomCreationListener;
    // Listener utilizzato per aggiornare l'ultimo messaggio di ogni ChatRoom
    private ChildEventListener chatRoomUpdateListener;
    private List<ChatRoomAdapterItem> chatList;
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
        removeChatListener();
        super.onPause();
    }

    @Override
    public void onDestroyView()
    {
        //removeChatListener();
        super.onDestroyView();
    }

    private void initComponents(View v)
    {
        chatList = new ArrayList<>();

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewChat);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

        wheel = (LinearLayout) v.findViewById(R.id.chatWheelLayout);

        // Inizializzazione adapter della lista delle domande
        mAdapter = new ChatRoomAdapter(chatList, getActivity());
        mRecyclerView.setAdapter(mAdapter);

        chatRoomCreationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                addConversation(dataSnapshot.getKey());
                setRecyclerViewVisibility(true);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                if (dataSnapshot.getValue(Boolean.class))
                    mAdapter.notifyItemChanged(0, CODE_NEW_MESSAGE);
                else
                    mAdapter.notifyItemChanged(0, CODE_NO_NEW_MESSAGE);
            }

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
                if (chatList.isEmpty())
                    wheel.setVisibility(View.GONE);
            }
        }.start();

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.item_divider));
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        // Listener utilizzato per aggiornare l'ultimo messaggio di ogni ChatRoom
        chatRoomUpdateListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s)
            {
                //Todo: gestire i casi di aggiornamento di: last_message, last_time, last_read_1, last_read_2, messages (non utile)
                //Toast.makeText(getContext(), dataSnapshot.getKey(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };
    }

    // Metodo che aggiunge un elemento ChatRoom alla recyclerView. Questo evento viene richiamato sia quando l'utente avvia una nuova
    // conversazione con un altro (A -> B), sia quando l'utente è il destinatario di una conversazione avviata da altri (B -> A).
    // Viene inoltre aggiunto un listener per ogni ChatRoom esistente, permettendo l'aggiornamento dell'ultimo messaggio inviato
    private void addConversation(final String chatKey)
    {
        Util.getDatabase().getReference("ChatRoom").child(chatKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (!mAdapter.contains(chatKey))
                {
                    chatList.add(0, new ChatRoomAdapterItem(dataSnapshot.getValue(ChatRoom.class), dataSnapshot.getKey()));
                    mAdapter.notifyItemInserted(0);
                }
                else
                    mAdapter.updateChatRoom(dataSnapshot.getValue(ChatRoom.class), chatKey);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void removeChatListener()
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("chat_rooms").removeEventListener(chatRoomCreationListener);
        for(ChatRoomAdapterItem chat : chatList)
            Util.getDatabase().getReference("ChatRoom").child(chat.getChatKey()).removeEventListener(chatRoomUpdateListener);
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
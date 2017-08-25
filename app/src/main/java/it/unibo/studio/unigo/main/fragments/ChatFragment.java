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
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.ChatRoomAdapterItem;
import it.unibo.studio.unigo.main.adapters.ChatRoomAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Chat;

public class ChatFragment extends android.support.v4.app.Fragment
{
    public static final int CODE_NO_NEW_MESSAGE = 0;
    public static final int CODE_NEW_MESSAGE = 1;

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

    private void initComponents(View v)
    {
        chatList = new ArrayList<>();

        mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerViewChat);
        setRecyclerViewVisibility(false);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        mRecyclerView.setItemAnimator(null);

        wheel = (LinearLayout) v.findViewById(R.id.chatWheelLayout);

        // Inizializzazione adapter della lista delle domande
        mAdapter = new ChatRoomAdapter(chatList);
        mRecyclerView.setAdapter(mAdapter);

        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("chat_rooms").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s)
            {
                addChatListener(dataSnapshot.getKey());
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
        });

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
    }

    private void addChatListener(String chatKey)
    {
        Util.getDatabase().getReference("ChatRoom").child(chatKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                chatList.add(0, new ChatRoomAdapterItem(dataSnapshot.getValue(Chat.class), dataSnapshot.getKey()));
                mAdapter.notifyItemInserted(0);
                setRecyclerViewVisibility(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
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
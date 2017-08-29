package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.MessageAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Message;
import it.unibo.studio.unigo.utils.firebase.User;

public class ChatActivity extends AppCompatActivity
{
    String recipientEmail;
    List<Message> messageList;
    User recipient;
    RecyclerView mRecyclerView;
    MessageAdapter mAdapter;

    Toolbar toolbar;
    EditText txtMessage;
    ImageView chatSend;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initializeComponents();
        getUserInfo();
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

        mAdapter = new MessageAdapter(messageList);
        mRecyclerView.setAdapter(mAdapter);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        chatSend = (ImageView) findViewById(R.id.chatSend);
        chatSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Message msg = new Message(Util.getCurrentUser().getEmail(), txtMessage.getText().toString(), Util.getDate());
                messageList.add(0, msg);
                mAdapter.notifyItemInserted(0);

                Message msg2 = new Message("riccardo.lucchi3@gmail.com", "Pedrito ebreito", Util.getDate());
                messageList.add(0, msg2);
                mAdapter.notifyItemInserted(0);
            }
        });
    }

    private void getUserInfo()
    {
        recipientEmail = getIntent().getStringExtra("user_key");

        Util.getDatabase().getReference("User").child(recipientEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                recipient = dataSnapshot.getValue(User.class);
                toolbar.setTitle(formatName(recipient.name, recipient.lastName));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private String formatName(String name, String lastName)
    {
        return name + " " + String.valueOf(lastName.charAt(0)).toUpperCase() + ".";
    }
}
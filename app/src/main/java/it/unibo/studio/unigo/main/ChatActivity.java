package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class ChatActivity extends AppCompatActivity
{
    String recipientEmail;
    User recipient;
    Toolbar toolbar;

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
        toolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
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
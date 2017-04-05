package it.unibo.studio.unigo.main;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Error;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class PostActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener
{
    EditText etTitle, etCourse, etDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        overridePendingTransition(R.anim.activity_open_translate_from_bottom, R.anim.activity_no_animation);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        initComponents();
    }

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.activity_no_animation, R.anim.activity_close_translate_to_bottom);
        super.onPause();
    }

    // Listener sul pulsante di invio del nuovo post
    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        Error.Type error;
        switch (item.getItemId())
        {
            case R.id.post_toolbar_send:
                error = isValid();
                if (error == null)
                {
                    addPost();
                }
                else
                    errorHandler(error);
                break;
        }
        return true;
    }

    private void initComponents()
    {
        // Inizializzazione Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.PostToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.post_activity_toolbar);
        toolbar.setOnMenuItemClickListener(this);

        etTitle = (EditText) findViewById(R.id.etPostTitle);
        etCourse = (EditText) findViewById(R.id.etPostCourse);
        etDesc = (EditText) findViewById(R.id.etPostDesc);
    }

    // Controllo di validità dei campi:
    // - Esiste un errore --> viene ritornato il codice dell'errore
    // - Non vi sono errori --> viene ritornato nul
    private Error.Type isValid()
    {
        if (etTitle.getText().toString().equals(""))
            return Error.Type.TITLE_IS_EMPTY;
        if (etCourse.getText().toString().equals(""))
            return Error.Type.COURSE_IS_EMPTY;
        if (etDesc.getText().toString().equals(""))
            return Error.Type.DESC_IS_EMPTY;
        return null;
    }

    // Metodo che gestisce gli errori sui campi e mostra un alert dialog
    private void errorHandler(Error.Type e)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (e)
        {
            // Titolo vuoto
            case TITLE_IS_EMPTY:
                builder.setMessage(getString(R.string.error_post_title));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
            // Materia vuota
            case COURSE_IS_EMPTY:
                builder.setMessage(getString(R.string.error_post_course));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
            // Nessuna descrizione, viene mostrato l'errore come warning, ma il post è comunque valido
            case DESC_IS_EMPTY:
                builder.setMessage(getString(R.string.error_post_desc));
                builder.setPositiveButton(getString(R.string.alert_dialog_send),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                addPost();
                            }
                        });
                builder.setNegativeButton(getString(R.string.alert_dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
        }

        // Visualizzazione alertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Metodo per aggiungere al database la domanda appena compilata
    private void addPost()
    {
        Util.getDatabase().getReference("User").child(MainActivity.currentUser_key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        User u = dataSnapshot.getValue(User.class);

                        // Definire nella classe Util crediti partenza, crediti per postare, crediti in arrivo (like/ quando si effetuano commenti)
                        /* if (u.credits > Util.CreditiNecessari)
                        {
                            usare transazioni per scalare crediti e effettuare il post
                            runTransaction, capitolo read & write data
                        }*/

                        Toast.makeText(getApplicationContext(), u.email, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });

        finish();
    }
}
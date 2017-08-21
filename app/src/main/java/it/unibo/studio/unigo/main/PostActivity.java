package it.unibo.studio.unigo.main;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Error;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Question;
import it.unibo.studio.unigo.utils.firebase.User;

import static android.R.attr.name;

public class PostActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener
{
    private EditText etTitle, etCourse, etDesc;

    private MaterialDialog dialog;

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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        setResult(Activity.RESULT_CANCELED);
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
                    validatePost();
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

        dialog = new MaterialDialog.Builder(PostActivity.this)
                .content(R.string.alert_dialog_post_creation)
                .progress(true, 0)
                .cancelable(false)
                .build();
    }

    // Controllo di validità dei campi:
    // - Esiste un errore --> viene ritornato il codice dell'errore
    // - Non vi sono errori --> viene ritornato nul
    private Error.Type isValid()
    {
        if (Util.isNetworkAvailable(getApplicationContext()))
        {
            if (etTitle.getText().toString().equals(""))
                return Error.Type.TITLE_IS_EMPTY;
            if (etCourse.getText().toString().equals(""))
                return Error.Type.COURSE_IS_EMPTY;
            if (etDesc.getText().toString().equals(""))
                return Error.Type.DESC_IS_EMPTY;
            return null;
        }
        else
            return Error.Type.NETWORK_UNAVAILABLE;
    }

    // Metodo che gestisce gli errori sui campi e mostra un alert dialog
    private void errorHandler(Error.Type e)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (e)
        {
            // Connessione non disponibile
            case NETWORK_UNAVAILABLE:
                builder.setTitle(getString(R.string.alert_dialog_warning));
                builder.setMessage(getString(R.string.error_network_unavailable));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
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
                                validatePost();
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
            case NOT_ENOUGH_CREDITS:
                builder.setMessage(getString(R.string.error_not_enough_credits));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
        }

        // Visualizzazione alertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Metodo che verifica se l'utente ha crediti sufficienti per effettuare la domanda.
    // In caso positivo, la domanda viene inserita correttamente e vengono scalati i crediti dal profilo,
    // altrimenti viene restituito un errore
    private void validatePost()
    {
        dialog.show();
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail()))
            .runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData)
                {
                    User u = mutableData.getValue(User.class);
                    if (u == null)
                        return Transaction.success(mutableData);
                    // L'utente possiede crediti sufficienti per effettuare la domanda
                    if (u.credits >= Util.CREDITS_QUESTION)
                    {
                        u.credits -= Util.CREDITS_QUESTION;
                        mutableData.setValue(u);
                        return Transaction.success(mutableData);
                    }
                    // L'utente non possiede crediti sufficienti per effettuare la domanda
                    else
                        return Transaction.abort();
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean success, DataSnapshot dataSnapshot)
                {
                    if (success)
                        addPost();
                    else
                    {
                        dialog.dismiss();
                        errorHandler(Error.Type.NOT_ENOUGH_CREDITS);
                    }
                }
            });
    }

    // Metodo per aggiungere al database la domanda appena compilata e collegarla all'utente
    private void addPost()
    {
        final String key = Util.getDatabase().getReference("Question").push().getKey();
        final Question question = new Question(formatString(etTitle.getText().toString()), formatString(etCourse.getText().toString()), formatString(etDesc.getText().toString()),
                                  Util.encodeEmail(Util.getCurrentUser().getEmail()), Util.CURRENT_COURSE_KEY);
        Util.getDatabase().getReference("Question").child(key).setValue(question).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                linkPostToCourse(key, question.date);
                linkPostToUser(key);
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), R.string.toast_post_sent, Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    // Metodo per collegare la domanda appena creata all'utente che l'ha effettuata
    private void linkPostToUser(String question_key)
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("questions").child(question_key).setValue(true);
    }

    private void linkPostToCourse(String question_key, String date)
    {
        Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("questions").child(question_key).setValue(date);
    }

    // Metodo che restituisce la stringa presa in ingresso, con il primo carattere in maiuscolo
    private String formatString(String string)
    {
        return (string.length() > 0) ? string.substring(0,1).toUpperCase() + string.substring(1) : "";
    }
}
package it.unibo.studio.unigo.main;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.SurveyNewChoiceAdapter;
import it.unibo.studio.unigo.utils.Error;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Survey;

public class NewSurveyActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener
{
    private EditText etTitle, etDesc;
    private SurveyNewChoiceAdapter surveyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_survey);
        initComponents();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        if (item.getItemId() == R.id.post_toolbar_send)
        {
            Error.Type error = isValid();
            if (error == null)
                sendNewSurvey();
            else
                errorHandler(error);
        }
        return true;
    }

    private void initComponents()
    {
        // Inizializzazione Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarSurvey);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.toolbar_newpost);
        toolbar.setOnMenuItemClickListener(this);

        etTitle = (EditText) findViewById(R.id.etSurvTitle);
        etDesc = (EditText) findViewById(R.id.etSurvDesc);
        RecyclerView rvChoice = (RecyclerView) findViewById(R.id.rvNewSurv);
        rvChoice.setHasFixedSize(true);
        rvChoice.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        surveyAdapter = new SurveyNewChoiceAdapter();
        rvChoice.setAdapter(surveyAdapter);

        RelativeLayout addChoiceLayout = (RelativeLayout) findViewById(R.id.rlSurvAddChoice);
        ImageView btnSurvAddChoice = (ImageView) findViewById(R.id.btnSurvAddChoice);
        View.OnClickListener addChoiceListener = new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                view.clearFocus();
                openNewChoiceDialog();
            }
        };
        addChoiceLayout.setOnClickListener(addChoiceListener);
        btnSurvAddChoice.setOnClickListener(addChoiceListener);
    }

    // Dialog per l'inserimento di una nuova opzione per il sondaggio (l'opzione non deve essere vuota)
    private void openNewChoiceDialog()
    {
        View v = View.inflate(getApplicationContext(), R.layout.dialog_new_survey_choice, null);
        final EditText txt = (EditText) v.findViewById(R.id.etSurvDialog);

        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.survey_add)
                .customView(v, true)
                .cancelable(true)
                .positiveText(getString(R.string.alert_dialog_confirm))
                .negativeText(getString(R.string.alert_dialog_cancel))
                .positiveColor(ContextCompat.getColor(this, R.color.colorAccent))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
                    {
                        surveyAdapter.addElement(txt.getText().toString());
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();
        dialog.show();

        txt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (txt.getText().toString().equals(""))
                    dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                else
                    dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                return false;
            }
        });
        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
    }

    // Metodo per salvare un sondaggio online
    private void sendNewSurvey()
    {
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(R.string.survey_dialog_sending)
                .progress(true, 0)
                .cancelable(false)
                .build();
        dialog.show();

        final String key = Util.getDatabase().getReference("Survey").push().getKey();
        final Survey survey = new Survey(etTitle.getText().toString(), etDesc.getText().toString(), Util.encodeEmail(Util.getCurrentUser().getEmail()), Util.CURRENT_COURSE_KEY);

        // Creazione sondaggio. Una volta creato, vengono aggiunte tutte le opzioni desiderate
        Util.getDatabase().getReference("Survey").child(key).setValue(survey).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                List<String> choiceList = surveyAdapter.getChoiceList();
                for(String option : choiceList)
                    Util.getDatabase().getReference("Survey").child(key).child("choices").child(option).child("empty").setValue(true);

                Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("surveys").child(key).setValue(true);
                Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("surveys").child(key).setValue(survey.date);

                dialog.dismiss();
                Toast.makeText(getApplicationContext(), R.string.survey_toast_sent, Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    // Controllo di validità dei campi:
    // - Esiste un errore --> viene ritornato il codice dell'errore
    // - Non vi sono errori --> viene ritornato null
    private Error.Type isValid()
    {
        if (Util.isNetworkAvailable(getApplicationContext()))
        {
            if (etTitle.getText().toString().equals(""))
                return Error.Type.TITLE_IS_EMPTY;
            if (surveyAdapter.getItemCount() < 2)
                return Error.Type.NOT_ENOUGH_SURVEY_CHOICES;
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
            // Nessuna descrizione, viene mostrato l'errore come warning, ma il post è comunque valido
            case DESC_IS_EMPTY:
                builder.setMessage(getString(R.string.error_post_desc));
                builder.setPositiveButton(getString(R.string.alert_dialog_send),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                sendNewSurvey();
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
            case NOT_ENOUGH_SURVEY_CHOICES:
                builder.setMessage(getString(R.string.survey_dialog_more_choices_needed));
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
}
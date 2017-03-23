package it.unibo.studio.unigo.signup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;
import com.stepstone.stepper.BlockingStep;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Error;
import it.unibo.studio.unigo.utils.SignupData;

import static it.unibo.studio.unigo.utils.Error.resetError;

public class Step1Fragment extends Fragment implements BlockingStep
{
    private boolean isValid;
    private FirebaseAuth mAuth;
    private MaterialDialog dialog;
    private TextInputLayout inRegEmail, inRegPass, inRegPassConfirm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_step1, container, false);
        initializeComponents(v);

        mAuth = FirebaseAuth.getInstance();

        return v;
    }

    // Ritornare null per procedere allo step successivo, VerificationErrore(string) altrimenti
    @Override
    public VerificationError verifyStep()
    {
        return null;
    }

    // Metodo richiamato al click del pulsante Next
    // Se i campi sono stati compilati correttamente si procede allo step successivo, altrimenti viene visualizzato
    // un errore
    @Override
    public void onNextClicked(final StepperLayout.OnNextClickedCallback callback)
    {
        // Variabile che indica la validità dei campi
        isValid = true;

        // Se vi sono degli errori sui campi password, is valid viene settato a false
        validatePassword();
        // Se il campo email è vuoto, is valid viene settato a false
        if (inRegEmail.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.EMAIL_IS_EMPTY);
            isValid = false;
            callback.getStepperLayout().updateErrorState(true);
        }
        // Controllo di validità sul campo email
        else
        {
            dialog.show();
            mAuth.fetchProvidersForEmail(inRegEmail.getEditText().getText().toString()).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                    if(task.isSuccessful())
                    {
                        // Se get providers contiene 1 solo elemento, la mail inserita è già stata utilizzata
                        // e is valid viene settato a false
                        if (task.getResult().getProviders().size() == 1)
                        {
                            errorHandler(Error.Type.EMAIL_ALREADY_IN_USE);
                            isValid = false;
                        }

                        // Se la mail inserita è corretta e non è gia in uso e i campi password sono stati
                        // compilati correttamente, vengono memorizzate le informazioni relative all'account
                        // e si procede allo step successivo
                        if (isValid)
                        {
                            SignupData.setEmail(inRegEmail.getEditText().getText().toString());
                            SignupData.setPassword(inRegPass.getEditText().getText().toString());
                            callback.goToNextStep();
                        }
                        else
                            callback.getStepperLayout().updateErrorState(true);
                    }
                    // Se viene generata un eccezione, significa che la mail inserita non è valida
                    // e is valid viene settato a false
                    else
                    {
                        errorHandler(Error.Type.EMAIL_INVALID);
                        isValid = false;
                        callback.getStepperLayout().updateErrorState(true);
                    }
                    dialog.dismiss();
                }
            });
        }
    }

    @Override
    public void onSelected(){ }

    @Override
    public void onError(@NonNull VerificationError error) { }

    @Override
    public void onCompleteClicked(StepperLayout.OnCompleteClickedCallback callback) { }

    @Override
    public void onBackClicked(StepperLayout.OnBackClickedCallback callback) { }

    // Inizializzazione dei componenti
    private void initializeComponents(View v)
    {
        inRegEmail = (TextInputLayout) v.findViewById(R.id.inRegEmail);
        inRegPass = (TextInputLayout) v.findViewById(R.id.inRegPass);
        inRegPassConfirm = (TextInputLayout) v.findViewById(R.id.inRegPassConfirm);

        // Se i campi sono già stati compilati correttamente in precedenza,
        // verranno caricati negli appositi spazi
        if (SignupData.getEmail() != null)
            inRegEmail.getEditText().setText(SignupData.getEmail());
        if (SignupData.getPassword()!= null)
        {
            inRegPass.getEditText().setText(SignupData.getPassword());
            inRegPassConfirm.getEditText().setText(SignupData.getPassword());
        }

        inRegEmail.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inRegEmail.isErrorEnabled())
                    resetError(inRegEmail);
                return false;
            }
        });

        inRegPass.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (inRegPass.isErrorEnabled())
                    resetError(inRegPass);
                return false;
            }
        });
        inRegPassConfirm.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (inRegPassConfirm.isErrorEnabled())
                    resetError(inRegPassConfirm);
                return false;
            }
        });

        dialog = new MaterialDialog.Builder(getContext())
                .title(getResources().getString(R.string.alert_dialog_step1_title))
                .content(getResources().getString(R.string.alert_dialog_step1_content))
                .progress(true, 0)
                .build();
    }

    // Controllo validità dei campi password
    private void validatePassword()
    {
        // Password vuota
        if (inRegPass.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.PASSWORD_IS_EMPTY);
            isValid = false;
        }
        if (inRegPass.getEditText().getText().toString().length() < 6)
        {
            errorHandler(Error.Type.PASSWORD_WEAK);
            isValid = false;
        }
        // Conferma password vuota
        if (inRegPassConfirm.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.PASSWORD_CONFIRM_IS_EMPTY);
            isValid = false;
        }
        // Le password non coincidono
        if (!inRegPass.getEditText().getText().toString().equals(inRegPassConfirm.getEditText().getText().toString()))
        {
            errorHandler(Error.Type.PASSWORD_MISMATCH);
            isValid = false;
        }
    }

    // Metodo per evidenziare gli errori nella GUI
    private void errorHandler(Error.Type e)
    {
        switch (e)
        {
            case EMAIL_IS_EMPTY:
                inRegEmail.setErrorEnabled(true);
                inRegEmail.setError(getResources().getString(R.string.error_email_is_empty));
                break;
            case EMAIL_ALREADY_IN_USE:
                inRegEmail.setErrorEnabled(true);
                inRegEmail.setError(getResources().getString(R.string.error_email_already_in_use));
                break;
            case EMAIL_INVALID:
                inRegEmail.setErrorEnabled(true);
                inRegEmail.setError(getResources().getString(R.string.error_email_invalid));
                break;
            case PASSWORD_IS_EMPTY:
                inRegPass.setErrorEnabled(true);
                inRegPass.setError(getResources().getString(R.string.error_password_is_empty));
                break;
            case PASSWORD_WEAK:
                inRegPass.setErrorEnabled(true);
                inRegPass.setError(getResources().getString(R.string.error_password_is_weak));
                break;
            case PASSWORD_CONFIRM_IS_EMPTY:
                inRegPassConfirm.setErrorEnabled(true);
                inRegPassConfirm.setError(getResources().getString(R.string.error_password_is_empty));
                break;
            case PASSWORD_MISMATCH:
                inRegPassConfirm.setErrorEnabled(true);
                inRegPassConfirm.setError(getResources().getString(R.string.error_password_mismatch));
                break;
        }
    }
}
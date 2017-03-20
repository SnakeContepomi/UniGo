package it.unibo.studio.unigo.signup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ProviderQueryResult;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Error;

import static it.unibo.studio.unigo.utils.Error.resetError;


public class Step1Fragment extends Fragment implements Step
{
    private FirebaseAuth mAuth;
    private boolean isValid;
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

    //return null if the user can go to the next step, create a new VerificationError instance otherwise
    @Override
    public VerificationError verifyStep()
    {
        if (isValid())
            return null;
        else
            return new VerificationError(getResources().getString(R.string.error_step_1));
    }

    @Override
    public void onSelected() {
        //update UI when selected
    }

    @Override
    public void onError(@NonNull VerificationError error) {
        //handle error inside of the fragment, e.g. show error on EditText
    }

    private void initializeComponents(View v)
    {
        inRegEmail = (TextInputLayout) v.findViewById(R.id.inRegEmail);
        inRegPass = (TextInputLayout) v.findViewById(R.id.inRegPass);
        inRegPassConfirm = (TextInputLayout) v.findViewById(R.id.inRegPassConfirm);

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
                .title("Titolo")
                .content("Attendere")
                .progress(true, 0)
                .build();
    }

    // Controllo validità dei campi
    private boolean isValid()
    {
        isValid = true;
        dialog.show();

        // Controllo campo EMAIL
        if (inRegEmail.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.EMAIL_IS_EMPTY);
            isValid = false;
        }
        else
            mAuth.fetchProvidersForEmail(inRegEmail.getEditText().getText().toString()).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                    if(task.isSuccessful()){
                        ///////// getProviders() will return size 1. if email ID is available.
                        if (task.getResult().getProviders().size() == 1)
                        {
                            errorHandler(Error.Type.EMAIL_ALREADY_IN_USE);
                            isValid = false;
                        }
                    }
                    else
                    {
                        errorHandler(Error.Type.EMAIL_INVALID);
                        isValid = false;
                    }
                    dialog.dismiss();
                }
            });

        // Controllo campi PASSWORD
        if (inRegPass.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.PASSWORD_IS_EMPTY);
            isValid = false;
        }
        if (inRegPassConfirm.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.PASSWORD_CONFIRM_IS_EMPTY);
            isValid = false;
        }
        if (!inRegPass.getEditText().getText().toString().equals(inRegPassConfirm.getEditText().getText().toString()))
        {
            errorHandler(Error.Type.PASSWORD_MISMATCH);
            isValid = false;
        }

        return isValid;
    }

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
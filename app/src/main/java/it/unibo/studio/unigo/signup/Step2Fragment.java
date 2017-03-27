package it.unibo.studio.unigo.signup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Error;
import it.unibo.studio.unigo.utils.SignupData;

import static it.unibo.studio.unigo.utils.Error.resetError;

public class Step2Fragment extends Fragment implements Step
{
    private TextInputLayout inRegName, inRegLastName, inRegPhone, inRegCity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_step2, container, false);
        initializeComponents(v);

        return v;
    }

    // Metodo richiamato al click del pulsante Next
    // Se i campi sono stati compilati correttamente si procede allo step successivo,
    // altrimenti viene visualizzato un errore
    @Override
    public VerificationError verifyStep()
    {
        if (validateInfo())
        {
            // Nome e cognome vengono memorizzati con la prima letera maiuscola
            String name, last_name;
            name = inRegName.getEditText().getText().toString();
            name = name.substring(0,1).toUpperCase() + name.substring(1).toLowerCase();
            last_name = inRegLastName.getEditText().getText().toString();
            last_name = last_name.substring(0,1).toUpperCase() + last_name.substring(1).toLowerCase();

            // Memorizzazione temporanea delle informazioni inserite
            SignupData.setName(name);
            SignupData.setLastName(last_name);
            SignupData.setPhone(inRegPhone.getEditText().getText().toString());
            SignupData.setCity(inRegCity.getEditText().getText().toString());
            return null;
        }
        else
            return new VerificationError(getResources().getString(R.string.error_step_2));
    }

    @Override
    public void onSelected()
    {
        loadData();
    }

    @Override
    public void onError(@NonNull VerificationError error) { }

    // Inizializzazione componenti
    private void initializeComponents(View v)
    {
        inRegName = (TextInputLayout) v.findViewById(R.id.inRegName);
        inRegLastName = (TextInputLayout) v.findViewById(R.id.inRegLastName);
        inRegPhone = (TextInputLayout) v.findViewById(R.id.inRegPhone);
        inRegCity = (TextInputLayout) v.findViewById(R.id.inRegCity);

        loadData();

        inRegName.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inRegName.isErrorEnabled())
                    resetError(inRegName);
                return false;
            }
        });
        inRegLastName.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inRegLastName.isErrorEnabled())
                    resetError(inRegLastName);
                return false;
            }
        });
        inRegPhone.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inRegPhone.isErrorEnabled())
                    resetError(inRegPhone);
                return false;
            }
        });
        inRegCity.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inRegCity.isErrorEnabled())
                    resetError(inRegCity);
                return false;
            }
        });
    }

    // Controllo validità campi
    private boolean validateInfo()
    {
        boolean isValid = true;

        // Campo nome vuoto
        if (inRegName.getEditText().getText().toString().equals(""))
        {
            isValid = false;
            errorHandler(Error.Type.NAME_IS_EMPTY);
        }
        // Campo cognome vuoto
        if (inRegLastName.getEditText().getText().toString().equals(""))
        {
            isValid = false;
            errorHandler(Error.Type.LASTNAME_IS_EMPTY);
        }
        // Campo numero di telefono errato (lunghezza consentita 7-10 caratteri)
        if (((inRegPhone.getEditText().getText().toString().length() < 7) ||
             (inRegPhone.getEditText().getText().toString().length() > 10)) &&
                (!inRegPhone.getEditText().getText().toString().equals("")))
        {
            isValid = false;
            errorHandler(Error.Type.PHONE_INCORRECT);
        }

        return isValid;
    }

    // Metodo per evidenziare gli errori nella GUI
    private void errorHandler(Error.Type e)
    {
        switch (e)
        {
            case NAME_IS_EMPTY:
                inRegName.setErrorEnabled(true);
                inRegName.setError(getResources().getString(R.string.error_name_is_empty));
                break;
            case LASTNAME_IS_EMPTY:
                inRegLastName.setErrorEnabled(true);
                inRegLastName.setError(getResources().getString(R.string.error_lastname_is_empty));
                break;
            case PHONE_INCORRECT:
                inRegPhone.setErrorEnabled(true);
                inRegPhone.setError(getResources().getString(R.string.error_phone));
                break;
        }
    }

    // Metodo per caricare i dati precedentemente compilati (se esistono) negli opportuni campi
    private void loadData()
    {
        // Se i campi sono già stati compilati correttamente in precedenza,
        // verranno caricati negli appositi spazi
        if (SignupData.getName() != null)
            inRegName.getEditText().setText(SignupData.getName());
        if (SignupData.getLastName() != null)
            inRegLastName.getEditText().setText(SignupData.getLastName());
        if (SignupData.getPhone() != null)
            inRegPhone.getEditText().setText(SignupData.getPhone());
        if (SignupData.getCity() != null)
            inRegCity.getEditText().setText(SignupData.getCity());
    }
}
package it.unibo.studio.unigo.signup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Error;

import static android.os.Build.VERSION_CODES.M;

public class Step1Fragment extends Fragment implements Step
{
    private TextInputLayout inRegEmail, inRegPass, inRegPassConfirm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_step1, container, false);
        initializeComponents(v);

        return v;
    }

    @Override
    public VerificationError verifyStep() {
        //return null if the user can go to the next step, create a new VerificationError instance otherwise
        Error.Type e = isValid();
        switch (e)
        {
            case EMAIL:
                return new VerificationError("1");
            case PASS:
                return new VerificationError("2");
            case PASS_MISMATCH:
                return new VerificationError("3");
        }
        return null;
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
    }

    // Return true if no error occurs, false otherwise
    private Error.Type isValid()
    {

        return null;
    }
}
package it.unibo.studio.unigo.signup;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;

import it.unibo.studio.unigo.R;

import static android.R.attr.enabled;

public class SignupActivity extends AppCompatActivity implements StepperLayout.StepperListener
{
    private StepperLayout mStepperLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mStepperLayout = (StepperLayout) findViewById(R.id.stepperLayout);
        mStepperLayout.setAdapter(new StepAdapter(getSupportFragmentManager(), this));
        mStepperLayout.setListener(this);

       // mStepperLayout.setNextButtonVerificationFailed(true);
        //mStepperLayout.setCompleteButtonVerificationFailed(!enabled);
    }

    @Override
    public void onCompleted(View completeButton) {
        Toast.makeText(this, "onCompleted!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(VerificationError verificationError) {
        Toast.makeText(this, "onError! -> " + verificationError.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStepSelected(int newStepPosition) {
        Toast.makeText(this, "onStepSelected! -> " + newStepPosition, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReturn() {
        finish();
    }
}
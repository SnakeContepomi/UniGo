package it.unibo.studio.unigo.signup;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.stepstone.stepper.StepperLayout;
import it.unibo.studio.unigo.R;

public class SignupActivity extends AppCompatActivity
{
    private StepperLayout mStepperLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mStepperLayout = (StepperLayout) findViewById(R.id.stepperLayout);
        mStepperLayout.setAdapter(new StepAdapter(getSupportFragmentManager(), this));
        //mStepperLayout.setListener(this);
    }

    @Override
    public void onBackPressed()
    {
        if (mStepperLayout.getCurrentStepPosition() > 0)
            mStepperLayout.setCurrentStepPosition(mStepperLayout.getCurrentStepPosition()-1);
        else
        {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }
    }
}
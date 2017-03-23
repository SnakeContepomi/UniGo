package it.unibo.studio.unigo.signup;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork;
import com.stepstone.stepper.StepperLayout;
import it.unibo.studio.unigo.R;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

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

        ReactiveNetwork.observeInternetConnectivity()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override public void call(Boolean isConnectedToInternet) {
                        if (!isConnectedToInternet)
                        {
                            Snackbar
                                .make(findViewById(R.id.l_signup), R.string.snackbar_no_internet_connection, Snackbar.LENGTH_LONG)
                                .show();
                        }
                    }
                });
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
package it.unibo.studio.unigo.signup;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
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

        // Inizializzazione Toolbar
        Toolbar mActionBar = (Toolbar) findViewById(R.id.toolbarSignup);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        // Inizializzazione dello stepper di registrazione
        mStepperLayout = (StepperLayout) findViewById(R.id.stepperLayout);
        mStepperLayout.setAdapter(new StepAdapter(getSupportFragmentManager(), this));

        // Inizializzazione del listener che monitora lo stato della connessione
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
            finish();
    }
}
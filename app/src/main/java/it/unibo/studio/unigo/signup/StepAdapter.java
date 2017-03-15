package it.unibo.studio.unigo.signup;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter;
import com.stepstone.stepper.viewmodel.StepViewModel;

import it.unibo.studio.unigo.R;

public class StepAdapter extends AbstractFragmentStepAdapter
{
    private final String CURRENT_STEP_POSITION_KEY = "position";

    public StepAdapter(FragmentManager fm, Context context)
    {
        super(fm, context);
    }

    @Override
    public Step createStep(int position) {
        final Step1Fragment step = new Step1Fragment();
        Bundle b = new Bundle();
        b.putInt(CURRENT_STEP_POSITION_KEY, position);
        step.setArguments(b);
        return step;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @NonNull
    @Override
    public StepViewModel getViewModel(@IntRange(from = 0) int position) {
        //Override this method to set Step title for the Tabs, not necessary for other stepper types
        return new StepViewModel.Builder(context)
                .setTitle(R.string.app_name) //can be a CharSequence instead
                .create();
    }
}
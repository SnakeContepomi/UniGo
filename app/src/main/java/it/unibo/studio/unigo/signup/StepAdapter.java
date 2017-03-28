package it.unibo.studio.unigo.signup;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter;
import com.stepstone.stepper.viewmodel.StepViewModel;
import it.unibo.studio.unigo.R;

public class StepAdapter extends AbstractFragmentStepAdapter
{
    public StepAdapter(FragmentManager fm, Context context)
    {
        super(fm, context);
    }

    @Override
    public Step createStep(int position)
    {
       switch(position)
       {
           case 0:
               return new Step1Fragment();
           case 1:
               return new Step2Fragment();
           case 2:
               return new Step3Fragment();
           default:
               throw new IllegalArgumentException("Errore");
       }
    }

    @Override
    public int getCount()
    {
        return 3;
    }

    @NonNull
    @Override
    public StepViewModel getViewModel(@IntRange(from = 0) int position)
    {
        StepViewModel.Builder builder = new StepViewModel.Builder(context);

        switch (position)
        {
            case 0:
                builder.setTitle(R.string.step1_title);
                break;
            case 1:
                builder.setTitle(R.string.step2_title);
                break;
            case 2:
                builder.setTitle(R.string.step3_title);
                break;
        }
        return builder.create();
    }
}
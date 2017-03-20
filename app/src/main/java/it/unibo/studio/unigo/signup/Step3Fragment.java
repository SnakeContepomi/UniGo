package it.unibo.studio.unigo.signup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import it.unibo.studio.unigo.R;

public class Step3Fragment extends Fragment implements Step
{

    private String[] REGIONS;

    private MaterialBetterSpinner spRegion, spUni, spSchool;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_step3, container, false);

        initializeComponents(v);

        return v;
    }

    @Override
    public VerificationError verifyStep() {
        //return null if the user can go to the next step, create a new VerificationError instance otherwise
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
        REGIONS = getResources().getStringArray(R.array.regions);

        ArrayAdapter<String> arrayAdapterRegion = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, REGIONS);
        spRegion = (MaterialBetterSpinner) v.findViewById(R.id.spinner_region);
        spRegion.setAdapter(arrayAdapterRegion);

        ArrayAdapter<String> arrayAdapterUni = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, REGIONS);
        spRegion = (MaterialBetterSpinner) v.findViewById(R.id.spinner_uni);
        spRegion.setAdapter(arrayAdapterUni);

        ArrayAdapter<String> arrayAdapterSchool = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, REGIONS);
        spRegion = (MaterialBetterSpinner) v.findViewById(R.id.spinner_school);
        spRegion.setAdapter(arrayAdapterSchool);
    }

}
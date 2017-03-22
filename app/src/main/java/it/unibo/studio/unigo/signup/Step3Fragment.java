package it.unibo.studio.unigo.signup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.School;
import it.unibo.studio.unigo.utils.University;

public class Step3Fragment extends Fragment implements Step
{
    List<String> UNI, SCHOOL;
    HashMap<Integer, String> uniKeys, schoolKeys;

    private DatabaseReference database;

    private MaterialBetterSpinner  spRegion, spUni, spSchool;
    ArrayAdapter<String> spinnerAdapter;

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
        String[] REGIONS = getResources().getStringArray(R.array.regions);

        UNI = new ArrayList<>();
        uniKeys = new HashMap<>();
        SCHOOL = new ArrayList<>();
        schoolKeys = new HashMap<>();

        database = FirebaseDatabase.getInstance().getReference();

        spRegion = (MaterialBetterSpinner ) v.findViewById(R.id.spinner_region);
        spUni = (MaterialBetterSpinner ) v.findViewById(R.id.spinner_uni);
        spSchool = (MaterialBetterSpinner) v.findViewById(R.id.spinner_school);

        spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, REGIONS);
        spRegion.setAdapter(spinnerAdapter);
        spRegion.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                spUni.setEnabled(true);
                spUni.setClickable(true);
                spSchool.setEnabled(false);
                spSchool.setClickable(false);
                spUni.getText().clear();
                spSchool.getText().clear();
                refreshUni();
            }
        });

        spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, UNI);
        spUni.setAdapter(spinnerAdapter);
        spUni.setEnabled(false);
        spUni.setClickable(false);
        spUni.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                spSchool.setEnabled(true);
                spSchool.setClickable(true);
                String uni_name = UNI.get(i);
                String uni_key = uniKeys.get(i);
                spSchool.getText().clear();
                refreshSchool(uni_key);
            }
        });

        spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, SCHOOL);
        spSchool.setAdapter(spinnerAdapter);
        spSchool.setEnabled(false);
        spSchool.setClickable(false);
        spSchool.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String school_name = SCHOOL.get(i);
                String school_key = schoolKeys.get(i);
                Toast.makeText(getContext(), "name: " + school_name + " key: " + school_key, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshUni()
    {
        database.child("University").orderByChild("region").equalTo(spRegion.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Toast.makeText(getContext(),String.valueOf(UNI.size()) + String.valueOf(uniKeys.size()) , Toast.LENGTH_SHORT).show();
                int i = 0;

                //Toast.makeText(getApplicationContext(), String.valueOf(snapshot.getChildrenCount()) , Toast.LENGTH_SHORT).show();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    University uni = child.getValue(University.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave dell'università corrente
                    // In posizione i della mappa delle chiavi viene memorizzato il nome dell'università corrente
                    uniKeys.put(i, child.getKey());
                    UNI.add(i, uni.name);
                    i++;
                }
                // Vengono visualizzati nello spUni i nomi presenti nell'array di nomi (UNI)
                spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, UNI);
                spinnerAdapter.notifyDataSetChanged();
                spUni.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void refreshSchool(String key)
    {
        database.child("School").orderByChild("university_key").equalTo(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Toast.makeText(getContext(),String.valueOf(UNI.size()) + String.valueOf(uniKeys.size()) , Toast.LENGTH_SHORT).show();
                int i = 0;

                //Toast.makeText(getApplicationContext(), String.valueOf(snapshot.getChildrenCount()) , Toast.LENGTH_SHORT).show();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    School school = child.getValue(School.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave dell'università corrente
                    // In posizione i della mappa delle chiavi viene memorizzato il nome dell'università corrente
                    schoolKeys.put(i, child.getKey());
                    SCHOOL.add(i, school.name);
                    i++;
                }
                // Vengono visualizzati nello spUni i nomi presenti nell'array di nomi (UNI)
                spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, SCHOOL);
                spinnerAdapter.notifyDataSetChanged();
                spSchool.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}
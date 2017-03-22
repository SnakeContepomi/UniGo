package it.unibo.studio.unigo.signup;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stepstone.stepper.BlockingStep;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Course;
import it.unibo.studio.unigo.utils.School;
import it.unibo.studio.unigo.utils.SignupData;
import it.unibo.studio.unigo.utils.University;

public class Step3Fragment extends Fragment implements BlockingStep
{
    String course_key;
    List<String> uniNames, schoolNames, courseNames;
    HashMap<Integer, String> uniKeys, schoolKeys, courseKeys;

    private DatabaseReference database;

    private MaterialBetterSpinner  spRegion, spUni, spSchool, spCourse;
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

    @Override
    public void onNextClicked(StepperLayout.OnNextClickedCallback callback) { }

    @Override
    public void onCompleteClicked(final StepperLayout.OnCompleteClickedCallback callback)
    {
        if (isValid())
        {
            SignupData.setCourseKey(course_key);
            createAccount(SignupData.getEmail(), SignupData.getPassword());
            callback.complete();
        }
        else
            callback.getStepperLayout().updateErrorState(true);
    }

    @Override
    public void onBackClicked(StepperLayout.OnBackClickedCallback callback) {
        callback.goToPrevStep();
    }

    private void initializeComponents(View v)
    {
        String[] REGIONS = getResources().getStringArray(R.array.regions);

        uniNames = new ArrayList<>();
        uniKeys = new HashMap<>();
        schoolNames = new ArrayList<>();
        schoolKeys = new HashMap<>();
        courseNames = new ArrayList<>();
        courseKeys = new HashMap<>();

        database = FirebaseDatabase.getInstance().getReference();

        spRegion = (MaterialBetterSpinner ) v.findViewById(R.id.spinner_region);
        spUni = (MaterialBetterSpinner ) v.findViewById(R.id.spinner_uni);
        spSchool = (MaterialBetterSpinner) v.findViewById(R.id.spinner_school);
        spCourse = (MaterialBetterSpinner) v.findViewById(R.id.spinner_course);

        spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, REGIONS);
        spRegion.setAdapter(spinnerAdapter);
        spRegion.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                spUni.setEnabled(true);
                spUni.setClickable(true);
                spSchool.setEnabled(false);
                spSchool.setClickable(false);
                spCourse.setEnabled(false);
                spCourse.setClickable(false);
                spUni.getText().clear();
                spSchool.getText().clear();
                spCourse.getText().clear();
                refreshUni();
            }
        });

        spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, uniNames);
        spUni.setAdapter(spinnerAdapter);
        spUni.setEnabled(false);
        spUni.setClickable(false);
        spUni.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                spSchool.setEnabled(true);
                spSchool.setClickable(true);
                spCourse.setEnabled(false);
                spCourse.setClickable(false);
                spSchool.getText().clear();
                spCourse.getText().clear();
                refreshSchool(uniKeys.get(i));
            }
        });

        spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, schoolNames);
        spSchool.setAdapter(spinnerAdapter);
        spSchool.setEnabled(false);
        spSchool.setClickable(false);
        spSchool.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                spCourse.setEnabled(true);
                spCourse.setClickable(true);
                spCourse.getText().clear();
                refreshCourse(schoolKeys.get(i));
            }
        });

        spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, courseNames);
        spCourse.setAdapter(spinnerAdapter);
        spCourse.setEnabled(false);
        spCourse.setClickable(false);
        spCourse.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                course_key = courseKeys.get(i);
            }
        });
    }

    private void refreshUni()
    {
        uniNames.clear();
        uniKeys.clear();

        database.child("University").orderByChild("region").equalTo(spRegion.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Toast.makeText(getContext(),String.valueOf(uniNames.size()) + String.valueOf(uniKeys.size()) , Toast.LENGTH_SHORT).show();
                int i = 0;

                //Toast.makeText(getApplicationContext(), String.valueOf(snapshot.getChildrenCount()) , Toast.LENGTH_SHORT).show();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    University uni = child.getValue(University.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave dell'università corrente
                    // In posizione i della mappa delle chiavi viene memorizzato il nome dell'università corrente
                    uniKeys.put(i, child.getKey());
                    uniNames.add(i, uni.name);
                    i++;
                }
                // Vengono visualizzati nello spUni i nomi presenti nell'array di nomi (uniNames)
                spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, uniNames);
                spinnerAdapter.notifyDataSetChanged();
                spUni.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void refreshSchool(String key)
    {
        schoolNames.clear();
        schoolKeys.clear();

        database.child("School").orderByChild("university_key").equalTo(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Toast.makeText(getContext(),String.valueOf(uniNames.size()) + String.valueOf(uniKeys.size()) , Toast.LENGTH_SHORT).show();
                int i = 0;

                //Toast.makeText(getApplicationContext(), String.valueOf(snapshot.getChildrenCount()) , Toast.LENGTH_SHORT).show();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    School school = child.getValue(School.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave dell'università corrente
                    // In posizione i della mappa delle chiavi viene memorizzato il nome dell'università corrente
                    schoolKeys.put(i, child.getKey());
                    schoolNames.add(i, school.name);
                    i++;
                }
                // Vengono visualizzati nello spUni i nomi presenti nell'array di nomi (uniNames)
                spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, schoolNames);
                spinnerAdapter.notifyDataSetChanged();
                spSchool.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void refreshCourse(String key)
    {
        courseNames.clear();
        courseKeys.clear();

        database.child("Course").orderByChild("school_key").equalTo(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Toast.makeText(getContext(),String.valueOf(uniNames.size()) + String.valueOf(uniKeys.size()) , Toast.LENGTH_SHORT).show();
                int i = 0;

                //Toast.makeText(getApplicationContext(), String.valueOf(snapshot.getChildrenCount()) , Toast.LENGTH_SHORT).show();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Course course = child.getValue(Course.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave dell'università corrente
                    // In posizione i della mappa delle chiavi viene memorizzato il nome dell'università corrente
                    courseKeys.put(i, child.getKey());
                    courseNames.add(i, course.name);
                    i++;
                }
                // Vengono visualizzati nello spUni i nomi presenti nell'array di nomi (uniNames)
                spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, courseNames);
                spinnerAdapter.notifyDataSetChanged();
                spCourse.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private boolean isValid()
    {
        if (spCourse.getText().toString().equals(""))
            return false;

        return true;
    }

    private void createAccount(String email, String password)
    {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        // Invio mail di conferma
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                            {
                                                Log.d("INFO", "Email sent.");
                                            }}
                                    });
                        }
                        Toast.makeText(getContext(), "createUserWithEmail:onComplete:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful())
                        {
                            Toast.makeText(getContext(), "registration failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addUser()
    {
        // todo: aggiungi utente coi dati signupdata
    }
}
package it.unibo.studio.unigo.signup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.stepstone.stepper.BlockingStep;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Course;
import it.unibo.studio.unigo.utils.School;
import it.unibo.studio.unigo.utils.SignupData;
import it.unibo.studio.unigo.utils.University;
import it.unibo.studio.unigo.utils.User;
import it.unibo.studio.unigo.utils.Util;

import static android.R.attr.bitmap;
import static android.R.attr.data;

public class Step3Fragment extends Fragment implements BlockingStep
{
    String course_key;
    List<String> uniNames, schoolNames, courseNames;
    HashMap<Integer, String> uniKeys, schoolKeys, courseKeys;

    private DatabaseReference database, dbUni, dbSchool, dbCourse;
    private FirebaseUser user;

    private MaterialDialog dialog;
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
        spRegion.getText().clear();
        spRegion.clearFocus();
        spUni.getText().clear();
        spSchool.getText().clear();
        spCourse.getText().clear();
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
        if (isValid() && Util.isNetworkAvailable(getContext()))
        {
            dialog.show();
            SignupData.setCourseKey(course_key);
            createAccount(SignupData.getEmail(), SignupData.getPassword(), callback);
        }
        else if (isValid() && (!Util.isNetworkAvailable(getContext())))
            Snackbar
                .make(getActivity().findViewById(R.id.l_signup), R.string.snackbar_registration_failed, Snackbar.LENGTH_LONG)
                .show();
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
        dbUni = FirebaseDatabase.getInstance().getReference("University");
        dbUni.keepSynced(true);
        dbSchool = FirebaseDatabase.getInstance().getReference("School");
        dbSchool.keepSynced(true);
        dbCourse = FirebaseDatabase.getInstance().getReference("Course");
        dbCourse.keepSynced(true);

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

        dialog = new MaterialDialog.Builder(getContext())
                .title(getResources().getString(R.string.alert_dialog_step3_title))
                .content(getResources().getString(R.string.alert_dialog_step3_content))
                .progress(true, 0)
                .cancelable(false)
                .build();
    }

    private void refreshUni()
    {
        uniNames.clear();
        uniKeys.clear();

        //database.child("University")
                dbUni.orderByChild("region").equalTo(spRegion.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
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

        //database.child("School")
                dbSchool.orderByChild("university_key").equalTo(key).addListenerForSingleValueEvent(new ValueEventListener() {
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

        //database.child("Course")
         dbCourse.orderByChild("school_key").equalTo(key).addListenerForSingleValueEvent(new ValueEventListener() {
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

    private void createAccount(String email, String password, final StepperLayout.OnCompleteClickedCallback callback)
    {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        // Invio mail di conferma
                        user = FirebaseAuth.getInstance().getCurrentUser();
                        StorageReference storageRef;
                        if (user != null)
                        {
                            storageRef = FirebaseStorage.getInstance().getReference().child("profile_pic").child(SignupData.getLastName() + "_" + SignupData.getName() + "_" + String.valueOf(System.currentTimeMillis()));

                            if (SignupData.getProfilePic() != null)
                            {
                                UploadTask uploadTask = storageRef.putBytes(bitmapToByteArray(SignupData.getProfilePic()));
                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                        @SuppressWarnings("VisibleForTests")
                                        Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(SignupData.getName() + " " + SignupData.getLastName())
                                                .setPhotoUri(downloadUrl)
                                                .build();
                                        user.updateProfile(profileUpdates);

                                        user.sendEmailVerification()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        // Aggiunta dell'utente al database
                                                        if (task.isSuccessful())
                                                            addUser(callback);
                                                    }
                                                });
                                    }
                                });
                            }
                            else
                            {
                                FirebaseStorage.getInstance().getReference()
                                        .child("profile_pic/empty_profile_pic.png").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                // Aggiornamento nominativo utente
                                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                        .setDisplayName(SignupData.getName() + " " + SignupData.getLastName())
                                                        .setPhotoUri(uri)
                                                        .build();
                                                user.updateProfile(profileUpdates);

                                                user.sendEmailVerification()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                // Aggiunta dell'utente al database
                                                                if (task.isSuccessful())
                                                                    addUser(callback);
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }

                        // Errore nella creazione dell'account
                        if (!task.isSuccessful())
                        {
                            dialog.dismiss();
                            Snackbar
                                    .make(getActivity().findViewById(R.id.l_signup), R.string.snackbar_registration_failed, Snackbar.LENGTH_LONG)
                                    .show();
                            callback.getStepperLayout().updateErrorState(true);
                        }
                    }
                });
    }

    private void addUser(final StepperLayout.OnCompleteClickedCallback callback)
    {
        database = FirebaseDatabase.getInstance().getReference();
        database.child("User").push().setValue(
                new User(SignupData.getEmail(), SignupData.getName(), SignupData.getLastName(), SignupData.getPhone(),
                         SignupData.getCity(), SignupData.getCourseKey())).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                dialog.dismiss();
                callback.complete();

                dbUni.keepSynced(false);
                dbSchool.keepSynced(false);
                dbCourse.keepSynced(false);

                Intent returnIntent = new Intent();
                returnIntent.putExtra("result_email",SignupData.getEmail());
                getActivity().setResult(Activity.RESULT_OK, returnIntent);
                SignupData.clear();
                getActivity().finish();
            }
        });
    }

    private byte[] bitmapToByteArray(Bitmap img)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        return baos.toByteArray();
    }
}
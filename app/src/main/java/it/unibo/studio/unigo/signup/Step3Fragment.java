package it.unibo.studio.unigo.signup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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
import java.util.HashMap;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.firebase.Course;
import it.unibo.studio.unigo.utils.firebase.School;
import it.unibo.studio.unigo.utils.SignupData;
import it.unibo.studio.unigo.utils.firebase.University;
import it.unibo.studio.unigo.utils.firebase.User;
import it.unibo.studio.unigo.utils.Util;

public class Step3Fragment extends Fragment implements BlockingStep
{
    private String course_key;
    private List<String> uniNames, schoolNames, courseNames;
    private HashMap<Integer, String> uniKeys, schoolKeys, courseKeys;
    private DatabaseReference dbUni, dbSchool, dbCourse;
    private FirebaseUser user;

    private MaterialDialog dialog;
    private MaterialBetterSpinner  spRegion, spUni, spSchool, spCourse;
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_step3, container, false);

        initializeComponents(v);

        return v;
    }

    @Override
    public VerificationError verifyStep()
    {
        return null;
    }

    @Override
    public void onSelected()
    {
        spRegion.getText().clear();
        spRegion.clearFocus();
        spUni.getText().clear();
        spSchool.getText().clear();
        spCourse.getText().clear();
    }

    @Override
    public void onError(@NonNull VerificationError error) { }

    @Override
    public void onNextClicked(StepperLayout.OnNextClickedCallback callback) { }

    // Metodo richiamato quando viene completata la registrazione (tasto 'FINE')
    @Override
    public void onCompleteClicked(final StepperLayout.OnCompleteClickedCallback callback)
    {
        // Se i campi sono compilati correttamente e la rete è disponibile, viene creato l'account
        if (isValid() && Util.isNetworkAvailable(getContext()))
        {
            dialog.show();
            SignupData.setCourseKey(course_key);
            createAccount(SignupData.getEmail(), SignupData.getPassword(), callback);
        }
        // Se i campi sono validi ma non c'è connessione, viene notificato l'errore
        else if (isValid() && (!Util.isNetworkAvailable(getContext())))
            Snackbar
                .make(getActivity().findViewById(R.id.l_signup), R.string.snackbar_registration_failed, Snackbar.LENGTH_LONG)
                .show();
        // Se i campi non sono validi, viene bloccato lo Stepper
        else
            callback.getStepperLayout().updateErrorState(true);
    }

    @Override
    public void onBackClicked(StepperLayout.OnBackClickedCallback callback)
    {
        callback.goToPrevStep();
    }

    private void initializeComponents(View v)
    {
        String[] REGIONS = getResources().getStringArray(R.array.regions);

        // Liste dei nomi presenti negli spinner e delle chiavi associate
        uniNames = new ArrayList<>();
        uniKeys = new HashMap<>();
        schoolNames = new ArrayList<>();
        schoolKeys = new HashMap<>();
        courseNames = new ArrayList<>();
        courseKeys = new HashMap<>();

        // Abilitazione dell'utilizzo di Firebase in modalità offline per le tabelle University, School e Course
        dbUni = Util.getDatabase().getReference("University");
        dbSchool = Util.getDatabase().getReference("School");
        dbCourse = Util.getDatabase().getReference("Course");

        spRegion = (MaterialBetterSpinner ) v.findViewById(R.id.spinner_region);
        spUni = (MaterialBetterSpinner ) v.findViewById(R.id.spinner_uni);
        spSchool = (MaterialBetterSpinner) v.findViewById(R.id.spinner_school);
        spCourse = (MaterialBetterSpinner) v.findViewById(R.id.spinner_course);

        // Inizializzazione dei quattro listener dei MaterialSpinner
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

    // Metodo per caricare le università ogni volta che viene selezionata una regione
    private void refreshUni()
    {
        uniNames.clear();
        uniKeys.clear();

        // Query che restituisce tutte le università di una determinata regione
        dbUni.orderByChild("region").equalTo(spRegion.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                int i = 0;

                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    University uni = child.getValue(University.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave dell'università corrente
                    uniKeys.put(i, child.getKey());
                    // In posizione i della mappa dei nomi viene memorizzato il nome dell'università corrente
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

    // Metodo per caricare le scuole ogni volta che viene selezionata un'università
    private void refreshSchool(String key)
    {
        schoolNames.clear();
        schoolKeys.clear();

        // Query che restituisce tutte le scuole di una determinata università
        dbSchool.orderByChild("university_key").equalTo(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                int i = 0;

                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    School school = child.getValue(School.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave della scuola corrente
                    schoolKeys.put(i, child.getKey());
                    // In posizione i della mappa dei nomi viene memorizzato il nome della scuola corrente
                    schoolNames.add(i, school.name);
                    i++;
                }
                // Vengono visualizzati nello spSchool i nomi presenti nell'array di nomi (schoolNames)
                spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, schoolNames);
                spinnerAdapter.notifyDataSetChanged();
                spSchool.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per caricare i corsi ogni volta che viene selezionata una scuola
    private void refreshCourse(String key)
    {
        courseNames.clear();
        courseKeys.clear();

        // Query che restituisce tutti i corsi di una determinata scuola
        dbCourse.orderByChild("school_key").equalTo(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                int i = 0;

                for (DataSnapshot child : dataSnapshot.getChildren())
                {
                    Course course = child.getValue(Course.class);
                    // In posizione i della mappa delle chiavi viene memorizzata la chiave del corso corrente
                    courseKeys.put(i, child.getKey());
                    // In posizione i della mappa dei nomi viene memorizzato il nome del corso corrente
                    courseNames.add(i, course.name);
                    i++;
                }
                // Vengono visualizzati nello spCourse i nomi presenti nell'array di nomi (courseNames)
                spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, courseNames);
                spinnerAdapter.notifyDataSetChanged();
                spCourse.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che controlla se è stato selezionato un corso
    private boolean isValid()
    {
        return !spCourse.getText().toString().equals("");

    }

    // Metodo per creare un account Firebase con email e password
    private void createAccount(String email, String password, final StepperLayout.OnCompleteClickedCallback callback)
    {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Creazione dell'account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        user = FirebaseAuth.getInstance().getCurrentUser();
                        StorageReference storageRef;
                        // Creato l'account, vengono impostati gli attributi 'DisplayName' e 'PhotoUrl'
                        if (user != null)
                        {
                            storageRef = FirebaseStorage.getInstance().getReference().child("profile_pic").child(SignupData.getLastName() + "_" + SignupData.getName() + "_" + String.valueOf(System.currentTimeMillis()));

                            // Se è stata selezionata un'immagine dalla galleria, viene caricata
                            if (SignupData.getProfilePic() != null)
                            {
                                UploadTask uploadTask = storageRef.putBytes(bitmapToByteArray(SignupData.getProfilePic()));
                                // Una volta caricata l'immagine profilo, essa viene collegata all'account insieme all'attributo 'DisplayName',
                                // e successivamente viene inviata la mail di conferma
                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        @SuppressWarnings("VisibleForTests")
                                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                        SignupData.setPhotoUrl(downloadUrl.toString());

                                        // Aggiornamento informazioni profilo
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(SignupData.getName() + " " + SignupData.getLastName())
                                                .setPhotoUri(downloadUrl)
                                                .build();
                                        user.updateProfile(profileUpdates);

                                        // Invio mail di conferma + Aggiunta al database di un utente con le informazioni appena inserite
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful())
                                                            addUser(callback);
                                                    }
                                                });
                                    }
                                });
                            }
                            // Se non è stata selezionata nessuna immagine dalla galleria, viene utilizzata quella di default presente sul server
                            else
                            {
                                // Si utilizza come immagine profilo 'empty_profile_pic.png', già presente sul server. Successivamente viene inviata la mail di conferma
                                FirebaseStorage.getInstance().getReference()
                                        .child("profile_pic/empty_profile_pic.png").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                SignupData.setPhotoUrl(getContext().getResources().getString(R.string.empty_profile_pic_url));

                                                // Aggiornamento informazioni profilo
                                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                        .setDisplayName(SignupData.getName() + " " + SignupData.getLastName())
                                                        .setPhotoUri(uri)
                                                        .build();
                                                user.updateProfile(profileUpdates);

                                                // Invio mail di conferma + Aggiunta al database di un utente con le informazioni appena inserite
                                                user.sendEmailVerification()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful())
                                                                    addUser(callback);
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                        // Se si è verificato un errore durante la creazione dell'account, questo viene notificato tramite Snackbar
                        if (!task.isSuccessful())
                        {
                            dialog.dismiss();
                            Snackbar
                                    .make(getActivity().findViewById(R.id.l_signup), R.string.snackbar_registration_failed, Snackbar.LENGTH_LONG)
                                    .show();
                            callback.getStepperLayout().updateErrorState(true);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Snackbar
                            .make(getActivity().findViewById(R.id.l_signup), R.string.error_email_malformed, Snackbar.LENGTH_LONG)
                            .show();
                        callback.getStepperLayout().updateErrorState(true);
                    }
                });
    }

    // Metodo per aggiungere alla tabella User del database una nuova entry, con le informazioni inserite dall'utente,
    // recuperate dalla classe statica SignupData
    private void addUser(final StepperLayout.OnCompleteClickedCallback callback)
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(SignupData.getEmail())).setValue(
                new User(SignupData.getPhotoUrl(), SignupData.getName(), SignupData.getLastName(), SignupData.getPhone(),
                         SignupData.getCity(), SignupData.getCourseKey()))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        // Viene collegato l'utente creato al corso selezionato
                        Util.getDatabase().getReference("Course").child(SignupData.getCourseKey()).child("users").child(Util.encodeEmail(SignupData.getEmail())).setValue(true);

                        dialog.dismiss();
                        callback.complete();

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("result_email",SignupData.getEmail());
                        getActivity().setResult(Activity.RESULT_OK, returnIntent);
                        SignupData.clear();
                        getActivity().finish();
                    }
                });
    }

    // Metodo per convertire un'immagine Bitmap in un Array di Byte
    private byte[] bitmapToByteArray(Bitmap img)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        return baos.toByteArray();
    }
}
package it.unibo.studio.unigo.main;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.QuestionDetailPictureAdapter;
import it.unibo.studio.unigo.main.adapters.ChipAdapter;
import it.unibo.studio.unigo.utils.Error;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Question;
import it.unibo.studio.unigo.utils.firebase.User;

public class NewPostActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener
{
    // Dimensione massima consentita per ciascuna immagine e file (in KiloBytes)
    private static final int IMAGE_SIZE_TO_COMPRESS_KB = 750;
    private static final int MAX_FILE_SIZE = 10000;
    // Numero masssimo di allegati per ciascuna domanda (immagini e file hanno contatori separati)
    private static final int NUM_FILE_ALLOWED = 5;
    private final int NUM_IMAGE_ALLOWED = 5;
    // Permessi per accedere alla fotocamera e alla memoria interna del dispositivo
    private final int REQUEST_CAMERA_PERMISSION = 0;
    private final int REQUEST_GALLERY_PERMISSION = 1;
    private final int REQUEST_FILE_PICKER = 2;
    private final int REQUEST_IMAGE_CAPTURE = 3;
    private final int REQUEST_IMAGE_FROM_GALLERY = 4;

    private EditText etTitle, etCourse, etDesc;
    private ChipAdapter chipAdapter;
    // Adapter utilizzato da StaticGridViewer per la rappresentazione delle immagini allegate
    private QuestionDetailPictureAdapter photoAdapter;
    private MaterialDialog dialog, permissionDialog;
    // Percorso dell'immagine attualmente scattata dalla fotocamera
    private String currentPhotoPath;
    // Indice dell'immagine da caricare su FirebaseStorage, utilizzato per capire
    // se si sta caricando l'ultima immagine oppure se ve ne sono delle altre
    private int indexImageUploaded;
    // Indice del file da caricare su FirebaseStorage
    private int indexFileUploaded;
    // Lista di url delle immagini caricate con successo su Firebase
    private List<String> urlPicList = new ArrayList<>();
    // Lista di url delle dei file caricati con successo su Firebase
    private List<String> urlFileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        overridePendingTransition(R.anim.activity_open_translate_from_bottom, R.anim.activity_no_animation);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        initComponents();
    }

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.activity_no_animation, R.anim.activity_close_translate_to_bottom);
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        deletePictures();
        setResult(Activity.RESULT_CANCELED);
    }

    // Listener sul pulsante di invio del nuovo post
    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        Error.Type error;
        switch (item.getItemId())
        {
            case R.id.post_toolbar_send:
                error = isValid();
                if (error == null)
                    validatePost();
                else
                    errorHandler(error);
                break;
            // Per utilizzare la fotocamera occorrono i permessi CAMERA e WRITE_EXTERNAL_STORAGE
            case R.id.menuListItemCamera:
                if (getCameraPermissions())
                    openCamera();
                break;

            // Per utilizzare la galleria immagini occorre il permesso WRITE_EXTERNAL_STORAGE
            case R.id.menuListItemGallery:
                // *** requestPermissions necessita SDK >= 23 ***
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_GALLERY_PERMISSION);
                else
                    openGallery();
                break;

            case R.id.menuListItemFile:
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_FILE_PICKER);
                else
                    openFilePicker();
                break;
        }
        return true;
    }

    // Viene verificato se l'app possiede dei permessi per accedere alla fotocamera (CAMERA e WRITE_EXTERNAL_STORAGE)
    private boolean getCameraPermissions()
    {
        // Permessi di cui si ha bisogno
        List<String> listPermissionsNeeded = new ArrayList<>();

        // Permessi CAMERA
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        // Permessi WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    // Gestione della chiusura dell'activity Camera e Gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            // Alla chiusura della fotocamera viene caricata la foto scattata (compressa)
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK)
                {
                    File camFile = new File(currentPhotoPath);
                    File compressedFile = compressFile(camFile);
                    //noinspection ResultOfMethodCallIgnored
                    camFile.delete();
                    assert compressedFile != null;
                    photoAdapter.addElement(compressedFile.getAbsolutePath());
                }
                break;

            // Alla chiusura della galleria vengono caricate le immagini selezionate,
            // opportunamente ridimensionate se necessario
            case REQUEST_IMAGE_FROM_GALLERY:
                if ((resultCode == RESULT_OK) && (data != null))
                {
                    ArrayList<Image> images = (ArrayList<Image>) ImagePicker.getImages(data);
                    for(Image img : images)
                    {
                        String filePath = img.getPath();
                        File file = new File(filePath);
                        // Se il file pesa più di IMAGE_SIZE_TO_COMPRESS_KB viene compresso
                        if ((file.length() / 1024) > IMAGE_SIZE_TO_COMPRESS_KB)
                        {
                            File compressedFile = compressFile(file);
                            assert compressedFile != null;
                            photoAdapter.addElement(compressedFile.getAbsolutePath());
                        }
                        else
                            photoAdapter.addElement(filePath);
                    }
                }
                break;

            case REQUEST_FILE_PICKER:
                if (resultCode == RESULT_OK)
                {
                    if (new File(data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)).length() / 1024 > MAX_FILE_SIZE)
                        openAlertDialog(getString(R.string.alert_dialog_max_file_size));
                    else
                    {
                        String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
                        chipAdapter.addElement(filePath);
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CAMERA_PERMISSION:
                // Permessi concessi, avvio della fotocamera
                if ((grantResults.length > 1) && (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&  (grantResults[1] == PackageManager.PERMISSION_GRANTED))
                    openCamera();
                // Se invece è presente almeno un permesso negato con opzione "non mosrare piu", allora viene mostrato un dialogPermission
                // che richiede l'abilitazione dei permessi attraverso le impostazioni di sistema
                else
                {
                    // Lista dei permessi che sono stati negati in modo permanente
                    List<String> listPermissionDenied = new ArrayList<>();

                    // Riempimento della lista con i permessi rifiutati permanentemente
                    for (String permission : permissions)
                        if (permission.equals(Manifest.permission.CAMERA) &&
                                ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                            listPermissionDenied.add(getString(R.string.permission_camera));
                        else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                                ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                            listPermissionDenied.add(getString(R.string.permission_storage));

                    // Se si rifiutano i permessi utilizzando l'opzione 'non mostrare più', verrà notificato all'utente
                    // di abilitare i permessi attraverso le impostazioni di sistema
                    if (!listPermissionDenied.isEmpty())
                    {
                        if (listPermissionDenied.size() == 2)
                            buildPermissionDialog(getString(R.string.permission_needed_camera_storage));
                        else if (listPermissionDenied.get(0).equals(getString(R.string.permission_camera)))
                            buildPermissionDialog(getString(R.string.permission_needed_camera));
                        else
                            buildPermissionDialog(getString(R.string.permission_needed_storage));

                        permissionDialog.show();
                    }
                }
                break;

            case REQUEST_GALLERY_PERMISSION:
                // Permesso concesso, apertura della galleria immagini
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    openGallery();
                // Permesso negato: non è possibile selezionare un immagine dalla galleria
                // Se si rifiutano i permessi utilizzando l'opzione 'non mostrare più', verrà notificato all'utente
                // di abilitare i permessi attraverso le impostazioni di sistema
                else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                {
                    buildPermissionDialog(getString(R.string.permission_needed_storage));
                    permissionDialog.show();
                }
                break;

            case REQUEST_FILE_PICKER:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openFilePicker();
                // Viene controllato se l'utente ha selezionato la voce 'non mostrare più questo messaggio' nel momento in cui
                // ha negato il permesso richiesto, per ricordare di fornire manualmente i permessi richiesti
                else if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
                {
                    buildPermissionDialog(getString(R.string.permission_needed_read));
                    permissionDialog.show();
                }
        }
    }

    private void initComponents()
    {
        // Inizializzazione Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.PostToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.toolbar_newpost);
        toolbar.inflateMenu(R.menu.menu_item_attachment);
        toolbar.setOnMenuItemClickListener(this);

        etTitle = (EditText) findViewById(R.id.etPostTitle);
        etCourse = (EditText) findViewById(R.id.etPostCourse);
        etDesc = (EditText) findViewById(R.id.etPostDesc);

        RecyclerView recyclerViewChips = (RecyclerView) findViewById(R.id.rvPostChip);
        recyclerViewChips.setHasFixedSize(true);
        recyclerViewChips.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        chipAdapter = new ChipAdapter();
        recyclerViewChips.setAdapter(chipAdapter);

        RecyclerView recyclerViewPhoto = (RecyclerView) findViewById(R.id.recyclerViewPhoto);
        recyclerViewPhoto.setHasFixedSize(true);
        recyclerViewPhoto.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        photoAdapter = new QuestionDetailPictureAdapter();
        recyclerViewPhoto.setAdapter(photoAdapter);

        dialog = new MaterialDialog.Builder(this)
                .progress(true, 0)
                .cancelable(false)
                .build();

        // Se il device non possiede la fotocamera, la voce "scatta foto" viene rimossa
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            toolbar.getMenu().getItem(0).getSubMenu().removeItem(0);
    }

    // Dialog utilizzato esclusivamente per la segnalazione di avvisi/errori
    private void openAlertDialog(String message)
    {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.alert_dialog_warning))
                .content(message)
                .cancelable(true)
                .positiveText(getString(R.string.alert_dialog_confirm))
                .positiveColor(ContextCompat.getColor(this, R.color.colorAccent))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();
        dialog.show();
    }

    // Dialog utilizzato per richiedere all'utente di abilitare manualmente i permessi dalle impostazioni di sistema
    private void buildPermissionDialog(String content)
    {
        permissionDialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.permission_denied))
                .content(content)
                .positiveText(getString(R.string.drawer_impostazioni))
                .positiveColor(ContextCompat.getColor(this, R.color.colorAccent))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.fromParts(getString(R.string.intent_package), getPackageName(), null));
                        startActivity(intent);
                    }
                })
                .negativeText(getString(R.string.alert_dialog_cancel))
                .negativeColor(ContextCompat.getColor(this, R.color.colorAccent))
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();
    }

    // Metodo utilizzato per avviare la fotocamera. La foto catturata viene memorizzata
    // nella memoria di massa (qualità originale)
    private void openCamera()
    {
        if (photoAdapter.getItemCount() < NUM_IMAGE_ALLOWED)
        {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Creazione del file dove verrà memorizzata la foto
                File photoFile = null;
                try
                {
                    photoFile = createImageFile();
                    currentPhotoPath = photoFile.getAbsolutePath();
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
                // Viene avviata la fotocamera solamente se è stato creato il file dove memorizzare l'immagine che verrà catturata
                if (photoFile != null)
                {
                    Uri photoURI = FileProvider.getUriForFile(this, "it.unibo.studio.unigo.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }
        else
            openAlertDialog(getString(R.string.alert_dialog_max_picture));
    }

    // Metodo utilizzato per aprire la galleria per la selezione delle immagini
    // (tramite Maerial ImagePicker)
    private void openGallery()
    {
        if (photoAdapter.getItemCount() < NUM_IMAGE_ALLOWED)
            ImagePicker.create(this)
                    .returnAfterFirst(false)
                    .folderMode(true)
                    .folderTitle(getString(R.string.imgpicker_select_folder))
                    .imageTitle(getString(R.string.imgpicker_select_img))
                    .multi()
                    .limit(NUM_IMAGE_ALLOWED - photoAdapter.getItemCount())
                    .showCamera(false)
                    .theme(R.style.AppTheme)
                    .start(REQUEST_IMAGE_FROM_GALLERY);
        else
            openAlertDialog(getString(R.string.alert_dialog_max_picture));
    }

    // Metodo utilizzato per scegliere un file da caricare insieme alla domanda
    private void openFilePicker()
    {
        if (chipAdapter.getItemCount() < NUM_FILE_ALLOWED)
            new MaterialFilePicker()
                    .withActivity(this)
                    .withRequestCode(REQUEST_FILE_PICKER)
                    .withHiddenFiles(true)
                    .start();
        else
            openAlertDialog(getString(R.string.alert_dialog_max_file));
    }

    // Metodo che permette di salvare la foto appena scattata nella cartella privata dell'app
    @NonNull
    private File createImageFile() throws IOException
    {
        // Definizione del nome della foto e del path dove salvarla
        String imageFileName = "IMG_" + String.valueOf(System.currentTimeMillis()/1000);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // Metodo che rimuove le immagini presenti nella cartella privata dell'applicazione, in quanto non più necessarie
    private void deletePictures()
    {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir != null && dir.isDirectory())
        {
            File[] children = dir.listFiles();
            for (File child : children)
                //noinspection ResultOfMethodCallIgnored
                child.delete();
        }
    }

    // Controllo di validità dei campi:
    // - Esiste un errore --> viene ritornato il codice dell'errore
    // - Non vi sono errori --> viene ritornato nul
    private Error.Type isValid()
    {
        if (Util.isNetworkAvailable(getApplicationContext()))
        {
            if (etTitle.getText().toString().equals(""))
                return Error.Type.TITLE_IS_EMPTY;
            if (etCourse.getText().toString().equals(""))
                return Error.Type.COURSE_IS_EMPTY;
            if (etDesc.getText().toString().equals(""))
                return Error.Type.DESC_IS_EMPTY;
            return null;
        }
        else
            return Error.Type.NETWORK_UNAVAILABLE;
    }

    // Metodo che gestisce gli errori sui campi e mostra un alert dialog
    private void errorHandler(Error.Type e)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (e)
        {
            // Connessione non disponibile
            case NETWORK_UNAVAILABLE:
                builder.setTitle(getString(R.string.alert_dialog_warning));
                builder.setMessage(getString(R.string.error_network_unavailable));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
            // Titolo vuoto
            case TITLE_IS_EMPTY:
                builder.setMessage(getString(R.string.error_post_title));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
            // Materia vuota
            case COURSE_IS_EMPTY:
                builder.setMessage(getString(R.string.error_post_course));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
            // Nessuna descrizione, viene mostrato l'errore come warning, ma il post è comunque valido
            case DESC_IS_EMPTY:
                builder.setMessage(getString(R.string.error_post_desc));
                builder.setPositiveButton(getString(R.string.alert_dialog_send),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                validatePost();
                            }
                        });
                builder.setNegativeButton(getString(R.string.alert_dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
                break;
            case NOT_ENOUGH_CREDITS:
                builder.setMessage(getString(R.string.error_not_enough_credits));
                builder.setPositiveButton(getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });
        }

        // Visualizzazione alertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Metodo che verifica se l'utente ha crediti sufficienti per effettuare la domanda.
    // In caso positivo, la domanda viene inserita correttamente e vengono scalati i crediti dal profilo,
    // altrimenti viene restituito un errore
    private void validatePost()
    {
        dialog.show();

        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail()))
            .runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData)
                {
                    User u = mutableData.getValue(User.class);
                    if (u == null)
                        return Transaction.abort();
                    // L'utente possiede crediti sufficienti per effettuare la domanda
                    if (u.credits >= Util.CREDITS_QUESTION)
                    {
                        u.credits -= Util.CREDITS_QUESTION;
                        mutableData.setValue(u);
                        return Transaction.success(mutableData);
                    }
                    // L'utente non possiede crediti sufficienti per effettuare la domanda
                    else
                        return Transaction.abort();
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean success, DataSnapshot dataSnapshot)
                {
                    if (success)
                    {
                        // Se non è stato inserito nessun allegato (foto/file), viene aggiunta direttamente la domanda
                        if (chipAdapter.getItemCount() == 0 && photoAdapter.getItemCount() == 0)
                            addPost();
                        // Se è stato inserito almeno un allegato, questo/i verranno inseriti prima dell'inserimento della domanda stessa
                        // (vengono creati i link di riferimento alle risorse, che andranno memorizzati all'interno della nuova domanda)
                        if (chipAdapter.getItemCount() != 0)
                        {
                            urlFileList.clear();
                            uploadFile(0, chipAdapter.getItemCount());
                        }
                        // Se è stata inserita almeno un'immagine, verranno caricate prima dell'inserimento della domanda stessa
                        // (vengono creati i link di riferimento alle risorse, che andranno memorizzati all'interno della nuova domanda)
                        if (photoAdapter.getItemCount() != 0)
                        {
                            urlPicList.clear();
                            uploadImage(0, photoAdapter.getItemCount());
                        }
                    }
                    else
                        errorHandler(Error.Type.NOT_ENOUGH_CREDITS);
                }
            });
    }

    // Caricamento degli allegati su FirebaseStorage. Per poter caricare più allegati in una domanda,
    // il metodo viene richiamato ricorsivamente affinché tutti gli elementi non sono stati caricati
    private void uploadFile(int positionInList, final int numFile)
    {
        String operation = String.format(getString(R.string.alert_dialog_request_finish_op_upload), indexFileUploaded + indexImageUploaded, chipAdapter.getItemCount() + photoAdapter.getItemCount());
        String filePath = chipAdapter.getChipFile(positionInList);

        dialog.setContent(String.format(getString(R.string.alert_dialog_request_finish_content), operation));
        if (!dialog.isShowing())
            dialog.show();

        final UploadTask uploadTask = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.storage_profile_attachment_folder))
                .child(String.valueOf(System.currentTimeMillis()/1000) + "_" + filePath.substring(filePath.lastIndexOf("/") + 1))
                .putBytes(convertFileToByteArray(new File(filePath)));

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @SuppressWarnings({"VisibleForTests", "ConstantConditions"})
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                indexFileUploaded++;
                urlFileList.add(taskSnapshot.getDownloadUrl().toString());
                String operation = String.format(getString(R.string.alert_dialog_request_finish_op_upload), indexFileUploaded + indexImageUploaded, chipAdapter.getItemCount() + photoAdapter.getItemCount());
                dialog.setContent(String.format(getString(R.string.alert_dialog_request_finish_content), operation));

                // Se vi sono altre immagini da caricare, si procede con il caricamento della foto successiva
                if (indexFileUploaded != numFile)
                    uploadFile(indexFileUploaded, numFile);

                // Se sono stati caricati tutti gli allegati (foto/file), viene inserita la domadna
                if (indexFileUploaded + indexImageUploaded == chipAdapter.getItemCount() + photoAdapter.getItemCount())
                    addPost();
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                uploadFile(indexFileUploaded, numFile);
            }
        });
    }

    private byte[] convertFileToByteArray(File f)
    {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead;

            while ((bytesRead = inputStream.read(b)) != -1)
                bos.write(b, 0, bytesRead);

            byteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return byteArray;
    }

    // Caricamento degli allegati su FirebaseStorage. Per poter caricare più allegati in una domanda,
    // il metodo viene richiamato ricorsivamente affinché tutti gli elementi non sono stati caricati
    private void uploadImage(int positionInList, final int numImages)
    {
        String operation = String.format(getString(R.string.alert_dialog_request_finish_op_upload), indexFileUploaded + indexImageUploaded, chipAdapter.getItemCount() + photoAdapter.getItemCount());
        String imagePath = photoAdapter.getPicture(positionInList);
        Bitmap image = BitmapFactory.decodeFile(imagePath);

        dialog.setContent(String.format(getString(R.string.alert_dialog_request_finish_content), operation));
        if (!dialog.isShowing())
            dialog.show();

        final UploadTask uploadTask = FirebaseStorage.getInstance().getReference()
                        .child(getString(R.string.storage_profile_attachment_folder))
                        .child(imagePath.substring(imagePath.lastIndexOf("/") + 1))
                        .putBytes(bitmapToByteArray(image));

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @SuppressWarnings({"VisibleForTests", "ConstantConditions"})
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                indexImageUploaded++;
                urlPicList.add(taskSnapshot.getDownloadUrl().toString());
                String operation = String.format(getString(R.string.alert_dialog_request_finish_op_upload), indexFileUploaded + indexImageUploaded, chipAdapter.getItemCount() + photoAdapter.getItemCount());
                dialog.setContent(String.format(getString(R.string.alert_dialog_request_finish_content), operation));

                // Se vi sono altre immagini da caricare, si procede con il caricamento della foto successiva
                if (indexImageUploaded != numImages)
                    uploadImage(indexImageUploaded, numImages);

                // Se sono stati caricati tutti gli allegati (foto/file), viene inserita la domadna
                if (indexFileUploaded + indexImageUploaded == chipAdapter.getItemCount() + photoAdapter.getItemCount())
                    addPost();
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                uploadImage(indexImageUploaded, numImages);
            }
        });
    }

    // Metodo per aggiungere al database la domanda appena compilata e collegarla all'utente
    private void addPost()
    {
        if (!dialog.isShowing())
        {
            dialog.setContent(String.format(getString(R.string.alert_dialog_request_finish_content), getString(R.string.alert_dialog_post_creation)));
            dialog.show();
        }
        final String key = Util.getDatabase().getReference("Question").push().getKey();
        final Question question = new Question(formatString(etTitle.getText().toString()), formatString(etCourse.getText().toString()), formatString(etDesc.getText().toString()),
                                  Util.encodeEmail(Util.getCurrentUser().getEmail()), Util.CURRENT_COURSE_KEY);
        Util.getDatabase().getReference("Question").child(key).setValue(question).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                // Vengono memorizzati, se presenti, tutti i link di riferimento alle risorse caricate assieme alla domanda (foto/documenti)
                if (urlFileList.size() != 0)
                    for(String fileUrl : urlFileList)
                        Util.getDatabase().getReference("Question").child(key).child("attachments").push().setValue(fileUrl);
                if (urlPicList.size() != 0)
                    for(String imageUrl : urlPicList)
                        Util.getDatabase().getReference("Question").child(key).child("images").push().setValue(imageUrl);
                linkPostToCourse(key, question.date);
                linkPostToUser(key);
                if (dialog.isShowing())
                    dialog.dismiss();
                Toast.makeText(getApplicationContext(), R.string.toast_post_sent, Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    // Metodo per collegare la domanda appena creata all'utente che l'ha effettuata
    private void linkPostToUser(String question_key)
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("questions").child(question_key).setValue(true);
    }

    private void linkPostToCourse(String question_key, String date)
    {
        Util.getDatabase().getReference("Course").child(Util.CURRENT_COURSE_KEY).child("questions").child(question_key).setValue(date);
    }

    // Metodo che restituisce la stringa presa in ingresso, con il primo carattere in maiuscolo
    private String formatString(String string)
    {
        return (string.length() > 0) ? string.substring(0,1).toUpperCase() + string.substring(1) : "";
    }

    // Metodo per convertire un'immagine Bitmap in un Array di Byte
    private byte[] bitmapToByteArray(Bitmap img)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        return baos.toByteArray();
    }

    // Metodo per comprimere il file selezionato per pesare meno di 100Kb
    // (applicando eventualmente il downscale mantendendo il ratio originale)
    private File compressFile(File f)
    {
        try
        {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // The new size we want to scale to
            final int REQUIRED_SIZE = 480;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while(o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;

            // Conversione dell'immagine Bitmap in un file nel path corretto
            File file = createImageFile();
            FileOutputStream fOut = new FileOutputStream(file);
            BitmapFactory.decodeStream(new FileInputStream(f), null, o2).compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            return file;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
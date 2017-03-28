package it.unibo.studio.unigo;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import it.unibo.studio.unigo.main.MainActivity;
import it.unibo.studio.unigo.signup.SignupActivity;
import it.unibo.studio.unigo.utils.Error;
import it.unibo.studio.unigo.utils.Util;
import static it.unibo.studio.unigo.utils.Error.resetError;
import static it.unibo.studio.unigo.utils.Error.resetErrorAndClearText;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener
{
    static final int USER_EMAIL_REQUEST = 1;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private TextInputLayout inEmail, inPass;
    private TextView txtSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Utilizzo dell'App in modalità Fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);
        initComponents();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (mAuthListener != null)
            mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btnLogin:
                login(inEmail.getEditText().getText().toString(), inPass.getEditText().getText().toString());
                break;
            case R.id.txtSignUp:
                if (Util.isNetworkAvailable(getApplicationContext()))
                    startActivityForResult(new Intent(LoginActivity.this, SignupActivity.class), USER_EMAIL_REQUEST);
                else
                {
                    Snackbar
                        .make(findViewById(R.id.l_login), R.string.snackbar_no_internet_connection, Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_retry_login, new View.OnClickListener() {
                            @Override
                            public void onClick(View view)
                            {
                                txtSignUp.performClick();
                            }
                        })
                        .show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == USER_EMAIL_REQUEST)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                inEmail.getEditText().setText(data.getStringExtra("result_email"));
                Snackbar
                    .make(findViewById(R.id.l_login), R.string.snackbar_success, Snackbar.LENGTH_LONG)
                    .show();
            }
            if (resultCode == Activity.RESULT_CANCELED)
            {
                Snackbar
                    .make(findViewById(R.id.l_login), R.string.snackbar_registration_cancelled, Snackbar.LENGTH_LONG)
                    .show();
            }
        }
    }

    private void initComponents()
    {
        inEmail = (TextInputLayout) findViewById(R.id.inEmail);
        inPass = (TextInputLayout) findViewById(R.id.inPass);
        txtSignUp = (TextView) findViewById(R.id.txtSignUp);

        mAuth = FirebaseAuth.getInstance();
        // Listener sull'utente attualmente connesso
        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null)
                {
                    // L'utente è autenticato e ha verificato l'email, quindi può utilizzare l'App
                    if (user.isEmailVerified()) {
                        Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(myIntent);
                        finish();
                    }
                }
                // L'utente si è disconnesso
                else { }
            }
        };

        inEmail.getEditText().setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inEmail.isErrorEnabled())
                    resetError(inEmail);
                return false;
            }
        });
        inPass.getEditText().setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inPass.isErrorEnabled())
                    resetErrorAndClearText(inPass);
                return false;
            }
        });
    }

    // Metodo che permette di effettuare il login tramite email e password, con controllo degli errori
    private void login(String email, String password)
    {
        boolean emptyField = false;

        // Campo email vuoto
        if (inEmail.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.EMAIL_IS_EMPTY);
            emptyField = true;
        }
        // Campo password vuoto
        if (inPass.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.PASSWORD_IS_EMPTY);
            emptyField = true;
        }
        if (emptyField == false)
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task)
                        {
                            // Credenziali errate
                            if (!task.isSuccessful())
                                errorHandler(Error.Type.INVALID_CREDENTIALS);
                        }
                    });
    }

    // Metodo utilizzato per evidenziare gli errori nella GUI
    private void errorHandler(Error.Type e)
    {
        switch (e)
        {
            case EMAIL_IS_EMPTY:
                inEmail.setErrorEnabled(true);
                inEmail.setError(getResources().getString(R.string.error_email_is_empty));
                break;
            case PASSWORD_IS_EMPTY:
                inPass.setErrorEnabled(true);
                inPass.setError(getResources().getString(R.string.error_password_is_empty));
                break;
            case INVALID_CREDENTIALS:
                inPass.setErrorEnabled(true);
                inPass.setError(getResources().getString(R.string.error_invalid_credentials));
                inPass.getEditText().setText("");
                break;
        }
    }
}
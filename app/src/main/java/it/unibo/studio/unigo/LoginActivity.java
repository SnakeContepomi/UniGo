package it.unibo.studio.unigo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import it.unibo.studio.unigo.main.MainActivity;
import it.unibo.studio.unigo.signup.SignupActivity;
import it.unibo.studio.unigo.utils.Error;

import static it.unibo.studio.unigo.utils.Error.Type.PASS;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener
{
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;



    private TextInputLayout inEmail, inPass;
    private Button btnLogin;
    private TextView txtSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Enable Fullscreen Activity
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);
        initComponents();

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null)
                {
                    // User is signed in
                    if (user.isEmailVerified())
                    {
                        //ToDo: Snackbar!!!
                        //Toast.makeText(getApplicationContext(), "User logged in: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(myIntent);
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Email not verified", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    // User is signed out
                }
            }
        };

        inEmail.getEditText().setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inEmail.isErrorEnabled())
                {
                    resetGUI(inEmail);
                }
                return false;
            }
        });

        inPass.getEditText().setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (inPass.isErrorEnabled())
                {
                    resetGUI(inPass);
                }
                return false;
            }
        });
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
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
                break;
        }
    }

    private void initComponents()
    {
        inEmail = (TextInputLayout) findViewById(R.id.inEmail);
        inPass = (TextInputLayout) findViewById(R.id.inPass);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtSignUp = (TextView) findViewById(R.id.txtSignUp);
    }

    private void login(String email, String password)
    {
        boolean emptyField = false;

        /*mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
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
                        Toast.makeText(getApplicationContext(), "createUserWithEmail:onComplete:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful())
                        {
                            Toast.makeText(getApplicationContext(), "registration failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });*/
        if (inEmail.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.EMAIL);
            emptyField = true;
        }

        if (inPass.getEditText().getText().toString().equals(""))
        {
            errorHandler(Error.Type.PASS);
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
                            {
                                Toast.makeText(getApplicationContext(), "login failed", Toast.LENGTH_SHORT).show();
                                errorHandler(Error.Type.LOGIN);
                            }
                        }
                    });
    }

    private void errorHandler(Error.Type e)
    {
        switch (e)
        {
            case EMAIL:
                inEmail.setErrorEnabled(true);
                inEmail.setError(getResources().getString(R.string.error_email));
                break;
            case PASS:
                inPass.setErrorEnabled(true);
                inPass.setError(getResources().getString(R.string.error_pass));
                break;
            case LOGIN:
                inPass.setErrorEnabled(true);
                inPass.setError(getResources().getString(R.string.error_login));
                inPass.getEditText().setText("");
                break;
        }
    }

    private void resetGUI(TextInputLayout layout)
    {
        layout.setErrorEnabled(false);
        layout.setError(null);
        layout.getEditText().setText("");
    }
}
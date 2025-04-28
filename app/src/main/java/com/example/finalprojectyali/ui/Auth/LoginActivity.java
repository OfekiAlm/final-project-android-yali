package com.example.finalprojectyali.ui.Auth;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectyali.ui.Home.MainActivity;
import com.example.finalprojectyali.R;
import com.google.android.material.textfield.TextInputEditText;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

/**
 This class handles the login process for the user. It provides a simple interface for the user
 to input their login credentials, validates their input and attempts to sign in the user using
 FirebaseAuth. If the sign-in is successful, the user is redirected to the MainActivity. Otherwise,
 appropriate error messages are displayed to the user based on the type of error encountered.
 @author Yali Shem Tov
 */
public class LoginActivity extends AppCompatActivity {

    /** Firebase Authentication instance */
    FirebaseAuth mAuth;

    /** EditText field for email */
    TextInputEditText editTextEmail;

    /** EditText field for password */
    TextInputEditText editTextpassword;

    /**
     * Initializes the activity layout and Firebase Authentication instance.
     * Also, it calls the init method to initialize the layout views.
     *
     * @param savedInstanceState An instance of Bundle class to restore the activity to a previous state if necessary.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        init();
    }

    /**
     * Initializes the views and listeners for the activity.
     */
    private void init(){
        editTextEmail = (TextInputEditText) findViewById(R.id.editTextTextEmailAddress_Register);
        editTextpassword = (TextInputEditText) findViewById(R.id.editTextTextPassword_Register);
        TextView moveToRegister = findViewById(R.id.move_screen);

        moveToRegister.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    /**
     * Validates user input for email and password fields.
     * @return boolean true if input is valid, false otherwise.
     */
    private boolean check_validation_credentials() {
        if(editTextEmail.getText().length() ==0){
            editTextEmail.setError("You haven't typed any credentials");
            editTextEmail.requestFocus();
            return false;
        }
        if(editTextpassword.getText().length() ==0){
            editTextpassword.setError("You haven't typed any credentials");
            editTextpassword.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Attempts to log the user in using the provided email and password.
     * @param view the view that triggered the method.
     */
    public void login(View view){
        boolean validation_credentials_are_valid= check_validation_credentials();
        if(validation_credentials_are_valid) {
            Log.d("AuthData", "Username: " + editTextEmail.getText().toString() + "\n" + "Password: " + editTextpassword.getText().toString());

            mAuth.signInWithEmailAndPassword(editTextEmail.getText().toString(), editTextpassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d("AuthData", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Log.w("AuthData", "signInWithCredential:failure", task.getException());
                    }
                }
            }).addOnFailureListener(e -> {
                if(e instanceof FirebaseAuthInvalidUserException){
                    Toast.makeText(LoginActivity.this, "This User Not Found , Create A New Account", Toast.LENGTH_SHORT).show();
                }
                if(e instanceof FirebaseAuthInvalidCredentialsException){
                    Toast.makeText(LoginActivity.this, "Check your credentials", Toast.LENGTH_SHORT).show();
                }
                if(e instanceof FirebaseNetworkException){
                    Toast.makeText(getApplicationContext(), "Please Check Your internet connection", Toast.LENGTH_SHORT).show();
                }
            });


        }
    }

    /**
     Moves the user to the main activity screen and passes the email as an extra parameter.
     @param email the email of the user
     */
    private void move_screen(String email){
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        String Identifier  = "username_of_user";
        i.putExtra(email,Identifier);
        startActivity(i);
    }

    /**
     Called when the activity is starting. Checks if the user is signed in and updates the UI accordingly.
     */
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            updateUI(currentUser);
        }
    }

    /**
     Updates the user interface after authentication.
     @param currentUser the currently authenticated user
     */
    private void updateUI(FirebaseUser currentUser) {

        Log.d("AuthData","Email is " + currentUser.getEmail());
        move_screen(currentUser.getEmail());
    }
}
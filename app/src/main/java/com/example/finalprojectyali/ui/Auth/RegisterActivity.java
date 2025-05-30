package com.example.finalprojectyali.ui.Auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalprojectyali.Extras.ActivityGuideTracker;
import com.example.finalprojectyali.Models.User;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Home.MainActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.BuildConfig;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Registration screen – lets a new user create an account and upload a profile image.
 */
public class RegisterActivity extends AppCompatActivity {

    // UI
    private TextInputEditText editTextName, editTextEmail, editTextPassword, editTextPhoneNum;
    private FloatingActionButton takePicBtn;
    private CircleImageView circleImageView;

    // Firebase
    private FirebaseAuth mAuth;
    private StorageReference storageReference;

    // Image-handling
    private Bitmap imageBitmap;
    private Uri imageUri;

    // Activity-result launchers
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private AlertDialog.Builder alertDialog;
    private static final String TAG = "AuthData";
    private static final int PERM_REQUEST_CODE = 100;

    /* -------------------------------------------------- lifecycle -------------------------------------------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Register activity-result launchers first
        registerActivityResultLaunchers();

        // 2. Init Firebase + UI
        mAuth = FirebaseAuth.getInstance();
        initViews();

        // 3. Permissions
        askForUserPermissions();

        // 4. Reset onboarding flags for fresh accounts
        new ActivityGuideTracker(this).clearActivitiesStatus();
    }

    /* -------------------------------------------------- init -------------------------------------------------- */

    private void initViews() {
        Drawable d = getDrawable(R.drawable.register_logo_pic);
        imageBitmap = drawableToBitmap(d);

        editTextName = findViewById(R.id.editTextName_Register);
        editTextEmail = findViewById(R.id.editTextTextEmailAddress_Register);
        editTextPassword = findViewById(R.id.editTextTextPassword_Register);
        editTextPhoneNum = findViewById(R.id.editTextTextPhoneNum_Register);

        circleImageView = findViewById(R.id.profile_image);
        takePicBtn = findViewById(R.id.profile_pic_fab);

        editTextPhoneNum.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)}); // Israeli numbers

        TextView moveToLogin = findViewById(R.id.move_screen);

        alertDialog = new AlertDialog.Builder(this);
        takePicBtn.setOnClickListener(v -> showImageSourceDialog());
        moveToLogin.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    /* -------------------------------------------------- image source dialog -------------------------------------------------- */

    private void showImageSourceDialog() {
        alertDialog.setTitle("Provide Image")
                .setMessage("Choose how to add a profile picture")
                .setCancelable(false)
                .setPositiveButton("Camera", (d, i) -> {
                    d.dismiss();
                    if (hasCameraPermission()) showImagePickerFromCamera();
                    else askForUserPermissions();
                })
                .setNegativeButton("Gallery", (d, i) -> {
                    d.dismiss();
                    pickImageLauncher.launch("image/*");
                })
                .setNeutralButton("Cancel", (d, i) -> d.dismiss())
                .create()
                .show();
    }

    /* -------------------------------------------------- launchers -------------------------------------------------- */

    private void registerActivityResultLaunchers() {

        takePictureLauncher =
                registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                    if (result) {
                        circleImageView.setImageURI(imageUri);
                        imageBitmap = BitmapFactory.decodeFile(imageUri.getPath());
                    }
                });

        pickImageLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        circleImageView.setImageURI(uri);
                        try {
                            imageBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    /* -------------------------------------------------- camera / gallery helpers -------------------------------------------------- */

    private void showImagePickerFromCamera() {
        try {
            File photoFile = createImageFile();
            String authority = getApplicationContext().getPackageName() + ".provider";

            imageUri = FileProvider.getUriForFile(
                    this,
                    authority,
                    photoFile);
            takePictureLauncher.launch(imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Couldn't create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", dir);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();

        Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    /* -------------------------------------------------- registration flow -------------------------------------------------- */

    public void registerNewUser(View view) {

        if (!credentialsValid()) return;

        String userPhone = editTextPhoneNum.getText().toString();
        User userObj = new User(
                userPhone,
                editTextName.getText().toString(),
                editTextEmail.getText().toString());

        String email = editTextEmail.getText().toString();
        String pass = editTextPassword.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(uid)
                                .setValue(userObj)
                                .addOnCompleteListener(t -> {
                                    if (t.isSuccessful())
                                        Log.d(TAG, "User saved in DB");
                                });

                        uploadImageToFirebaseStorage(imageBitmap);
                        updateUI();

                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }

    private boolean credentialsValid() {
        if (editTextName.length() == 0) return error(editTextName, "Required");
        if (editTextEmail.length() == 0) return error(editTextEmail, "Required");
        if (editTextPassword.length() == 0) return error(editTextPassword, "Required");
        if (editTextPhoneNum.length() != 9) return error(editTextPhoneNum, "9 digits");
        return true;
    }

    private boolean error(TextInputEditText field, String msg) {
        field.setError(msg);
        field.requestFocus();
        return false;
    }

    private void handleAuthError(Exception e) {
        try {
            throw e;
        } catch (FirebaseAuthWeakPasswordException ex) {
            error(editTextPassword, "Password too short – it must be at least 6 characters long.");
        } catch (FirebaseAuthInvalidCredentialsException ex) {
            error(editTextEmail, "Invalid email");
        } catch (FirebaseAuthUserCollisionException ex) {
            error(editTextEmail, "Email in use");
        } catch (FirebaseNetworkException ex) {
            Toast.makeText(this, "No Internet", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_LONG).show();
        }
    }

    /* -------------------------------------------------- Firebase Storage -------------------------------------------------- */

    private void uploadImageToFirebaseStorage(Bitmap bmp) {
        if (bmp == null) return;
        storageReference = FirebaseStorage.getInstance().getReference();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        byte[] data = stream.toByteArray();

        StorageReference imgRef = storageReference
                .child("profile-images/" + FirebaseAuth.getInstance().getCurrentUser().getUid());

        imgRef.putBytes(data)
                .addOnSuccessListener(t -> Log.d(TAG, "Image uploaded"))
                .addOnFailureListener(e -> Log.e(TAG, "Upload failed", e));
    }

    /* -------------------------------------------------- permissions -------------------------------------------------- */

    private void askForUserPermissions() {
        if (hasCameraPermission()) return;

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERM_REQUEST_CODE);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == PERM_REQUEST_CODE && hasCameraPermission())
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
    }

    /* -------------------------------------------------- navigation -------------------------------------------------- */

    private void updateUI() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

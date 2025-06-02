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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalprojectyali.Extras.ActivityGuideTracker;
import com.example.finalprojectyali.Extras.GuiderDialog;
import com.example.finalprojectyali.Models.User;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Home.MainActivity;
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

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Registration screen – lets a new user create an account and upload a profile image.
 */
public class RegisterActivity extends AppCompatActivity {

    /* -------------------- constants -------------------- */
    private static final String TAG = "AuthData";
    private static final int    CAMERA_PERM_CODE = 100;
    private static final String NOTIF_PERMISSION = Manifest.permission.POST_NOTIFICATIONS;

    /* -------------------- UI -------------------- */
    private TextInputEditText editTextName, editTextEmail, editTextPassword, editTextPhoneNum;
    private FloatingActionButton takePicBtn;
    private CircleImageView circleImageView;

    /* -------------------- Firebase -------------------- */
    private FirebaseAuth       mAuth;
    private StorageReference   storageReference;

    /* -------------------- image handling -------------------- */
    private Bitmap imageBitmap;
    private Uri    imageUri;

    /* -------------------- activity-result launchers -------------------- */
    private ActivityResultLauncher<Uri>    takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> notifPermLauncher;   // <-- NEW

    private AlertDialog.Builder alertDialog;

    /* -------------------------------------------------- lifecycle -------------------------------------------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerActivityResultLaunchers();   // #1

        mAuth = FirebaseAuth.getInstance();  // #2
        initViews();

        askForCameraPermission();            // #3 camera
        ensureNotificationPermission();      // #3 notifications (Android 13 +)

        new ActivityGuideTracker(this).clearActivitiesStatus(); // #4 onboarding reset

        new GuiderDialog(this, "RegisterActivity",
                "Create your account by filling in your details and adding a profile picture.").startDialog();
    }

    /* -------------------------------------------------- init -------------------------------------------------- */

    private void initViews() {
        Drawable d = getDrawable(R.drawable.register_logo_pic);
        imageBitmap = drawableToBitmap(d);

        editTextName     = findViewById(R.id.editTextName_Register);
        editTextEmail    = findViewById(R.id.editTextTextEmailAddress_Register);
        editTextPassword = findViewById(R.id.editTextTextPassword_Register);
        editTextPhoneNum = findViewById(R.id.editTextTextPhoneNum_Register);
        editTextPhoneNum.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)}); // Israeli phone length

        circleImageView = findViewById(R.id.profile_image);
        takePicBtn      = findViewById(R.id.profile_pic_fab);

        TextView moveToLogin = findViewById(R.id.move_screen);

        alertDialog = new AlertDialog.Builder(this);
        takePicBtn.setOnClickListener(v -> showImageSourceDialog());
        moveToLogin.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    /* -------------------------------------------------- permission helpers -------------------------------------------------- */

    /** Camera permission */
    private void askForCameraPermission() {
        if (hasCameraPermission()) return;

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERM_CODE);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Notification permission – only needed on API 33 + */
    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (ContextCompat.checkSelfPermission(this, NOTIF_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            return; // already granted
        }
        // Request via launcher (shows system dialog)
        notifPermLauncher.launch(NOTIF_PERMISSION);
    }

    /* Handle camera permission result (notification is handled via launcher) */
    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == CAMERA_PERM_CODE && hasCameraPermission())
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
    }

    /* -------------------------------------------------- activity-result launchers -------------------------------------------------- */

    private void registerActivityResultLaunchers() {

        /* Take-picture */
        takePictureLauncher =
                registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                    if (result) {
                        circleImageView.setImageURI(imageUri);
                        imageBitmap = BitmapFactory.decodeFile(imageUri.getPath());
                    }
                });

        /* Pick from gallery */
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

        /* NEW – notification permission */
        notifPermLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "App can't show reminders unless you allow notifications",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /* -------------------------------------------------- image source dialog -------------------------------------------------- */

    private void showImageSourceDialog() {
        alertDialog.setTitle("Provide Image")
                .setMessage("Choose how to add a profile picture")
                .setCancelable(false)
                .setPositiveButton("Camera", (d, i) -> {
                    d.dismiss();
                    if (hasCameraPermission()) showImagePickerFromCamera();
                    else askForCameraPermission();
                })
                .setNegativeButton("Gallery", (d, i) -> {
                    d.dismiss();
                    pickImageLauncher.launch("image/*");
                })
                .setNeutralButton("Cancel", (d, i) -> d.dismiss())
                .create()
                .show();
    }

    /* -------------------------------------------------- camera / gallery helpers -------------------------------------------------- */

    private void showImagePickerFromCamera() {
        try {
            File photoFile = createImageFile();
            String authority = getApplicationContext().getPackageName() + ".provider";

            imageUri = FileProvider.getUriForFile(this, authority, photoFile);
            takePictureLauncher.launch(imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Couldn't create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName  = "JPEG_" + timeStamp + "_";
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", dir);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();

        Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
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
        String pass  = editTextPassword.getText().toString();

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
            error(editTextPassword, "Password too short – at least 6 characters");
        } catch (FirebaseAuthInvalidCredentialsException ex) {
            error(editTextEmail, "Invalid email");
        } catch (FirebaseAuthUserCollisionException ex) {
            error(editTextEmail, "Email already in use");
        } catch (FirebaseNetworkException ex) {
            Toast.makeText(this, "No Internet connection", Toast.LENGTH_LONG).show();
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

    /* -------------------------------------------------- navigation -------------------------------------------------- */

    private void updateUI() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

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

import com.example.finalprojectyali.ui.Home.MainActivity;

import com.example.finalprojectyali.Extras.ActivityGuideTracker;
import com.example.finalprojectyali.Models.User;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Home.MainActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.BuildConfig;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 The RegisterAct class represents the activity that allows the user to register
 for a new account. It provides UI elements to capture user inputs such as email,
 password, phone number, and profile picture. It uses Firebase Authentication and
 Firebase Storage to authenticate the user and store the profile picture.
 The class includes methods to validate user inputs, select an image from the camera
 or the device's gallery, and register the user in Firebase. It also includes methods
 to handle the result of the image selection, including handling camera permissions,
 creating an image file, and setting the profile picture ImageView.
 This class extends the AppCompatActivity class and overrides its onCreate method
 to initialize the UI elements and Firebase objects. It also implements other helper
 methods to perform specific tasks such as registering for activity results, converting
 a drawable to a bitmap, and resetting the activity guide tracker for new users.

 @author Yali Shem Tov
 */
public class RegisterActivity extends AppCompatActivity {

    /**
     Input fields for email, password, and phone number.
     */
    TextInputEditText editTextName,editTextEmail, editTextPassword, editTextPhoneNum;

    /**
     The Firebase authentication object.
     */
    private FirebaseAuth mAuth;

    /**
     The Firebase storage reference object for storing the user's profile picture.
     */
    private StorageReference storageReference;

    /**
     The FAB button for taking a picture.
     */
    private FloatingActionButton takePicBtn;

    /**
     The ImageView for the profile picture.
     */
    private CircleImageView circleImageView;

    /**
     The bitmap object for the profile picture.
     */
    private Bitmap imageBitmap;

    /**
     The alert dialog for selecting an image source.
     */
    private AlertDialog.Builder alertDialog;

    /**
     The URI for the image file.
     */
    private Uri imageUri;

    /**
     The activity result launcher for taking a picture.
     */
    private ActivityResultLauncher<Uri> takePictureLauncher;

    /**
     The activity result launcher for selecting an image from the gallery.
     */
    private ActivityResultLauncher<String> pickImageLauncher;

    private final String TAG = "AuthData"; //Used for logging.

    /**

     Initializes the UI elements and Firebase objects.

     Also initializes camera and local storage methods.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Reset activity guide tracker for new users
        ActivityGuideTracker agt = new ActivityGuideTracker(this);
        agt.clearActivitiesStatus();

        //FirebaseAuth object
        mAuth = FirebaseAuth.getInstance();

        init();

        askForUserPermissions();


    }

    /**
     Initializes the input fields, FAB button, ImageView, and alert dialog.

     Also sets the default profile picture and sets a listener for the "move to login" text.
     */
    private void init(){
        Drawable d = getDrawable(R.drawable.register_logo_pic);
        imageBitmap = drawableToBitmap(d);

        editTextName = findViewById(R.id.editTextTextEmailAddress_Register);

        editTextEmail = findViewById(R.id.editTextTextEmailAddress_Register);
        editTextPassword = findViewById(R.id.editTextTextPassword_Register);
        circleImageView = findViewById(R.id.profile_image);
        takePicBtn = findViewById(R.id.profile_pic_fab);

        editTextPhoneNum = findViewById(R.id.editTextTextPhoneNum_Register);
        InputFilter[] lengthFilter = new InputFilter []{new InputFilter.LengthFilter(9)}; // Restrict to 10 characters, for israeli numbers only
        editTextPhoneNum.setFilters(lengthFilter);

        TextView moveToLogin = findViewById(R.id.move_screen);
        alertDialog = new AlertDialog.Builder(this);

        takePicBtn.setOnClickListener(view -> {
            //Show the user the selection dialog.
            alertDialog
                    .setMessage("Choose your way to provide image")
                    .setTitle("Provide Image")
                    .setCancelable(false);

            alertDialog.setPositiveButton("Camera", (dialogInterface, i) -> {
                dialogInterface.cancel();
                showImagePickerFromCamera();
            });
            alertDialog.setNegativeButton("Storage", (dialogInterface, i) -> {
                dialogInterface.cancel();
                showImagePickerFromGallery();
            });
            alertDialog.setNeutralButton("Cancel", (dialogInterface, i) -> {
                dialogInterface.dismiss();
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        });

        moveToLogin.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    /**
     * This method checks if all the registration credentials are valid.
     * It checks if the email and password fields are not empty and if the phone
     * number is exactly 10 digits long.
     *
     * @return true if all the credentials are valid, false otherwise.
     */
    private boolean check_validation_credentials() {
        if(editTextName.getText().length() == 0){
            editTextName.setError("You haven't typed any credentials");
            editTextName.requestFocus();
            return false;
        }
        if(editTextEmail.getText().length() == 0){
            editTextEmail.setError("You haven't typed any credentials");
            editTextEmail.requestFocus();
            return false;
        }
        if(editTextPassword.getText().length() ==0){
            editTextPassword.setError("You haven't typed any credentials");
            editTextPassword.requestFocus();
            return false;
        }
        if (editTextPhoneNum.getText().length() != 9){
            editTextPhoneNum.setError("Phone number should be 9 digits");
            editTextPhoneNum.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * This method shows a dialog to allow the user to choose to take a picture with the device's camera
     * and starts an activity to capture the image. It uses the ActivityResultLauncher API to handle
     * the result of the activity.
     */
    private void showImagePickerFromCamera() {
        try {
            // Create an image file to store the camera photo
            File photoFile = createImageFile();
            if (photoFile != null) {
                // Create a file URI to pass to the camera app
                imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", photoFile);

                // Launch the camera app
                takePictureLauncher.launch(imageUri);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Error occurred while creating the photo file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method shows a dialog to allow the user to choose an image from the device's gallery
     * and starts an activity to pick the image. It uses the ActivityResultLauncher API to handle
     * the result of the activity.
     */
    private void showImagePickerFromGallery() {
        pickImageLauncher.launch("image/*");
    }

    /**

     Sets up the take picture and pick image launchers to handle the user taking a photo
     with the camera or selecting an image from their device's gallery.
     If the user takes a photo with the camera, the image is displayed in the CircleImageView and
     stored as a bitmap.
     If the user cancels or fails the camera operation, the gallery is opened for image selection.
     If the user selects an image from the gallery, the image is displayed in the CircleImageView and
     stored as a bitmap.
     If the user cancels the image selection from gallery, the gallery is opened again for image selection until the user chooses.
     */
    private void RegisterOnResult_and_takePicFromCamera(){
        try {
            takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result) {
                    // Camera photo was taken successfully
                    circleImageView.setImageURI(imageUri);
                    imageBitmap = BitmapFactory.decodeFile(imageUri.getPath());
                } else {
                    // Camera operation was canceled or failed
                    alertDialog.show();
                    //let user re-select
                }
            });
        }catch (IllegalStateException e) {
            // Handle the exception
            e.printStackTrace(); // or use a logging mechanism
            // Perform any necessary cleanup or error handling
        }

        try {
            pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
                if (result != null) {
                    // Image was picked successfully from gallery
                    circleImageView.setImageURI(result);
                    try {
                        imageBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(result));
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // Image picking was canceled
                    alertDialog.show();
                    //let user re-select
                }
            });
        }catch (IllegalStateException e) {
            // Handle the exception
            e.printStackTrace(); // or use a logging mechanism
            // Perform any necessary cleanup or error handling
        }


    }

    /**
     * This method converts a Drawable object to a Bitmap object.
     *
     * @param drawable the Drawable object to convert.
     * @return a Bitmap object representing the converted Drawable object.
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * This method creates a unique image file name based on the current timestamp.
     *
     * @return a File object representing the created image file.
     * @throws IOException if the file creation fails.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**

     Registers a new user with Firebase Authentication and adds the user's information to the Firebase Realtime Database.

     Validates the input credentials entered by the user, and displays an error message for any invalid input.

     If registration is successful, the user's profile picture is uploaded to Firebase Storage, and the user is taken to the main screen of the app.

     @param view The view that triggers the registration process, typically a button.
     */
    public void registerNewUser(View view) {
        String userPhone = editTextPhoneNum.getText().toString();
        User my_user = new User(
                userPhone,
                editTextName.getText().toString(),
                editTextEmail.getText().toString()
        );
        String userEmail = editTextEmail.getText().toString();
        String userPassword = editTextPassword.getText().toString();
        if(check_validation_credentials()){
            mAuth.createUserWithEmailAndPassword(userEmail,userPassword)
                    .addOnCompleteListener(RegisterActivity.this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");

                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(my_user).addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d(TAG, "AddToDatabase:success");
                                            Toast.makeText(getApplicationContext(), "YES", Toast.LENGTH_LONG).show();
                                        } else {
                                            Log.d(TAG, "AddToDatabase:failure");
                                            Toast.makeText(getApplicationContext(), "NO", Toast.LENGTH_LONG).show();
                                        }
                                    });

                            uploadImageToFirebaseStorage(imageBitmap);

                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());

                            try {
                                throw task.getException();
                            } catch(FirebaseAuthWeakPasswordException e) {
                                editTextPassword.setError("Password is too weak");
                                editTextPassword.requestFocus();
                            } catch(FirebaseAuthInvalidCredentialsException e) {
                                editTextEmail.setError("Email is invalid");
                                editTextEmail.requestFocus();
                            }catch(FirebaseAuthUserCollisionException e) {
                                editTextEmail.setError("This email is in use");
                                editTextEmail.requestFocus();
                            }catch (FirebaseNetworkException e){
                                Toast.makeText(getApplicationContext(),"Please Check Your internet connection",Toast.LENGTH_LONG).show();
                            }
                            catch(Exception e) {
                                Toast.makeText(getApplicationContext(),"Some of the fields are not valid",Toast.LENGTH_LONG).show();
                            }
                        }
                    });

        }
    }

    /**

     Uploads the given bitmap image to Firebase Storage, compressing it and converting it to bytes first.

     The image is saved in the "profile-images" folder with the filename as the current user's ID.

     A Toast message is displayed if the upload is successful.

     @param bitmap The Bitmap image to upload to Firebase Storage
     */
    private void uploadImageToFirebaseStorage(Bitmap bitmap){
        storageReference = FirebaseStorage.getInstance().getReference();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 5, stream);
        byte[] data = stream.toByteArray();
        //\\
        StorageReference imageRef = storageReference.child("profile-images/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
        Task<Uri> urlTask = imageRef.putBytes(data).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            if (task.isSuccessful()) {
                Toast.makeText(this,"Image uploaded to database",Toast.LENGTH_LONG).show();
            } else {
                // Handle failures
                // ...
            }
            return imageRef.getDownloadUrl();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                // Permissions are granted
                // Your code logic here
                RegisterOnResult_and_takePicFromCamera();
            } else {
                // Permissions are denied
                // Handle the denied permission case
                Toast.makeText(this,"Please accept the camera and storage permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void askForUserPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted, request them
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 100);
        } else {
            // Permissions are already granted

            // Register activity result launchers and take a picture from camera
            RegisterOnResult_and_takePicFromCamera();
        }
    }

    /**

     Redirects the user to the main activity screen when successful registration occurs.
     */
    private void updateUI() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

}
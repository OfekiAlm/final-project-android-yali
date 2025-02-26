package com.example.finalprojectyali.ui.Auth;

import androidx.appcompat.app.AppCompatActivity;

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
}

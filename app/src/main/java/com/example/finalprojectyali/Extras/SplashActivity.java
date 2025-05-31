package com.example.finalprojectyali.Extras;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Auth.LoginActivity;
import com.example.finalprojectyali.ui.Home.MainActivity;

public class SplashActivity extends AppCompatActivity {

    /**
     The ImageView displaying the splash screen animation.
     */
    private ImageView splashImage;


    /**
     Called when the activity is starting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Always use dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashImage = findViewById(R.id.splash_image_imgv);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.cart_splash);
        splashImage.startAnimation(animation);

        // Wait for the animation to finish and start the main activity
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
}
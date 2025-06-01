package com.example.finalprojectyali.Extras;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        // Hide status bar for immersive experience
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        splashImage = findViewById(R.id.splash_image_imgv);

        // Add smooth fade-in animation for the whole content
        View mainContent = findViewById(android.R.id.content);
        mainContent.setAlpha(0f);
        mainContent.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(200)
                .start();

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.cart_splash);
        splashImage.startAnimation(animation);

        // Wait for the animation to finish and start the main activity
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Add a slight delay for better UX
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Smooth transition to next activity
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }, 500);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
}
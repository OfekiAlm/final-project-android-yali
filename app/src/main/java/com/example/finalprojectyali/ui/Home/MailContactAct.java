package com.example.finalprojectyali.ui.Home;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalprojectyali.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.finalprojectyali.Extras.GuiderDialog;

/**
 The MailContactAct class is an activity that allows the user to send an email to a specified recipient.
 It contains a title and body for the email, as well as a submit button that opens the email client to send the email.
 @author Ofek Almog
 */
public class MailContactAct extends AppCompatActivity {

    /** TextViews to display the email title and body. */
    TextView emailTitleTv,emailBodyTv;

    /** FloatingActionButton to submit the email with enhanced styling. */
    FloatingActionButton submitMail;

    /**
     Initializes the activity and sets up the submit button to send the email when clicked.
     @param savedInstanceState the saved state of the activity, or null if there is no saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail_contact);
        
        // Hide status bar for immersive experience
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        
        init();
        setupAnimations();

        new GuiderDialog(this, "MailContactAct",
                "Send us your feedback, questions, or suggestions. Fill in the subject and message, then tap the send button.").startDialog();

        submitMail.setOnClickListener(view -> {
            // Add button press animation
            view.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();

            // Validate input before sending
            String subject = emailTitleTv.getText().toString().trim();
            String message = emailBodyTv.getText().toString().trim();
            
            if (subject.isEmpty()) {
                Toast.makeText(this, "Please enter a subject", Toast.LENGTH_SHORT).show();
                emailTitleTv.requestFocus();
                return;
            }
            
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                emailBodyTv.requestFocus();
                return;
            }

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL,new String[]{"yalishemtov1234@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message);
            emailIntent.setType("text/html");
            
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email Using: "));
                Toast.makeText(this, "Opening email client...", Toast.LENGTH_SHORT).show();
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     Initializes the UI components of the activity.
     */
    private void init(){
        submitMail = findViewById(R.id.fab_contact);
        emailTitleTv = findViewById(R.id.title_contact_et);
        emailBodyTv = findViewById(R.id.task_desc_et);
    }
    
    /**
     Sets up smooth entrance animations for UI elements.
     */
    private void setupAnimations() {
        // Fade in the main content
        View headerCard = findViewById(R.id.header_card);
        View mainCard = findViewById(R.id.main_card);
        
        headerCard.setAlpha(0f);
        mainCard.setAlpha(0f);
        submitMail.setAlpha(0f);
        
        // Staggered animations
        headerCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(200)
                .start();
                
        mainCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(400)
                .start();
                
        submitMail.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(600)
                .start();
    }
}
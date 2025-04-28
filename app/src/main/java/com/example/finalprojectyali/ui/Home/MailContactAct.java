package com.example.finalprojectyali.ui.Home;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.finalprojectyali.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 The MailContactAct class is an activity that allows the user to send an email to a specified recipient.
 It contains a title and body for the email, as well as a submit button that opens the email client to send the email.
 @author Ofek Almog
 */
public class MailContactAct extends AppCompatActivity {

    /** TextViews to display the email title and body. */
    TextView emailTitleTv,emailBodyTv;

    /** FloatingActionButton to submit the email. */
    FloatingActionButton submitMail;

    /**
     Initializes the activity and sets up the submit button to send the email when clicked.
     @param savedInstanceState the saved state of the activity, or null if there is no saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail_contact);
        init();

        submitMail.setOnClickListener(view -> {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL,new String[]{"ofekalm100@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitleTv.getEditableText().toString()); //title
            emailIntent.putExtra(Intent.EXTRA_TEXT, emailBodyTv.getEditableText().toString()); // description
            emailIntent.setType("text/html");
            startActivity(Intent.createChooser(emailIntent,
                    "Send Email Using: "));
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
}
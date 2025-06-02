package com.example.finalprojectyali.ui.Home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.finalprojectyali.Extras.EventRepository;
import com.example.finalprojectyali.Extras.GuiderDialog;
import com.example.finalprojectyali.Models.Event;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.databinding.ActivityAddEventBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private ActivityAddEventBinding binding;
    private Calendar when = Calendar.getInstance();
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    private boolean isCreating = false;

    @Override
    protected void onCreate(@Nullable Bundle saved) {
        // Enable shared element transitions
        setEnterSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        getWindow().setSharedElementEnterTransition(buildContainerTransform());
        getWindow().setSharedElementReturnTransition(buildContainerTransform());
        
        super.onCreate(saved);
        binding = ActivityAddEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Make status bar transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat windowInsetsController = 
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        /* Toolbar */
        setSupportActionBar(binding.addEventToolbar);
        binding.addEventToolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initial setup
        updateDateTimeLabel();
        setupDateTimePickers();
        setupFab();
        
        // Animate cards on entry
        animateCardsOnEntry();

        new GuiderDialog(this, "AddEventActivity",
                "Fill in the event details, set the date and time, and create your new event.").startDialog();
    }

    private void animateCardsOnEntry() {
        // Get all cards (assuming they have IDs)
        View[] cards = {
            findViewById(R.id.event_details_card),
            findViewById(R.id.date_time_card),
            findViewById(R.id.location_card)
        };
        
        // Animate each card with staggered delays
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                cards[i].setAlpha(0f);
                cards[i].setTranslationY(100f);
                
                cards[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(i * 100L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            }
        }
        
        // Animate FAB with overshoot
        binding.saveEventFab.setScaleX(0f);
        binding.saveEventFab.setScaleY(0f);
        binding.saveEventFab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(new OvershootInterpolator())
            .start();
    }

    private void setupDateTimePickers() {
        /* Date picker with animation */
        binding.pickDateBtn.setOnClickListener(v -> {
            // Pulse animation on button click
            pulseView(v);
            
            DatePickerDialog dp = new DatePickerDialog(this,
                    R.style.DatePickerTheme,
                    (DatePicker view, int y, int m, int d) -> {
                        when.set(Calendar.YEAR, y);
                        when.set(Calendar.MONTH, m);
                        when.set(Calendar.DAY_OF_MONTH, d);
                        updateDateTimeLabel();
                        
                        // Animate the preview update
                        animatePreviewUpdate();
                    },
                    when.get(Calendar.YEAR), when.get(Calendar.MONTH), when.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        /* Time picker with animation */
        binding.pickTimeBtn.setOnClickListener(v -> {
            // Pulse animation on button click
            pulseView(v);
            
            TimePickerDialog tp = new TimePickerDialog(this,
                    R.style.TimePickerTheme,
                    (TimePicker view, int h, int min) -> {
                        when.set(Calendar.HOUR_OF_DAY, h);
                        when.set(Calendar.MINUTE, min);
                        updateDateTimeLabel();
                        
                        // Animate the preview update
                        animatePreviewUpdate();
                    },
                    when.get(Calendar.HOUR_OF_DAY), when.get(Calendar.MINUTE), true);
            tp.show();
        });
    }

    private void setupFab() {
        /* Save with animation */
        binding.saveEventFab.setOnClickListener(v -> {
            if (!isCreating) {
                createEvent(v);
            }
        });
    }

    private void updateDateTimeLabel() {
        binding.datetimePreview.setText(fmt.format(when.getTime()));
    }

    private void animatePreviewUpdate() {
        // Fade out and in animation
        binding.datetimePreview.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction(() -> {
                updateDateTimeLabel();
                binding.datetimePreview.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start();
            })
            .start();
    }

    private void pulseView(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    private void createEvent(View v) {
        String name = binding.eventNameEt.getText().toString().trim();
        String desc = binding.eventDescEt.getText().toString().trim();
        String loc = binding.eventLocationEt.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) {
            showError("Please enter an event name");
            shakeView(binding.eventNameEt);
            return;
        }
        if (TextUtils.isEmpty(desc)) {
            showError("Please add a description");
            shakeView(binding.eventDescEt);
            return;
        }
        if (TextUtils.isEmpty(loc)) {
            showError("Please specify a location");
            shakeView(binding.eventLocationEt);
            return;
        }

        // Start loading animation
        isCreating = true;
        animateCreatingEvent();

        EventRepository.createEvent(name, desc, new Date(when.getTimeInMillis()), loc,
                e -> {
                    // Success animation
                    animateSuccess(() -> {
                        Toast.makeText(this, "Event created successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    });
                },
                err -> {
                    isCreating = false;
                    resetFab();
                    showError("Failed: " + err.getMessage());
                });
    }

    private void animateCreatingEvent() {
        // Rotate FAB icon
        binding.saveEventFab.animate()
            .rotation(360f)
            .setDuration(1000)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                if (isCreating) {
                    binding.saveEventFab.setRotation(0f);
                    animateCreatingEvent(); // Loop
                }
            })
            .start();
        
        // Change FAB text
        binding.saveEventFab.setText("Creating...");
        binding.saveEventFab.setEnabled(false);
    }

    private void animateSuccess(Runnable onComplete) {
        binding.saveEventFab.setIconResource(R.drawable.ic_baseline_done);
        binding.saveEventFab.setText("Success!");
        binding.saveEventFab.setBackgroundTintList(
            getResources().getColorStateList(R.color.success_color));
        
        // Circular reveal animation
        int cx = binding.saveEventFab.getWidth() / 2;
        int cy = binding.saveEventFab.getHeight() / 2;
        float finalRadius = Math.max(binding.saveEventFab.getWidth(), binding.saveEventFab.getHeight());
        
        new Handler().postDelayed(onComplete, 1000);
    }

    private void resetFab() {
        binding.saveEventFab.animate().rotation(0f).setDuration(300).start();
        binding.saveEventFab.setText("Create Event");
        binding.saveEventFab.setEnabled(true);
        binding.saveEventFab.setIconResource(R.drawable.ic_baseline_check_24);
        binding.saveEventFab.setBackgroundTintList(
            getResources().getColorStateList(R.color.accent_color));
    }

    private void shakeView(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        shake.setDuration(500);
        shake.setInterpolator(new AccelerateDecelerateInterpolator());
        shake.start();
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getResources().getColor(R.color.error_color))
            .setTextColor(Color.WHITE)
            .show();
    }

    private com.google.android.material.transition.platform.MaterialContainerTransform buildContainerTransform() {
        com.google.android.material.transition.platform.MaterialContainerTransform transform = 
            new com.google.android.material.transition.platform.MaterialContainerTransform();
        transform.setDuration(300L);
        transform.setScrimColor(Color.TRANSPARENT);
        transform.setAllContainerColors(getResources().getColor(R.color.surface_dark));
        return transform;
    }

    @Override
    public void onBackPressed() {
        // Animate cards out before finishing
        View[] cards = {
            findViewById(R.id.event_details_card),
            findViewById(R.id.date_time_card),
            findViewById(R.id.location_card)
        };
        
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                cards[i].animate()
                    .alpha(0f)
                    .translationY(-50f)
                    .setDuration(300)
                    .setStartDelay(i * 50L)
                    .setInterpolator(new AnticipateInterpolator())
                    .start();
            }
        }
        
        binding.saveEventFab.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(300)
            .withEndAction(() -> super.onBackPressed())
            .start();
    }
}
package com.example.finalprojectyali.ui.Home.Fragments;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.finalprojectyali.R;

import java.util.Calendar;
import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {
    EditTextPreference editTextPreference;
    MultiSelectListPreference multiSelectListPreference;
    private EditTextPreference notificationTimePreference;
    private CheckBoxPreference notificationEnabledPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        editTextPreference = findPreference("time_selector");
        multiSelectListPreference = findPreference("activities_multiselect_list_preference");

        // Initialize preferences
        notificationTimePreference = findPreference("time_selector");
        notificationEnabledPreference = findPreference("want_notifications");
        
        // Set up notification time preference
        if (notificationTimePreference != null) {
            // Set default time if not set
            if (notificationTimePreference.getText() == null || notificationTimePreference.getText().isEmpty()) {
                notificationTimePreference.setText("09:00"); // Default to 9 AM
            }
            
            // Update summary to show current time
            notificationTimePreference.setSummaryProvider(preference -> {
                String time = notificationTimePreference.getText();
                return time != null ? "Notification time: " + time : "Tap to select time";
            });
            
            // Save preference when changed
            notificationTimePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String time = (String) newValue;
                // You can add validation here if needed
                return true;
            });
        }
        
        // Set up notification enabled preference
        if (notificationEnabledPreference != null) {
            notificationEnabledPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (boolean) newValue;
                // Here you can enable/disable the notification scheduling
                if (enabled) {
                    scheduleNotifications();
                } else {
                    cancelNotifications();
                }
                return true;
            });
        }
    }

    /**
     * Opens the time picker dialog.
     * @param view The view that triggered the method.
     */
    public void popTimePicker(View view) {
        // Get current time from preference or use default
        String currentTime = notificationTimePreference.getText();
        int hour = 9; // Default hour
        int minute = 0; // Default minute
        
        if (currentTime != null && currentTime.contains(":")) {
            String[] parts = currentTime.split(":");
            try {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // Use defaults if parsing fails
            }
        }
        
        TimePickerDialog.OnTimeSetListener onTimeSetListener = (timePicker, selectedHour, selectedMinute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            notificationTimePreference.setText(time);
            
            // If notifications are enabled, reschedule with new time
            if (notificationEnabledPreference.isChecked()) {
                scheduleNotifications();
            }
        };
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            getContext(), 
            onTimeSetListener, 
            hour, 
            minute, 
            true // 24-hour format
        );
        
        timePickerDialog.setTitle("Select Notification Time");
        timePickerDialog.show();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if ("time_selector".equals(preference.getKey())) {
            popTimePicker(getView());
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    /**
     * Schedule notifications based on current settings
     */
    private void scheduleNotifications() {
        // TODO: Implement notification scheduling logic
        // This should schedule notifications for events 1 day before at the selected time
        // You'll need to use AlarmManager or WorkManager
    }

    /**
     * Cancel all scheduled notifications
     */
    private void cancelNotifications() {
        // TODO: Implement notification cancellation logic
        // This should cancel all scheduled notifications
    }
}
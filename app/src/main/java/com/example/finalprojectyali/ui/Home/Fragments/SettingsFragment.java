package com.example.finalprojectyali.ui.Home.Fragments;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.finalprojectyali.R;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {
    EditTextPreference editTextPreference;
    MultiSelectListPreference multiSelectListPreference;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        editTextPreference = findPreference("time_selector");
        multiSelectListPreference = findPreference("activities_multiselect_list_preference");
        //multiSelectListPreference.setEntries(R.array.activities);

        //multiSelectListPreference.setEntryValues(R.array.activities_values);
    }

    /**
     * Opens the time picker dialog.
     * @param view The view that triggered the method.
     */
    public void popTimePicker(View view) {
        TimePickerDialog.OnTimeSetListener onTimeSetListener = (timePicker, selectedHour, selectedMinute) -> editTextPreference.setText(String.format(Locale.getDefault(), "%02d:%02d",selectedHour, selectedMinute));
        //TimePickerDialog.OnCancelListener onCancelListener = (timePicker)  -> editTextPreference.performClick();

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), onTimeSetListener, 1,0, true);
        //timePickerDialog.setOnCancelListener(onCancelListener);
        timePickerDialog.setTitle("Select Task Duration (HH:MM)");
        timePickerDialog.show();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        //super.onDisplayPreferenceDialog(preference);
        switch (preference.getKey()){
            case "time_selector":
                popTimePicker(getView());
                break;
            case "activities_multiselect_list_preference":
                super.onDisplayPreferenceDialog(preference);
                break;
        }
    }
}
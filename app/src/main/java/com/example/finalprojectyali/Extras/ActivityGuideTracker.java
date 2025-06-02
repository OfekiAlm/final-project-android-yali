package com.example.finalprojectyali.Extras;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A utility class for tracking which activities or fragments have been visited
 * by the user in a mobile app.
 * @author Yali Shem Tov
 */
public class ActivityGuideTracker {

    /**
     The name of the shared preferences file where the activity/fragment visit status is saved.
     */
    private static final String PREFS_NAME = "activity_tracker_prefs";

    /**
     The shared preferences object used to store and retrieve activity/fragment visit status.
     */
    private SharedPreferences mPrefs;

    /**
     * Constructs a new ActivityGuideTracker instance.
     *
     * @param context The application context.
     */
    public ActivityGuideTracker(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns true if the given activity or fragment has been visited by the user before,
     * false otherwise.
     *
     * @param activityOrFragmentName The name of the activity or fragment to check.
     * @return True if the activity or fragment has been visited before, false otherwise.
     */
    public boolean isVisited(String activityOrFragmentName) {
        return mPrefs.getBoolean(activityOrFragmentName, false);
    }

    /**
     * Sets the visited status of the given activity or fragment to true.
     *
     * @param activityOrFragmentName The name of the activity or fragment to mark as visited.
     */
    public void setVisited(String activityOrFragmentName) {
        mPrefs.edit().putBoolean(activityOrFragmentName, true).apply();
    }

    /**
     Clear all saved activity/fragment visit status from the shared preferences.
     */
    public void clearActivitiesStatus() {
        mPrefs.edit().clear().apply();
    }

}
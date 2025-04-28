package com.example.finalprojectyali.Extras.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AirplaneModeReceiver extends BroadcastReceiver {
    public static boolean isAirplaneMode;
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
        isAirplaneMode = isAirplaneModeOn;
        if (isAirplaneModeOn) {
            // Airplane mode is turned on
            Toast.makeText(context.getApplicationContext(),"Airplane is turned on.",Toast.LENGTH_LONG).show();
        } else {
            // Airplane mode is turned off
            Toast.makeText(context.getApplicationContext(),"Airplane is turned off.",Toast.LENGTH_LONG).show();
        }
    }
}

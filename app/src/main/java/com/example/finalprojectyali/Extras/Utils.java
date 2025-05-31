package com.example.finalprojectyali.Extras;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Adapters.RecyclerViewFunctionalities;
import com.example.finalprojectyali.R;

/**
 * Misc. helper methods used throughout the app.
 *   • Network connectivity check
 *   • Recycler-view adapter safety check
 *   • One-shot timed notifications
 *
 * @author Yali Shem Tov
 */
public final class Utils {

    /*──────────────────────────  Connectivity  ──────────────────────────*/

    /**
     * Returns true if the device currently has network connectivity.
     */
    public static boolean isConnectedToInternet(@NonNull Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm != null ? cm.getActiveNetworkInfo() : null;
        return net != null && net.isConnected();
    }

    /*──────────────────────  Recycler-view guard  ───────────────────────*/

    /**
     * Quick guard for adapter click-handlers.
     */
    public static boolean checkInterfaceValid(@Nullable RecyclerViewFunctionalities rvFuncs, int adapterPos) {
        return rvFuncs != null && adapterPos != RecyclerView.NO_POSITION;
    }

    /*─────────────────────  Timed notification helpers  ─────────────────────*/

    private static final String CHANNEL_ID   = "reminder_channel";
    private static final String CHANNEL_NAME = "Reminders";
    private static final int    CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT;

    private Utils() {/* no instances */}

    /**
     * Schedule a one-time notification.
     *
     * @param ctx          application Context
     * @param triggerAtMs  wall-clock time (System.currentTimeMillis()) when the alert should fire
     * @param title        notification title
     * @param content      notification body
     * @param smallIconRes drawable for the small icon (24 dp). Pass 0 for default bell.
     */
    public static void scheduleNotification(@NonNull Context ctx,
                                            long triggerAtMs,
                                            @NonNull String title,
                                            @NonNull String content,
                                            @DrawableRes @Nullable Integer smallIconRes) {

        createChannelIfNeeded(ctx);

        Intent intent = new Intent(ctx, NotificationPublisher.class)
                .putExtra(NotificationPublisher.EXTRA_TITLE,   title)
                .putExtra(NotificationPublisher.EXTRA_CONTENT, content)
                .putExtra(NotificationPublisher.EXTRA_ICON,    smallIconRes != null ? smallIconRes : 0);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                (int) triggerAtMs,   // reasonably unique requestCode
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            }
        }
    }

    /** Creates the notification channel once (API 26+). */
    private static void createChannelIfNeeded(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel =
                        new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, CHANNEL_IMPORTANCE);
                nm.createNotificationChannel(channel);
            }
        }
    }

    /**
     * BroadcastReceiver that builds and posts the notification when the alarm fires.
     * (Must be registered in AndroidManifest.xml)
     */
    public static class NotificationPublisher extends BroadcastReceiver {

        static final String EXTRA_TITLE   = "title";
        static final String EXTRA_CONTENT = "content";
        static final String EXTRA_ICON    = "icon";

        @Override
        public void onReceive(Context ctx, Intent intent) {
            String title   = intent.getStringExtra(EXTRA_TITLE);
            String content = intent.getStringExtra(EXTRA_CONTENT);
            int iconRes    = intent.getIntExtra(EXTRA_ICON, 0);
            if (iconRes == 0) iconRes = android.R.drawable.ic_popup_reminder;

            NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(iconRes)
                    .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), iconRes))
                    .setContentTitle(title)
                    .setContentText(content)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify((int) System.currentTimeMillis(), nb.build());
        }
    }
}

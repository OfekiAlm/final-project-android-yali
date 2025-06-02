package com.example.finalprojectyali.Extras.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.finalprojectyali.ui.Home.EventActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationListenerService extends Service {
    
    private static final String TAG = "NotifListenerService";
    private static final String CHANNEL_ID = "event_notifications";
    private static final String CHANNEL_NAME = "Event Notifications";
    private static final int FOREGROUND_SERVICE_ID = 1;
    
    private DatabaseReference notifRef;
    private ChildEventListener notifListener;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        createNotificationChannel();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        // For Android 8+ we need to run as foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Monitoring event notifications")
                    .setContentText("You'll be notified of event updates")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true);
                    
            if (Build.VERSION.SDK_INT >= 34) { // Android 14+
                startForeground(FOREGROUND_SERVICE_ID, builder.build(), 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(FOREGROUND_SERVICE_ID, builder.build());
            }
        }
        
        startListening();
        return START_STICKY;
    }
    
    private void startListening() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.w(TAG, "No user logged in");
            stopSelf();
            return;
        }
        
        // Clean up any existing listener
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
        
        notifRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("notifications");
        
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                handleNewNotification(snapshot);
            }
            
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Handle if a notification is updated
            }
            
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        };
        
        notifRef.addChildEventListener(notifListener);
        Log.d(TAG, "Started listening for notifications");
    }
    
    private void handleNewNotification(DataSnapshot snapshot) {
        try {
            // Get notification data
            String title = snapshot.child("title").getValue(String.class);
            String message = snapshot.child("message").getValue(String.class);
            String type = snapshot.child("type").getValue(String.class);
            String eventId = snapshot.child("eventId").getValue(String.class);
            String eventName = snapshot.child("eventName").getValue(String.class);
            Boolean read = snapshot.child("read").getValue(Boolean.class);
            
            Log.d(TAG, "New notification: " + title);
            
            // Only show if not already read
            if (read == null || !read) {
                showNotification(title, message, type, eventId, eventName);
                
                // Mark as read
                snapshot.getRef().child("read").setValue(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling notification", e);
        }
    }
    
    private void showNotification(String title, String message, String type, 
                                  String eventId, String eventName) {
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Choose icon based on type
        int icon = "event_accepted".equals(type) ? 
                android.R.drawable.ic_dialog_info : 
                android.R.drawable.ic_dialog_alert;
        
        // Choose color based on type
        int color = "event_accepted".equals(type) ? 
                android.graphics.Color.GREEN : 
                android.graphics.Color.RED;
        
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(color)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        
        // Add click action to open event (if accepted)
        if ("event_accepted".equals(type) && eventId != null) {
            Intent intent = new Intent(this, EventActivity.class);
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_name", eventName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 
                    (int) System.currentTimeMillis(), // unique request code
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            builder.setContentIntent(pendingIntent);
        }
        
        // Show notification with unique ID based on timestamp
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
        
        Log.d(TAG, "Notification shown: " + title);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for event requests and updates");
            channel.enableLights(true);
            channel.enableVibration(true);
            
            NotificationManager notificationManager = 
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
    }
    
    // Static helper methods for easy start/stop
    public static void start(Context context) {
        Intent serviceIntent = new Intent(context, NotificationListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
    
    public static void stop(Context context) {
        context.stopService(new Intent(context, NotificationListenerService.class));
    }
} 
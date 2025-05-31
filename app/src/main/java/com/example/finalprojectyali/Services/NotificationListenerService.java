package com.example.finalprojectyali.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Home.EventActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationListenerService {
    
    private static final String CHANNEL_ID = "event_notifications";
    private static final String CHANNEL_NAME = "Event Notifications";
    private static final String CHANNEL_DESC = "Notifications for event requests";
    
    private Context context;
    private DatabaseReference notifRef;
    private ChildEventListener notifListener;
    
    public NotificationListenerService(Context context) {
        this.context = context;
        createNotificationChannel();
    }
    
    public void startListening() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        
        notifRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("notifications");
        
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Get notification data
                String title = snapshot.child("title").getValue(String.class);
                String message = snapshot.child("message").getValue(String.class);
                String type = snapshot.child("type").getValue(String.class);
                String eventId = snapshot.child("eventId").getValue(String.class);
                String eventName = snapshot.child("eventName").getValue(String.class);
                Boolean read = snapshot.child("read").getValue(Boolean.class);
                
                // Only show if not already read
                if (read == null || !read) {
                    showNotification(title, message, type, eventId, eventName);
                    
                    // Mark as read
                    snapshot.getRef().child("read").setValue(true);
                }
            }
            
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        
        notifRef.addChildEventListener(notifListener);
    }
    
    public void stopListening() {
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
    }
    
    private void showNotification(String title, String message, String type, 
                                  String eventId, String eventName) {
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Choose icon based on type
        int icon = "event_accepted".equals(type) ? 
                android.R.drawable.ic_dialog_info : 
                android.R.drawable.ic_dialog_alert;
        
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        
        // Add click action to open event (if accepted)
        if ("event_accepted".equals(type) && eventId != null) {
            // Create simple intent - EventActivity will fetch event data from Firebase if needed
            Intent intent = new Intent(context, EventActivity.class);
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_name", eventName);
            // Note: EventActivity should fetch complete event data from Firebase using eventId
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            builder.setContentIntent(pendingIntent);
        }
        
        // Show notification
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            
            NotificationManager notificationManager = 
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
} 
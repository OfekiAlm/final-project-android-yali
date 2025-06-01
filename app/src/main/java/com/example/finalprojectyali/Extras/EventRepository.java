package com.example.finalprojectyali.Extras;

import androidx.annotation.NonNull;

import com.example.finalprojectyali.Models.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class EventRepository {

    private static final DatabaseReference ROOT   = FirebaseDatabase.getInstance().getReference();
    private static final DatabaseReference EVENTS = ROOT.child("Events");

    /*───────────────────────────────────────────────────────────────
      1. Create event   (join-codes removed)
    ───────────────────────────────────────────────────────────────*/
    public static Task<Event> createEvent(String name,
                                          String description,
                                          Date   when,
                                          String whereAddress,
                                          @NonNull Consumer<Event> onSuccess,
                                          @NonNull Consumer<Exception> onError) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null)
            return Tasks.forException(new IllegalStateException("Not signed in"));

        String pushKey = EVENTS.push().getKey();

        Event e = new Event(name, description, when.getTime(), whereAddress, uid);
        e.setKey(pushKey);

        Map<String, Object> fanOut = new HashMap<>();
        fanOut.put("/Events/" + pushKey, e);
        fanOut.put("/Users/" + uid + "/Events/" + pushKey, "accepted");

        return ROOT.updateChildren(fanOut)
                .continueWith(t -> e)
                .addOnSuccessListener(onSuccess::accept)
                .addOnFailureListener(onError::accept);
    }

    /*───────────────────────────────────────────────────────────────
      1-b.  Admin: update basic event details
    ───────────────────────────────────────────────────────────────*/
    public static Task<Void> updateEvent(String eventId,
                                         String name,
                                         String description,
                                         long   when,
                                         String whereAddress) {

        Map<String, Object> upd = new HashMap<>();
        upd.put("name",            name);
        upd.put("description",     description);
        upd.put("eventDate",       when);
        upd.put("locationAddress", whereAddress);

        return EVENTS.child(eventId).updateChildren(upd);
    }

    /*───────────────────────────────────────────────────────────────
      2. Request to join event
    ───────────────────────────────────────────────────────────────*/
    public static void requestJoin(String eventId, Consumer<Void> cb) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference evRef = EVENTS.child(eventId);
        evRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData cur) {
                Event e = cur.getValue(Event.class);
                if (e == null) return Transaction.abort();

                Map<String, String> m = e.getMembers();
                m.put(uid, "pending");
                e.setMembers(m);
                cur.setValue(e);
                return Transaction.success(cur);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot d) {
                if (c && e == null)
                    ROOT.child("Users").child(uid).child("Events")
                            .child(eventId).setValue("pending")
                            .addOnSuccessListener(cb::accept);
            }
        });
    }

    /*───────────────────────────────────────────────────────────────
      3. Admin: accept / reject member
    ───────────────────────────────────────────────────────────────*/
    public static void acceptMember(String eventId, String memberUid) {
        Map<String, Object> fanOut = new HashMap<>();
        fanOut.put("/Events/" + eventId + "/members/" + memberUid, "accepted");
        fanOut.put("/Users/" + memberUid + "/Events/" + eventId, "accepted");
        ROOT.updateChildren(fanOut);
    }

    public static void rejectMember(String eventId, String memberUid) {
        Map<String, Object> fanOut = new HashMap<>();
        fanOut.put("/Events/" + eventId + "/members/" + memberUid, null);
        fanOut.put("/Users/" + memberUid + "/Events/" + eventId, null);
        ROOT.updateChildren(fanOut);
    }

    /*───────────────────────────────────────────────────────────────
      4. Push in-app notification to a user
    ───────────────────────────────────────────────────────────────*/
    public static void sendNotification(String toUid,
                                        String title,
                                        String message,
                                        String type,
                                        String eventId,
                                        String eventName) {

        DatabaseReference notifRef = ROOT.child("Users")
                .child(toUid)
                .child("notifications");
        String pushKey = notifRef.push().getKey();

        Map<String, Object> data = new HashMap<>();
        data.put("title",     title);
        data.put("message",   message);
        data.put("type",      type);
        data.put("eventId",   eventId);
        data.put("eventName", eventName);
        data.put("timestamp", ServerValue.TIMESTAMP);
        data.put("read",      false);
        data.put("key",       pushKey);

        notifRef.child(pushKey).setValue(data);
    }

    /*───────────────────────────────────────────────────────────────
      5. Admin: completely delete an event
    ───────────────────────────────────────────────────────────────*/
    public static void deleteEvent(String eventId, @NonNull Consumer<Void> onSuccess, @NonNull Consumer<Exception> onError) {
        // First, get the event to find all members
        EVENTS.child(eventId).get().addOnSuccessListener(eventSnapshot -> {
            if (!eventSnapshot.exists()) {
                onError.accept(new Exception("Event not found"));
                return;
            }

            Event event = eventSnapshot.getValue(Event.class);
            if (event == null) {
                onError.accept(new Exception("Failed to parse event data"));
                return;
            }

            Map<String, Object> deletionUpdates = new HashMap<>();
            
            // Delete the main event
            deletionUpdates.put("/Events/" + eventId, null);
            
            // Remove event from all members' user events
            if (event.getMembers() != null) {
                for (String memberUid : event.getMembers().keySet()) {
                    deletionUpdates.put("/Users/" + memberUid + "/Events/" + eventId, null);
                }
            }
            
            // Remove notifications related to this event from all users
            ROOT.child("Users").get().addOnSuccessListener(usersSnapshot -> {
                for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                    DataSnapshot notificationsSnapshot = userSnapshot.child("notifications");
                    for (DataSnapshot notifSnapshot : notificationsSnapshot.getChildren()) {
                        Map<String, Object> notifData = (Map<String, Object>) notifSnapshot.getValue();
                        if (notifData != null && eventId.equals(notifData.get("eventId"))) {
                            deletionUpdates.put("/Users/" + userSnapshot.getKey() + "/notifications/" + notifSnapshot.getKey(), null);
                        }
                    }
                }
                
                // Execute all deletions atomically
                ROOT.updateChildren(deletionUpdates)
                        .addOnSuccessListener(aVoid -> onSuccess.accept(null))
                        .addOnFailureListener(onError::accept);
            }).addOnFailureListener(onError::accept);
        }).addOnFailureListener(onError::accept);
    }

    /*───────────────────────────────────────────────────────────────
      6. User: leave an event
    ───────────────────────────────────────────────────────────────*/
    public static void leaveEvent(String eventId, @NonNull Consumer<Void> onSuccess, @NonNull Consumer<Exception> onError) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            onError.accept(new IllegalStateException("Not signed in"));
            return;
        }

        // Check if user is the owner (owners cannot leave, they must delete the event)
        EVENTS.child(eventId).child("ownerUid").get().addOnSuccessListener(ownerSnapshot -> {
            String ownerUid = ownerSnapshot.getValue(String.class);
            if (uid.equals(ownerUid)) {
                onError.accept(new Exception("Event owners cannot leave. Please delete the event instead."));
                return;
            }

            Map<String, Object> leaveUpdates = new HashMap<>();
            leaveUpdates.put("/Events/" + eventId + "/members/" + uid, null);
            leaveUpdates.put("/Users/" + uid + "/Events/" + eventId, null);

            ROOT.updateChildren(leaveUpdates)
                    .addOnSuccessListener(aVoid -> onSuccess.accept(null))
                    .addOnFailureListener(onError::accept);
        }).addOnFailureListener(onError::accept);
    }

    /*───────────────────────────────────────────────────────────────*/
    private EventRepository() {}
}

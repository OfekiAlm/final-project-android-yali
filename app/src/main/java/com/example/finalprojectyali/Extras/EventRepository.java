package com.example.finalprojectyali.Extras;

import androidx.annotation.NonNull;

import com.example.finalprojectyali.Models.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class EventRepository {

    private static final DatabaseReference ROOT = FirebaseDatabase.getInstance().getReference();
    private static final DatabaseReference EVENTS = ROOT.child("Events");
    private static final DatabaseReference JOIN_CODES = ROOT.child("joinCodes");

    private static final SecureRandom RNG = new SecureRandom();
    private static final String ALPHA_NUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /*───────────────────────────────────────────────────────────────
      1. Create event
    ───────────────────────────────────────────────────────────────*/
    public static Task<Event> createEvent(String name,
                                          String description,
                                          Date when,
                                          String whereAddress,
                                          @NonNull Consumer<Event> onSuccess,
                                          @NonNull Consumer<Exception> onError) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        String pushKey = EVENTS.push().getKey();

        return generateUniqueCodeAsync()
                .continueWithTask(codeTask -> {
                    if (!codeTask.isSuccessful())
                        return Tasks.forException(codeTask.getException());

                    String joinCode = codeTask.getResult();
                    Event e = new Event(name, description, when.getTime(), whereAddress, uid, joinCode);
                    e.setKey(pushKey);

                    Map<String, Object> fanOut = new HashMap<>();
                    fanOut.put("/Events/" + pushKey, e);
                    fanOut.put("/joinCodes/" + joinCode, pushKey);
                    fanOut.put("/Users/" + uid + "/Events/" + pushKey, "accepted");

                    return ROOT.updateChildren(fanOut).continueWith(t -> e);
                })
                .addOnSuccessListener(onSuccess::accept)
                .addOnFailureListener(onError::accept);
    }

    /* ───────────────────────────────────────────────────────────────────
   1-b.  Admin: update basic event details (name/desc/date/location)
   ─────────────────────────────────────────────────────────────────── */
    public static Task<Void> updateEvent(String eventId,
                                         String name,
                                         String description,
                                         long when,
                                         String whereAddress) {

        Map<String, Object> upd = new HashMap<>();
        upd.put("name", name);
        upd.put("description", description);
        upd.put("eventDate", when);
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
            @NonNull
            @Override
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
                    ROOT.child("Users").child(uid).child("Events").child(eventId).setValue("pending")
                            .addOnSuccessListener(cb::accept);
            }
        });
    }

    /*───────────────────────────────────────────────────────────────
      3. Admin: accept request
    ───────────────────────────────────────────────────────────────*/
    public static void acceptMember(String eventId, String memberUid) {
        Map<String, Object> fanOut = new HashMap<>();
        fanOut.put("/Events/" + eventId + "/members/" + memberUid, "accepted");
        fanOut.put("/Users/" + memberUid + "/Events/" + eventId, "accepted");
        ROOT.updateChildren(fanOut);
    }

    /*───────────────────────────────────────────────────────────────
      3b. Admin: reject request
    ───────────────────────────────────────────────────────────────*/
    public static void rejectMember(String eventId, String memberUid) {
        Map<String, Object> fanOut = new HashMap<>();
        fanOut.put("/Events/" + eventId + "/members/" + memberUid, null); // Remove from event
        fanOut.put("/Users/" + memberUid + "/Events/" + eventId, null);   // Remove from user's events
        ROOT.updateChildren(fanOut);
    }

    /*───────────────────────────────────────────────────────────────
      4. Send notification to user
    ───────────────────────────────────────────────────────────────*/
    public static void sendNotification(String toUid, String title, String message, 
                                      String type, String eventId, String eventName) {
        DatabaseReference notifRef = ROOT.child("Users").child(toUid).child("notifications");
        String pushKey = notifRef.push().getKey();
        
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("type", type);
        notifData.put("eventId", eventId);
        notifData.put("eventName", eventName);
        notifData.put("timestamp", ServerValue.TIMESTAMP);
        notifData.put("read", false);
        notifData.put("key", pushKey);
        
        notifRef.child(pushKey).setValue(notifData);
    }

    /*───────────────────────────────────────────────────────────────*/
    private static String randomCode() {
        char[] buf = new char[4];
        for (int i = 0; i < 4; i++) buf[i] = ALPHA_NUM.charAt(RNG.nextInt(ALPHA_NUM.length()));
        return new String(buf);
    }

    private static Task<String> generateUniqueCodeAsync() {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        attemptReserve(tcs);
        return tcs.getTask();
    }

    private static void attemptReserve(TaskCompletionSource<String> tcs) {
        String code = randomCode();
        DatabaseReference ref = JOIN_CODES.child(code);

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData cur) {
                if (cur.getValue() != null) return Transaction.abort();
                cur.setValue(true);
                return Transaction.success(cur);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot d) {
                if (e != null) {
                    tcs.setException(e.toException());
                    return;
                }
                if (c) tcs.setResult(code);
                else attemptReserve(tcs);
            }
        });
    }

    private EventRepository() {
    }
}
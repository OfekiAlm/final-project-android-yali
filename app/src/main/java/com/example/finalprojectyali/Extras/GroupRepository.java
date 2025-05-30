package com.example.finalprojectyali.Extras;

import static com.google.firebase.database.Transaction.Handler;

import androidx.annotation.NonNull;

import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.Models.Ingredient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class GroupRepository {

    private static final DatabaseReference ROOT   = FirebaseDatabase.getInstance().getReference();
    private static final DatabaseReference GROUPS = ROOT.child("Groups");      // **exact casing**
    private static final DatabaseReference JOIN_CODES = ROOT.child("joinCodes");

    private static final SecureRandom RNG = new SecureRandom();
    private static final String ALPHA_NUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /* ─────────────────────────────────────────────────────────────────── */
    /* 1. Create group                                                    */
    /* ─────────────────────────────────────────────────────────────────── */
    public static Task<Group> createGroup(String name,
                                          String description,
                                          List<Ingredient> ingredients,
                                          @NonNull Consumer<Group> onSuccess,
                                          @NonNull Consumer<Exception> onError) {

        String uid     = FirebaseAuth.getInstance().getUid();
        String pushKey = GROUPS.push().getKey();

        return generateUniqueCodeAsync()
                .continueWithTask(codeTask -> {

                    if (!codeTask.isSuccessful())
                        return Tasks.forException(codeTask.getException());

                    String joinCode = codeTask.getResult();

                    Group g = new Group(name, description, uid, joinCode, ingredients);
                    g.setKey(pushKey);

                    Map<String, Object> fanOut = new HashMap<>();
                    fanOut.put("/Groups/" + pushKey, g);
                    fanOut.put("/joinCodes/" + joinCode, pushKey);
                    fanOut.put("/Users/" + uid + "/Groups/" + pushKey, true);

                    return ROOT.updateChildren(fanOut).continueWith(t -> g);
                })
                .addOnSuccessListener(onSuccess::accept)
                .addOnFailureListener(onError::accept);
    }

    /* ─────────────────────────────────────────────────────────────────── */
    /* 2. Update name / description                                       */
    /* ─────────────────────────────────────────────────────────────────── */
    public static Task<Void> updateGroup(String groupId,
                                         String name,
                                         String description,
                                         @NonNull Consumer<Void> onSuccess,
                                         @NonNull Consumer<Exception> onError) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);

        return GROUPS.child(groupId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> onSuccess.accept(null))
                .addOnFailureListener(onError::accept);
    }

    /* ─────────────────────────────────────────────────────────────────── */
    /* 3. Join by code                                                    */
    /* ─────────────────────────────────────────────────────────────────── */
    public static void joinByCode(String code,
                                  @NonNull Consumer<Group> onSuccess,
                                  @NonNull Consumer<String> onFail) {

        JOIN_CODES.child(code).get().addOnSuccessListener(snap -> {

            if (!snap.exists()) { onFail.accept("Code not found"); return; }

            String groupId = snap.getValue(String.class);
            String uid     = FirebaseAuth.getInstance().getUid();

            if (groupId == null || uid == null) { onFail.accept("Invalid state"); return; }

            GROUPS.child(groupId).runTransaction(new Handler() {

                @NonNull @Override
                public Transaction.Result doTransaction(@NonNull MutableData current) {
                    Group g = current.getValue(Group.class);
                    if (g == null) return Transaction.abort();

                    Map<String, Boolean> members = g.getMembers();
                    if (members == null) members = new HashMap<>();

                    if (members.containsKey(uid))
                        return Transaction.success(current);   // already inside

                    members.put(uid, true);
                    g.setMembers(members);
                    g.setMembersCount(g.getMembersCount() + 1);
                    current.setValue(g);
                    return Transaction.success(current);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot data) {
                    if (error != null) { onFail.accept("Join failed: " + error.getMessage()); return; }
                    if (!committed)   { onFail.accept("Join aborted"); return; }

                    ROOT.child("Users").child(uid).child("Groups").child(groupId)
                            .setValue(true)
                            .addOnSuccessListener(v -> onSuccess.accept(data.getValue(Group.class)))
                            .addOnFailureListener(e -> onFail.accept(e.getMessage()));
                }
            });
        }).addOnFailureListener(e -> onFail.accept(e.getMessage()));
    }

    /* ─────────────────────────────────────────────────────────────────── */
    /* 4. Privilege check                                                 */
    /* ─────────────────────────────────────────────────────────────────── */
    public static void isCurrentUserOwner(String groupId, Consumer<Boolean> cb) {
        GROUPS.child(groupId).child("ownerUid").get()
                .addOnSuccessListener(snap ->
                        cb.accept(FirebaseAuth.getInstance().getUid()
                                .equals(snap.getValue(String.class))));
    }

    /* ─────────────────────────────────────────────────────────────────── */
    /* 5. Delete group (owner only)                                       */
    /* ─────────────────────────────────────────────────────────────────── */
    public static Task<Void> deleteGroupIfOwner(String groupId) {

        return GROUPS.child(groupId).get().continueWithTask(t -> {

            /* 5-a. Basic validation */
            DataSnapshot snap = t.getResult();
            Group g = snap.getValue(Group.class);

            if (g == null)
                throw new IllegalStateException("Group does not exist");

            String currentUid = FirebaseAuth.getInstance().getUid();
            if (currentUid == null || !currentUid.equals(g.getOwnerUid()))
                throw new SecurityException("Only the owner can delete");

            /* 5-b. Build one big fan-out map of deletions */
            Map<String, Object> fanOut = new HashMap<>();

            /* remove the group itself */
            fanOut.put("/Groups/" + groupId, null);

            /* remove the join-code */
            fanOut.put("/joinCodes/" + g.getJoinCode(), null);

            /* remove the group pointer under each user */
            if (g.getMembers() != null) {
                for (String memberUid : g.getMembers().keySet()) {
                    fanOut.put("/Users/" + memberUid + "/Groups/" + groupId, null);
                }
            }

            /* 5-c. Atomically apply */
            return ROOT.updateChildren(fanOut);
        });
    }

    /* ─────────────────────────────────────────────────────────────────── */
    /* 6. Helper: generate unique code                                    */
    /* ─────────────────────────────────────────────────────────────────── */
    private static String randomCode() {
        char[] buf = new char[4];
        for (int i = 0; i < 4; i++)
            buf[i] = ALPHA_NUM.charAt(RNG.nextInt(ALPHA_NUM.length()));
        return new String(buf);
    }

    private static Task<String> generateUniqueCodeAsync() {

        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        attemptReserveCode(tcs);
        return tcs.getTask();
    }

    private static void attemptReserveCode(TaskCompletionSource<String> tcs) {

        String code = randomCode();
        DatabaseReference ref = JOIN_CODES.child(code);

        ref.runTransaction(new Handler() {

            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData cur) {
                if (cur.getValue() != null) return Transaction.abort();
                cur.setValue(true);
                return Transaction.success(cur);
            }

            @Override
            public void onComplete(DatabaseError e, boolean committed, DataSnapshot ds) {

                if (e != null) { tcs.setException(e.toException()); return; }

                if (committed) {
                    tcs.setResult(code);
                } else {
                    attemptReserveCode(tcs);  // collision – try another code
                }
            }
        });
    }

    private GroupRepository() {}     // no instances
}
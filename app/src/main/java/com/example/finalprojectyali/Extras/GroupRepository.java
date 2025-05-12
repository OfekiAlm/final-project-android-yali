package com.example.finalprojectyali.Extras;

import static com.google.firebase.database.Transaction.*;

import androidx.annotation.NonNull;

import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.Models.Ingredient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.android.gms.tasks.*;
import com.google.firebase.database.Transaction;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class GroupRepository {

    private static final DatabaseReference ROOT = FirebaseDatabase.getInstance().getReference();
    private static final DatabaseReference GROUPS = ROOT.child("groups");
    private static final DatabaseReference JOIN_CODES = ROOT.child("joinCodes");
    private static final SecureRandom RNG = new SecureRandom();
    private static final String ALPHA_NUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";


    /* ------------------------------------------------------------------ */
    /* 1. Create a new group  (collision-safe join-code)                  */
    /* ------------------------------------------------------------------ */
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

                    Map<String,Object> fanOut = new HashMap<>();
                    fanOut.put("/Groups/" + pushKey, g);
                    fanOut.put("/joinCodes/" + joinCode, pushKey);
                    fanOut.put("/Users/" + uid + "/Groups/" + pushKey, true);

                    // propagate the Group object forward so callers get it
                    return ROOT.updateChildren(fanOut).continueWith(t -> g);
                })
                .addOnSuccessListener(onSuccess::accept)
                .addOnFailureListener(onError::accept);
    }



    /* ------------------------------------------------------------------ */
    /* 2. Join existing group via code                                    */
    /* ------------------------------------------------------------------ */
    public static void joinByCode(String code,
                                  @NonNull Consumer<Group> onSuccess,
                                  @NonNull Consumer<String> onFail)
    {
        JOIN_CODES.child(code).get().addOnSuccessListener(snap -> {
            if (!snap.exists()) { onFail.accept("Code not found"); return; }

            String groupId = snap.getValue(String.class);
            String uid     = FirebaseAuth.getInstance().getUid();

            // Atomically add user to members[] and increment count
            GROUPS.child(groupId).runTransaction(new Handler() {
                @NonNull @Override
                public Result doTransaction(@NonNull MutableData current) {
                    Group g = current.getValue(Group.class);
                    if (g == null) return success(current);

                    if (g.getMembers() != null && g.getMembers().containsKey(uid))
                        return success(current); // already inside

                    g.getMembers().put(uid, true);
                    g.setMembersCount(g.getMembersCount() + 1);
                    current.setValue(g);
                    return success(current);
                }

                @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                    if (committed && error == null) {
                        // Add shortcut in user branch
                        ROOT.child("Users").child(uid)
                                .child("Groups").child(groupId).setValue(true);
                        onSuccess.accept(currentData.getValue(Group.class));
                    } else {
                        onFail.accept("Join failed: " + (error != null ? error.getMessage() : ""));
                    }
                }
            });
        }).addOnFailureListener(e -> onFail.accept(e.getMessage()));
    }

    /* ------------------------------------------------------------------ */
    /* 3. Check owner privileges                                          */
    /* ------------------------------------------------------------------ */
    public static void isCurrentUserOwner(String groupId, Consumer<Boolean> cb) {
        GROUPS.child(groupId).child("ownerUid").get()
                .addOnSuccessListener(snap -> cb.accept(
                        FirebaseAuth.getInstance().getUid().equals(snap.getValue(String.class))));
    }

    /* ------------------------------------------------------------------ */
    /* 4. Delete a group (owner only)                                     */
    /* ------------------------------------------------------------------ */
    public static Task<Void> deleteGroupIfOwner(String groupId) {
        return GROUPS.child(groupId).get().continueWithTask(t -> {
            Group g = t.getResult().getValue(Group.class);
            if (g == null) throw new IllegalStateException("Group missing");
            if (!FirebaseAuth.getInstance().getUid().equals(g.getOwnerUid()))
                throw new SecurityException("Only owner can delete");

            // Easy version: just remove /groups and /joinCodes/{code}
            Task<?> t1 = GROUPS.child(groupId).removeValue();
            Task<?> t2 = JOIN_CODES.child(g.getJoinCode()).removeValue();
            return Tasks.whenAll(t1, t2);
        });
    }

    /* utility */
    private static String randomCode() {
        char[] buf = new char[4];
        for (int i = 0; i < 4; i++)
            buf[i] = ALPHA_NUM.charAt(RNG.nextInt(ALPHA_NUM.length()));
        return new String(buf);
    }

    /** Generates a 4-char code that is guaranteed unique under /joinCodes */
    private static Task<String> generateUniqueCodeAsync() {

        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        attemptReserveCode(tcs);   // recursion happens inside this helper

        return tcs.getTask();      // the Task<String> the rest of the code awaits
    }

    private static void attemptReserveCode(TaskCompletionSource<String> tcs) {

        String code = randomCode();
        DatabaseReference codeRef = JOIN_CODES.child(code);

        codeRef.runTransaction(new Transaction.Handler() {

            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData current) {
                if (current.getValue() != null) {             // collision – abort
                    return Transaction.abort();
                }
                current.setValue(true);                       // reserve
                return Transaction.success(current);
            }

            @Override
            public void onComplete(DatabaseError error,
                                   boolean committed,
                                   DataSnapshot snapshot) {

                if (error != null) {                          // network / perms
                    tcs.setException(error.toException());
                    return;
                }

                if (committed) {                              // success – done
                    tcs.setResult(code);
                } else {                                      // collision – retry
                    attemptReserveCode(tcs);
                }
            }
        });
    }
    private GroupRepository() {}
}

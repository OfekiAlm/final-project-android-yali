package com.example.finalprojectyali.Extras;

import static com.google.android.gms.tasks.Task.*;
import static com.google.firebase.database.Transaction.*;

import androidx.annotation.NonNull;

import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.Extras.Utils.JoinCodeUtil;
import com.example.finalprojectyali.Models.Ingredient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.android.gms.tasks.*;
import java.util.List;
import java.util.function.Consumer;

public final class GroupRepository {

    private static final DatabaseReference ROOT = FirebaseDatabase.getInstance().getReference();
    private static final DatabaseReference GROUPS = ROOT.child("groups");
    private static final DatabaseReference JOIN_CODES = ROOT.child("joinCodes");

    /* ------------------------------------------------------------------ */
    /* 1. Create a new group  (collision-safe join-code)                  */
    /* ------------------------------------------------------------------ */
    public static Task<Void> createGroup(String name,
                                         String description,
                                         List<Ingredient> ingredients,
                                         @NonNull Consumer<Group> onSuccess,
                                         @NonNull Consumer<Exception> onError)
    {
        String uid = FirebaseAuth.getInstance().getUid();
        String pushKey = GROUPS.push().getKey();
        String joinCode = generateUniqueCodeBlocking();   // 99.9 % of apps fine with blocking loop

        Group g = new Group(name, description, uid, joinCode, ingredients);
        g.setKey(pushKey);

        return GROUPS.child(pushKey).setValue(g).continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    // 1) /joinCodes/{code} → groupId   (so others can “lookup”)
                    // 2) /Users/{uid}/Groups/{groupId} = true  (creator membership shortcut)
                    Task<?> t1 = JOIN_CODES.child(joinCode).setValue(pushKey);
                    Task<?> t2 = ROOT.child("Users").child(uid)
                            .child("Groups").child(pushKey).setValue(true);
                    return Tasks.whenAll(t1, t2);
                }).addOnSuccessListener(v -> onSuccess.accept(g))
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
    private static String generateUniqueCodeBlocking() {
        while (true) {
            String c = JoinCodeUtil.generateCode();
            if (!JOIN_CODES.child(c).get().getResult().exists()) return c;
        }
    }

    private GroupRepository() {}
}

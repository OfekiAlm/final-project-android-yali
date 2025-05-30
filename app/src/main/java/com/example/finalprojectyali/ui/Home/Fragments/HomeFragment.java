package com.example.finalprojectyali.ui.Home.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Adapters.GroupAdapter;
import com.example.finalprojectyali.Adapters.RecyclerViewFunctionalities;
import com.example.finalprojectyali.Extras.GroupRepository;
import com.example.finalprojectyali.Extras.Utils;
import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Home.GroupActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays user's groups in real-time.
 */
public class HomeFragment extends Fragment implements RecyclerViewFunctionalities {

    // ─────────── Instance fields ───────────
    private final List<Group> groupsList = new ArrayList<>();
    private final Map<String, ValueEventListener> groupDetailsListeners = new HashMap<>();

    private GroupAdapter adapter;
    private RecyclerView recyclerView;

    private DatabaseReference userGroupsRef;
    private ChildEventListener groupsKeysListener;

    private FirebaseAuth mAuth;

    // ─────────── Lifecycle ───────────
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.group_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 1));
        recyclerView.setHasFixedSize(true);

        adapter = new GroupAdapter(requireContext(), groupsList, this);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getCurrentUser().getUid();
        userGroupsRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("Groups");

        if (Utils.isConnectedToInternet(requireContext())) {
            attachGroupKeysListener();
        } else {
            new AlertDialog.Builder(requireContext())
                    .setIcon(R.drawable.baseline_airplanemode_active_24)
                    .setTitle("Internet Connection")
                    .setMessage("You're not connected to the internet. We can't proceed.")
                    .setCancelable(false)
                    .setPositiveButton("I WILL TURN IT OFF", (d, w) -> {
                        d.dismiss();
                        attachGroupKeysListener();
                    })
                    .setNeutralButton("OK", (d, w) -> d.dismiss())
                    .show();
        }
    }

    // ─────────── Real-time listeners ───────────
    private void attachGroupKeysListener() {
        groupsKeysListener = new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot,
                                     @Nullable String previousChildName) {
                String groupId = snapshot.getKey();
                if (groupId != null) attachGroupDetailsListener(groupId);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String groupId = snapshot.getKey();
                if (groupId == null) return;

                // stop listening to that group’s changes
                detachGroupDetailsListener(groupId);

                int idx = indexOfGroup(groupId);
                if (idx != -1) {
                    groupsList.remove(idx);
                    adapter.notifyItemRemoved(idx);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {/*unused*/}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p)     {/*unused*/}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "userGroups listener cancelled", error.toException());
            }
        };

        userGroupsRef.addChildEventListener(groupsKeysListener);
    }

    private void attachGroupDetailsListener(@NonNull String groupId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Groups")
                .child(groupId);

        ValueEventListener detailsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group g = snapshot.getValue(Group.class);
                if (g == null) return;

                int idx = indexOfGroup(groupId);
                if (idx == -1) {                       // new
                    groupsList.add(g);
                    adapter.notifyItemInserted(groupsList.size() - 1);
                } else {                               // update
                    groupsList.set(idx, g);
                    adapter.notifyItemChanged(idx);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "group details listener cancelled", error.toException());
            }
        };

        ref.addValueEventListener(detailsListener);
        groupDetailsListeners.put(groupId, detailsListener);
    }

    private void detachGroupDetailsListener(@NonNull String groupId) {
        ValueEventListener l = groupDetailsListeners.remove(groupId);
        if (l != null) {
            FirebaseDatabase.getInstance()
                    .getReference("Groups")
                    .child(groupId)
                    .removeEventListener(l);
        }
    }

    private int indexOfGroup(String groupId) {
        for (int i = 0; i < groupsList.size(); i++) {
            if (groupsList.get(i).getKey().equals(groupId)) return i;
        }
        return -1;
    }

    // ─────────── RecyclerViewFunctionalities ───────────
    @Override
    public void onItemClick(int position) {
        Group g = groupsList.get(position);
        Intent i = new Intent(requireContext(), GroupActivity.class);
        i.putExtra("group_key",           g.getKey());
        i.putExtra("group_name",          g.getName());
        i.putExtra("group_description",   g.getDescription());
        i.putExtra("group_ownerUid",      g.getOwnerUid());
        i.putExtra("group_membersCount",  g.getMembersCount());
        i.putExtra("group_joinCode",      g.getJoinCode());
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(int position) {
        Group g = groupsList.get(position);

        GroupRepository.isCurrentUserOwner(g.getKey(), isOwner -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(isOwner ? "Delete Group" : "Exit Group")
                    .setMessage(isOwner
                            ? "Are you sure you want to delete this group for everyone?"
                            : "Are you sure you want to leave this group?")
                    .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                    .setPositiveButton("Yes", (d, w) -> {
                        d.dismiss();
                        if (isOwner) GroupRepository.deleteGroupIfOwner(g.getKey());
                        else leaveGroup(g);
                    })
                    .show();
        });
        return true;
    }

    private void leaveGroup(Group g) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference("Users")
                .child(uid).child("Groups")
                .child(g.getKey()).removeValue();

        db.getReference("Groups")
                .child(g.getKey()).child("members").child(uid).removeValue();

        db.getReference("Groups")
                .child(g.getKey()).child("membersCount")
                .setValue(ServerValue.increment(-1));
    }

    // ─────────── Cleanup ───────────
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (groupsKeysListener != null) {
            userGroupsRef.removeEventListener(groupsKeysListener);
        }

        // remove each per-group listener
        for (Map.Entry<String, ValueEventListener> e : groupDetailsListeners.entrySet()) {
            FirebaseDatabase.getInstance()
                    .getReference("Groups")
                    .child(e.getKey())
                    .removeEventListener(e.getValue());
        }
        groupDetailsListeners.clear();
    }
}

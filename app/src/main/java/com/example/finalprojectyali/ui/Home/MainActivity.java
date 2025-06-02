package com.example.finalprojectyali.ui.Home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.finalprojectyali.Extras.GuiderDialog;
import com.example.finalprojectyali.Extras.Receivers.AirplaneModeReceiver;
import com.example.finalprojectyali.Models.User;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.Extras.Services.NotificationListenerService;
import com.example.finalprojectyali.databinding.ActivityMainBinding;
import com.example.finalprojectyali.ui.Auth.LoginActivity;
import com.example.finalprojectyali.ui.Home.Fragments.EventsFragment;
import com.example.finalprojectyali.ui.Home.Fragments.ProfileFragment;
import com.example.finalprojectyali.ui.Home.Fragments.SettingsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FirebaseCallback {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private ActivityMainBinding binding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FloatingActionButton addBtn;
    private TextView greetTv;
    private AirplaneModeReceiver airplaneModeReceiver;
    public static User current_user;
    private FirebaseAuth mAuth;

    private enum Screen { EVENTS, PROFILE, SETTINGS }
    private Screen current = Screen.EVENTS;

    /* ─────────────────────────────────────────────────────────── */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Always use dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
        
        current_user = new User();
        getUserDetails(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* Toolbar & drawer */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        /* FAB — initialise BEFORE bottom‑nav listener */
        addBtn = binding.addButtonFab; // direct binding reference
        addBtn.setOnClickListener(v -> {
            if (current == Screen.EVENTS) {
                startActivity(new Intent(this, AddEventActivity.class));
            }
        });

        /* Bottom navigation */
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homescreen) {
                switchFragment(new EventsFragment(), Screen.EVENTS);
            } else if (item.getItemId() == R.id.profile) {
                switchFragment(new ProfileFragment(), Screen.PROFILE);
            }
            return true;
        });
        binding.bottomNavigationView.setSelectedItemId(R.id.homescreen); // triggers listener AFTER everything ready

        /* Greeting */
        greetTv = findViewById(R.id.xp_main_display);
        greetTv.setText("Hi, " + current_user.getName());
        greetTv.setContentDescription("Your name: " + current_user.getName());

        mAuth = FirebaseAuth.getInstance();
        airplaneModeReceiver = new AirplaneModeReceiver();

        // Start notification listener service
        NotificationListenerService.start(this);

        new GuiderDialog(this, "MainActivity",
                "Welcome! This is your events dashboard.").startDialog();
    }

    /* ───────────────── Fragment helper ───────────────── */
    private void switchFragment(Fragment f, Screen target) {
        current = target;
        if (addBtn != null) {
            addBtn.setVisibility(target == Screen.EVENTS ? View.VISIBLE : View.GONE);
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frame_layout, f, target.name());
        ft.commit();
    }

    /* ───────────────── Drawer actions ────────────────── */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_settings:
                switchFragment(new SettingsFragment(), Screen.SETTINGS);
                break;
            case R.id.nav_contact:
                startActivity(new Intent(this, MailContactAct.class));
                break;
            case R.id.nav_donate:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://m.youtube.com/watch?v=UICsbQIKi9s")));
                break;
            case R.id.nav_auth:
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /* ───────────────── Lifecycle ─────────────────────── */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(airplaneModeReceiver,
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(airplaneModeReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser u = mAuth.getCurrentUser();
        if (u == null) startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Service will handle its own cleanup when app is closed
    }

    /* ───────────────── Firebase helper ───────────────── */
    public static void getUserDetails(FirebaseCallback cb) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(FirebaseAuth.getInstance().getUid());
        ref.get().addOnSuccessListener(s -> {
            User u = s.getValue(User.class);
            if (u != null) {
                current_user.setName(u.getName());
                current_user.setPhoneNumber(u.getPhoneNumber());
            } else {
                current_user.setName("User");
                current_user.setPhoneNumber("000");
            }
            cb.onUserDetailsReceived();
        });
    }

    @Override
    public void onUserDetailsReceived() {
        if (greetTv != null) greetTv.setText("Hi, " + current_user.getName());
    }
}

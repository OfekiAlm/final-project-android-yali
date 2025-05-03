package com.example.finalprojectyali.ui.Home;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.finalprojectyali.Extras.GroupRepository;
import com.example.finalprojectyali.Extras.GuiderDialog;
import com.example.finalprojectyali.Extras.Receivers.AirplaneModeReceiver;
import com.example.finalprojectyali.Models.User;
import com.example.finalprojectyali.databinding.ActivityMainBinding;
import com.example.finalprojectyali.ui.Auth.LoginActivity;
import com.example.finalprojectyali.ui.DetailedGroupAct;
import com.example.finalprojectyali.ui.Home.Fragments.HomeFragment;
import com.example.finalprojectyali.ui.Home.Fragments.ProfileFragment;
import com.example.finalprojectyali.R;

import com.example.finalprojectyali.ui.Home.Fragments.SettingsFragment;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

/**
 * MainActivity is the main screen of the app, containing a navigation drawer with various options,
 * a bottom navigation bar, and a floating action button for adding tasks or rewards based on the fragments. It also
 * displays user XP, allows users to sign out, and initializes various fragments.
 * @author Ofek Almog
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FirebaseCallback{

    /** ActivityMainBinding is a generated class that provides easy access to all views in the layout. */
    ActivityMainBinding binding;

    /** AirplaneModeReceiver is a broadcast receiver for detecting airplane mode changes. */
    AirplaneModeReceiver airplaneModeReceiver;

    /** current_user stores the current User object. */
    public static User current_user;

    /** drawerLayout is the layout for the navigation drawer. */
    DrawerLayout drawerLayout;

    /** navigationView is the navigation drawer itself. */
    NavigationView navigationView;

    /** addBtn is the floating action button for adding tasks or rewards. */
    FloatingActionButton addBtn;

    /** mAuth is the instance of the FirebaseAuth class for authentication. */
    FirebaseAuth mAuth;

    /** xpDisplayTv is the TextView that displays the user's XP. */
    TextView xpDisplayTv;

    /** Fragments is an enum representing the three possible fragments on the main screen. */
    enum Fragments{
        HOME,
        PROFILE,
        SETTINGS,
    }

    /**
     * onCreate is called when the activity is starting. It initializes various components and
     * sets listeners for the navigation drawer, bottom navigation bar, and floating action button.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     * saved then this Bundle contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current_user = new User();
        getUserDetails(this);

        init();

        GuiderDialog guiderDialog = new GuiderDialog(this,"MainActivity","Hello there,\nYou're new here. Let me guide you through the pages of the app, What's behind me is the first and the main screen. You can add tasks at the plus button at the bottom of the screen and also navigate to another screens.");
        guiderDialog.startDialog();
        airplaneModeReceiver = new AirplaneModeReceiver();
//        if(AirplaneModeReceiver.isAirplaneMode){
//
//        }

        addBtn.setOnClickListener(view -> {
            if(determineFragment() == Fragments.HOME){
                Intent i = new Intent(getApplicationContext(), DetailedGroupAct.class);
                i.putExtra("from_intent","Add");
                startActivity(i);
            }
            if (determineFragment() == Fragments.PROFILE){
                Toast.makeText(this,"",Toast.LENGTH_LONG).show();
            }
            if (determineFragment() == Fragments.SETTINGS){
                Toast.makeText(this,"",Toast.LENGTH_LONG).show();
            }
        });

        mAuth = FirebaseAuth.getInstance();

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.homescreen:
                    replaceFragment(new HomeFragment(),Fragments.HOME);
                    addBtn.setVisibility(View.VISIBLE);
                    break;
                case R.id.profile:
                    replaceFragment(new ProfileFragment(),Fragments.PROFILE);
                    addBtn.setVisibility(View.GONE);
                    break;
            }
            return true;
        });
        navdrawer_init();
        // TODO: fix this.
//        GroupRepository.isCurrentUserOwner("dummy", any -> {}); // forces class init – optional
//        GroupRepository.createGroup(
//                "Friday BBQ", "Who brings what?", new ArrayList<>(),
//                g  -> Log.d("TEST", "Group created: " + g.getKey()),
//                err -> Log.e("TEST", err.getMessage())
//        );
    }

    /**
     * Initializes the activity by inflating the layout, setting the content view,
     * replacing the fragment, setting up the bottom navigation view and setting the XP display.
     */
    private void init(){
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new HomeFragment(),Fragments.HOME);
        binding.bottomNavigationView.setSelectedItemId(R.id.homescreen);
        binding.bottomNavigationView.setBackground(null);
        addBtn = (FloatingActionButton) binding.addButtonFab.findViewById(R.id.add_button_fab);
        xpDisplayTv = findViewById(R.id.xp_main_display);
        xpDisplayTv.setText("Hi, "+current_user.getPhoneNumber()+"");
        binding.xpMainDisplay.setContentDescription("Your name: "+current_user.getPhoneNumber()+" ");
        //xpDisplayTv.setOnLongClickListener(this);
    }

    /**
     * Initializes the navigation drawer by setting up the toolbar, drawer layout, toggle,
     * and navigation view.
     */
    private void navdrawer_init(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    /**
     Overrides the default behavior of the back button press to close the navigation drawer if it is open,
     otherwise it calls the default behavior of the back button press.
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     This method is called when a menu item in the Navigation Drawer is selected.
     It handles the item selection based on the item ID and performs the corresponding action.
     @param item The menu item that was selected in the Navigation Drawer
    */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.nav_settings:
                Log.i("MoveScreen", "MoveToSettingsScreen:Success");
                //Intent moveToSettings = new Intent(this, SettingsActivity.class);
                //startActivity(moveToSettings);
                replaceFragment(new SettingsFragment(),Fragments.SETTINGS);
                addBtn.setVisibility(View.GONE);
                //Toast.makeText(this,"Default settings is enabled in the current version",Toast.LENGTH_LONG).show();
                break;
            case R.id.nav_contact:
                Log.i("MoveScreen", "MoveToContact  Screen:Success");
                Intent moveToMailContact = new Intent(this, MailContactAct.class);
                startActivity(moveToMailContact);
                break;
            case R.id.nav_donate:
                Log.i("MoveScreen", "MoveToDonateScreen:Success");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate/?hosted_button_id=G7LXD4YAGLL5Y")); // TODO: REPLACE LINK
                startActivity(browserIntent);
                break;
            case R.id.nav_auth:
                Log.i("AuthData", "LogOut:Success");
                sign_out();
                startActivity(new Intent(this, LoginActivity.class));
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     Signs out the current user by calling the signOut() method of FirebaseAuth instance.
     */
    public void sign_out() {
        mAuth.signOut();
    }

    /**
     Called when the activity is becoming visible to the user. This method checks if a user is signed in
     and updates the UI accordingly. If the user is not signed in, it starts the LoginActivity.
     @see LoginActivity
     */
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            startActivity(new Intent(this,LoginActivity.class));
        }
    }

    /**
     Replaces the current fragment in the FrameLayout container with the specified fragment instance.
     @param theInstance the fragment instance to replace the current fragment with
     @param enumVersion an enumeration representing the type of fragment to be replaced
     */
    private void replaceFragment(Fragment theInstance, Fragments enumVersion) {
        String onWhichFragment = "";
        int theFrag = enumVersion.ordinal();
        switch (theFrag){
            case 0:
                onWhichFragment = "HOME";
                break;
            case 2:
                onWhichFragment = "PROFILE";
                break;
            case 3:
                onWhichFragment = "SETTINGS";
                break;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, theInstance,onWhichFragment);
        fragmentTransaction.commit();
    }

    /**
     Determines the current visible fragment by checking if the rewards or profile fragment is visible,
     if not, returns the HOME fragment.
     @return An enum value of the currently visible fragment.
     */
    public Fragments determineFragment(){
        Fragment profileFrag = (Fragment)getSupportFragmentManager().findFragmentByTag("PROFILE");
        Fragment settingsFrag = (Fragment)getSupportFragmentManager().findFragmentByTag("SETTINGS");
        if(settingsFrag != null && settingsFrag.isVisible()){
            return Fragments.SETTINGS;
        }
        else if(profileFrag != null && profileFrag.isVisible()){
            return Fragments.PROFILE;
        }
        return Fragments.HOME;
    }

    /**
     This method is called when the activity is resumed from a paused state.
     It registers an instance of AirplaneModeReceiver to receive broadcast intents for airplane mode changes.
     */
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
    }

    /**
     This method is called when the activity is going into the background and unregistering the airplane mode broadcast receiver to avoid leaks
     or unnecessary processing while the activity is not visible.
     */
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(airplaneModeReceiver);
    }

    /**
     Retrieves user details from the Firebase Realtime Database using the Firebase Authentication user ID
     and updates the {@link #current_user} object accordingly.
     @param callback an interface that provides a callback method for when the user details have been received
     */
    public static void getUserDetails(FirebaseCallback callback){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getUid());
        Task<DataSnapshot> task = ref.get();
        task.addOnSuccessListener(dataSnapshot -> {
            User user_fb = dataSnapshot.getValue(User.class);
            current_user.setName(user_fb.getName());
            current_user.setPhoneNumber(user_fb.getPhoneNumber());
            Log.d("AuthData","got all user details" + current_user.toString());
            callback.onUserDetailsReceived();
        }).addOnFailureListener(e -> {
            // Handle any errors here
            Log.d("AuthData","The opreation is not good\nCause: \n" +e);
        });
    }


    /**
     * onUserDetailsReceived is a method from the FirebaseCallback interface that sets the
     * text of xpDisplayTv to the user's current XP.
     */
    @Override
    public void onUserDetailsReceived() {
        xpDisplayTv.setText("Hi, "+ current_user.getName());
    }
}
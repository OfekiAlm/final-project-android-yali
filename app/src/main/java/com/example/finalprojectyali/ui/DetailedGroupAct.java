package com.example.finalprojectyali.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.finalprojectyali.Extras.GroupRepository;
import com.example.finalprojectyali.Extras.GuiderDialog;
import com.example.finalprojectyali.Models.Group;
import com.example.finalprojectyali.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

/**
 * This activity class is responsible for displaying the details of a group, including its name,
 * description, difficulty, and duration. It also provides functionality to add a new group or
 * update an existing one.
 * @author Ofek Almog
 */
public class DetailedGroupAct extends AppCompatActivity {

    /**
     * The TextInputEditTexts for the group name, description, difficulty and duration.
     */
    TextInputEditText groupNameTv, groupDescTv;

    /**
     * The group being edited or added.
     */
    Group group;

    /**
     * The FloatingActionButton used to submit the form.
     */
    FloatingActionButton submitFormBtn;

    /**
     * A counter used to determine if the form has been submitted before. A UI updating solution.
     */
    int counter = 0;

    /**
     * The name of the activity that started this one.
     */
    String fromActivity;

    /**
     * Indicates whether the user is adding a new group or editing an existing one.
     */
    String userChoice;

    /**
     * Called when the activity is starting. Sets up the views and initializes the group.
     * @param savedInstanceState The saved state of the activity, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed_group);

        init();
        group = new Group();
        determineEditOrAdd(fromActivity);

        GuiderDialog gd = new GuiderDialog(this, "DetailedGroupAct", "This is an explanation for you 😀");
        gd.startDialog();

        submitFormBtn.setOnClickListener(view -> {
            ++counter;
            if (counter == 1) {
                openEditTexts();
                setUpdateIconDrawable();
            } else if (counter >= 2) {
                if (userChoice.equals("AddGroup")) {
                    //addGroupToFirebase();
//                    GroupRepository.createGroup(
//                            "Friday BBQ", "Who brings what?", new ArrayList<>(),
//                            g  -> Log.d("TEST", "Group created: " + g.getKey()),
//                            err -> Log.e("TEST", err.getMessage())
//                    );
                    finish();
                } else {
                    //updateGroupToFirebase();
                    finish();
                }
            }
        });
    }

    /**
     * Initializes the activity by setting up views and retrieving data from the incoming intent.
     */
    private void init() {
        if (getIntent() != null) {
            fromActivity = getIntent().getStringExtra("from_intent");
        }
        groupNameTv = findViewById(R.id.task_name_et);
        groupDescTv = findViewById(R.id.task_desc_et);
        submitFormBtn = findViewById(R.id.fab_edit_add_task);
    }

    /**
     * Determines whether the user is editing or adding a group based on the name of the activity.
     * If the activity name is "Edit", it sets edit to true and calls the {@link #getValuesFromPrevActivityGroup()}
     * method to get the previous group values, and {@link #insertGroupEditTextsValues()} method to insert the values
     * into the edit texts.
     * If the activity name is not "Edit", it sets edit to false, calls the {@link #setAddIconDrawable()}
     * method to set the add icon drawable to the submit button, {@link #openEditTexts()} method to open the edit texts,
     * and increments the counter by one.
     * @param activityName the name of the activity, either "Edit" or "Add"
     */
    private void determineEditOrAdd(String activityName) {
        boolean edit = false;
        if (activityName.equals("Edit"))
            edit = true;
        if (edit) {
            userChoice = "EditGroup";
            getValuesFromPrevActivityGroup();
            insertGroupEditTextsValues();
        } else {
            userChoice = "AddGroup";
            setAddIconDrawable();
            openEditTexts();
            ++counter;
        }
    }

    /**
     * This method retrieves the values of the selected group from the previous activity
     * and sets them to the corresponding fields in the current activity's group object.
     */
    private void getValuesFromPrevActivityGroup() {
        group.setName(getIntent().getStringExtra("selected_group_name"));
        //group.setDesc(getIntent().getStringExtra("selected_group_desc"));
        group.setKey(getIntent().getStringExtra("selected_group_key"));
    }

    /**
     * This method sets the group data to the corresponding fields in the current activity's group object.
     * This method should be called after the {@link #insertGroupEditTextsValues()} method.
     */
    private void insertGroupEditTextsValues() {
        groupNameTv.setText(group.getName());
        groupDescTv.setText(group.getDescription());
    }

    /**
     * Sets the add icon to the submit button in the current activity.
     */
    private void setAddIconDrawable() {
        submitFormBtn.setImageResource(R.drawable.ic_baseline_add);
    }

    /**
     * Sets the update icon to the submit button in the current activity.
     */
    private void setUpdateIconDrawable() {
        submitFormBtn.setImageResource(R.drawable.ic_baseline_update);
    }

    /**
     * Opens the edit texts in the current activity, allowing the user to modify the values of the fields.
     */
    private void openEditTexts() {
        groupNameTv.setFocusable(true);
        groupNameTv.setFocusableInTouchMode(true);

        groupDescTv.setFocusable(true);
        groupDescTv.setFocusableInTouchMode(true);
    }
}

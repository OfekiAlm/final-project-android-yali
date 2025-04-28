package com.example.finalprojectyali.Extras;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.finalprojectyali.R;

/**
 A custom dialog class used to display guide messages for activities to the user. {@link ActivityGuideTracker}
 @author Yali Shem Tov
 */
public class GuiderDialog extends Dialog {

    /**The context of the dialog*/
    private Context mContext;

    /** The name of the activity associated with the dialog */
    private String mActivityName;

    /** The explanation message to display in the dialog */
    private String mExplanation;

    /** The activity tracker instance */
    private ActivityGuideTracker activityTracker;

    /**
     Constructs a new GuiderDialog instance.
     @param context The application context.
     @param activityName The name of the activity to display the guide for.
     @param explanation The message to display to the user.
     */
    public GuiderDialog(Context context, String activityName, String explanation) {
        super(context);
        mContext = context;
        mActivityName = activityName;
        mExplanation = explanation;
    }

    /**
     Initializes the dialog view and shows the dialog.
     */
    private void init() {
        show();
        setContentView(R.layout.custom_guider_dialog);
        TextView explanationTv = findViewById(R.id.explanation_text);
        explanationTv.setText(mExplanation);
        Button okBtn = findViewById(R.id.proceed_guide_btn);

        okBtn.setOnClickListener(view ->
                proceedOk());
    }

    /**
     Starts the dialog for the given activity if it has not been visited before.
     */
    public void startDialog() {
        activityTracker = new ActivityGuideTracker(mContext);
        if (!activityTracker.isVisited(mActivityName)) {
            init();
            activityTracker.setVisited(mActivityName);
        }
    }

    /**
     Dismisses the dialog when the "OK" button is clicked.
     */
    public void proceedOk(){
        dismiss();
    }
}
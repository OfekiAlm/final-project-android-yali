package com.example.finalprojectyali.ui.Home.Fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalprojectyali.Models.User;
import com.example.finalprojectyali.R;
import com.example.finalprojectyali.ui.Auth.RegisterActivity;
import com.example.finalprojectyali.ui.Home.MainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 ProfileFragment is a fragment that displays the user's profile details, including their image, email, and phone number.
 @author Ofek Almog
 */
public class ProfileFragment extends Fragment {

    /** The user's profile image */
    Bitmap imageBitmap;

    /** The view to display the profile image */
    CircleImageView circleImageView;

    /** The user's email address */
    String userEmail;

    /** The views to display the user's email and phone  */
    TextView userEmailTv, userPhoneTv;

    /** The user's phone number */
    String userPhoneNumber;

    /**
     Called immediately after onCreateView() has returned, but before any saved state has been restored in to the view.
     @param view The View returned by onCreateView()
     @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getUserDetails();
        init(view);

    }

    /**
     Initializes the views in this fragment.
     @param view The root view of the fragment
     */
    private void init(View view){
        circleImageView = view.findViewById(R.id.profile_circle_image);
        getImageFromFireBase();

        userEmail = getEmail();
        userEmailTv = view.findViewById(R.id.task_name_et);
        userEmailTv.setText(userEmail);

        userPhoneTv = view.findViewById(R.id.phone_textview_profile_from_fb);
    }

    /**
     Called to create the view hierarchy associated with this fragment.
     @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     @param container The parent view that the fragment's UI should be attached to
     @param savedInstanceState This fragment is being re-constructed from a previous saved state as given here
     @return Return the View for the fragment's UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    /**
     Gets the user's profile image from Firebase Storage and sets it in the {@link #circleImageView} view.
     */
    public void getImageFromFireBase(){
        StorageReference mImageRef = FirebaseStorage.getInstance().getReference().child("profile-images/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
        final long ONE_MEGABYTE = 1024 * 1024;
        mImageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            imageBitmap  = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if(imageBitmap !=null)
                circleImageView.setImageBitmap(imageBitmap);
            //Toast.makeText(getActivity(), "successfully loaded your profile credentials", Toast.LENGTH_LONG).show();
        }).addOnFailureListener(exception -> Toast.makeText(getContext(), "there's no image in firebase stroage for user", Toast.LENGTH_LONG).show());
    }

    /**
     Gets the user's email address from FirebaseAuth.
     @return the user's email address
     */
    public String getEmail(){
        return FirebaseAuth.getInstance().getCurrentUser().getEmail();
    }

    /**
     Gets the user's phone number from Firebase Database and sets it in the {@link #userPhoneTv} TextView.
     */
    public void getUserDetails(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getUid()).child("phoneNumber");
        Task<DataSnapshot> task = ref.get();
        task.addOnSuccessListener(dataSnapshot -> {
            String str_p_num = dataSnapshot.getValue(String.class);
            userPhoneNumber = str_p_num;
            userPhoneTv.setText(userPhoneNumber);
            Log.d("AuthData","lol that's worked actually");
        }).addOnFailureListener(e -> {
            // Handle any errors here
            Log.d("AuthData","The operation is not good\nCause: \n" +e);
        });

    }
}
package com.example.finalprojectyali.Extras;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.recyclerview.widget.RecyclerView;

import com.example.finalprojectyali.Adapters.RecyclerViewFunctionalities;
import java.security.SecureRandom;

/**
 * Utility class with various helper methods.
 * @author Yali Shem Tov
 */
public class Utils {

    /**
     * Checks if the device is connected to the internet.
     *
     * @param context the application or activity context.
     * @return true if the device is connected to the internet, false otherwise.
     */
    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Method to check if the RecyclerViewFunctionalities interface is valid and the item position is not RecyclerView.NO_POSITION.
     * @param recyclerViewFunctionalities The interface implementing the RecyclerView functionalities.
     * @param adapterPos The position of the item in the list.
     * @return True if the interface is valid and the position is not RecyclerView.NO_POSITION, false otherwise.
     */
    public static boolean checkInterfaceValid(RecyclerViewFunctionalities recyclerViewFunctionalities, int adapterPos){
        if(recyclerViewFunctionalities != null){
            int pos = adapterPos;
            if(pos != RecyclerView.NO_POSITION){
                return true;
            }
        }
        return false;
    }
}

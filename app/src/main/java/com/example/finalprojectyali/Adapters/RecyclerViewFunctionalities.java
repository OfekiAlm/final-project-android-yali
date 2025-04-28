package com.example.finalprojectyali.Adapters;

/**
 An interface defining common functionalities for RecyclerView items.
 */
public interface RecyclerViewFunctionalities {

    /**
     Invoked when an item in the RecyclerView is clicked.
     @param position the position of the clicked item in the RecyclerView
     */
    void onItemClick(int position);

    /**
     Invoked when an item in the RecyclerView is long-clicked.
     @param position the position of the long-clicked item in the RecyclerView
     @return true if the long-click event is consumed and false otherwise
     */
    boolean onItemLongClick(int position);
}

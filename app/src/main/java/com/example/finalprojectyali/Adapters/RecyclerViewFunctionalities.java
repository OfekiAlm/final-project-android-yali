package com.example.finalprojectyali.Adapters;

import com.example.finalprojectyali.Models.Event;

/**
 An interface defining common functionalities for RecyclerView items.
 */
public interface RecyclerViewFunctionalities {
    void onEventClick(Event event);
    default boolean onEventLongClick(Event event) { return false; }
    default void onMapIconClick(Event event) { }
}

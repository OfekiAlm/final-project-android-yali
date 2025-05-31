package com.example.finalprojectyali.Models;

public class Notification {
    private String key;
    private String title;
    private String message;
    private String type; // "event_accepted", "event_rejected"
    private String eventId;
    private String eventName;
    private long timestamp;
    private boolean read;
    
    public Notification() {
        // Required for Firebase
    }
    
    public Notification(String title, String message, String type, String eventId, String eventName) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.eventName = eventName;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }
    
    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
} 
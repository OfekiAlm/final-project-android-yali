package com.example.finalprojectyali.Models;

import com.google.firebase.database.Exclude;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Event {

    // Firebase keys ────────────────────────────────────────────
    private String key;              // push‑key under /Events
    private String name;
    private String description;
    private long   creationDate;
    private long   eventDate;        // when the event takes place
    private String locationAddress;  // free‑form address (for Maps)

    private String ownerUid;         // admin UID

    /** uid ➜ "pending" | "accepted" */
    private Map<String, String> members = new HashMap<>();

    private String joinCode;         // 4‑char invite code

    // ─────────── c'tors ───────────
    public Event() {/*needed for Firebase*/}

    public Event(String name, String description, long eventDate,
                 String locationAddress, String ownerUid, String joinCode) {
        this.name            = name;
        this.description     = description;
        this.eventDate       = eventDate;
        this.locationAddress = locationAddress;
        this.ownerUid        = ownerUid;
        this.joinCode        = joinCode;
        this.creationDate    = new Date().getTime();
        this.members.put(ownerUid, "accepted");
    }

    // ─────────── accessors ───────────
    public String getKey()                 { return key; }
    public void   setKey(String key)       { this.key = key; }

    public String getName()                { return name; }
    public void   setName(String n)        { this.name = n; }

    public String getDescription()         { return description; }
    public void   setDescription(String d) { this.description = d; }

    public long   getCreationDate()        { return creationDate; }
    public void   setCreationDate(long d)  { this.creationDate = d; }

    public long   getEventDate()           { return eventDate; }
    public void   setEventDate(long d)     { this.eventDate = d; }

    public String getLocationAddress()     { return locationAddress; }
    public void   setLocationAddress(String a){ this.locationAddress = a; }

    public String getOwnerUid()            { return ownerUid; }
    public void   setOwnerUid(String u)    { this.ownerUid = u; }

    public Map<String,String> getMembers() { return members; }
    public void setMembers(Map<String,String> m){ this.members = m; }

    public String getJoinCode()            { return joinCode; }
    public void   setJoinCode(String c)    { this.joinCode = c; }

    /* Transient helper – NOT stored in Firebase */
    private transient Status status = Status.AVAILABLE;
    @Exclude public  Status getStatus()         { return status; }
    @Exclude public  void   setStatus(Status s) { status = s; }

    public enum Status { ACCEPTED, PENDING, AVAILABLE }
}
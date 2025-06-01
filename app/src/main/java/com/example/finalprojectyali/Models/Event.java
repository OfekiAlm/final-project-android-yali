package com.example.finalprojectyali.Models;

import com.google.firebase.database.Exclude;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Event {

    private String key;              // /Events push-key
    private String name;
    private String description;
    private long   creationDate;
    private long   eventDate;
    private String locationAddress;
    private String ownerUid;

    /** uid → "pending" | "accepted" */
    private Map<String,String> members = new HashMap<>();

    /*──────────── constructors ────────────*/
    public Event() {} // Firebase needs it

    public Event(String name, String description, long eventDate,
                 String locationAddress, String ownerUid) {
        this.name            = name;
        this.description     = description;
        this.eventDate       = eventDate;
        this.locationAddress = locationAddress;
        this.ownerUid        = ownerUid;
        this.creationDate    = new Date().getTime();
        this.members.put(ownerUid, "accepted");
    }

    /*──────────── getters / setters ────────────*/
    public String getKey()                  { return key; }      public void setKey(String k){ key = k; }
    public String getName()                 { return name; }     public void setName(String n){ name = n; }
    public String getDescription()          { return description;}public void setDescription(String d){ description = d; }
    public long   getCreationDate()         { return creationDate;}
    public void   setCreationDate(long c)   { creationDate = c; }
    public long   getEventDate()            { return eventDate; }public void setEventDate(long e){ eventDate = e; }
    public String getLocationAddress()      { return locationAddress;}
    public void   setLocationAddress(String a){ locationAddress = a; }
    public String getOwnerUid()             { return ownerUid; } public void setOwnerUid(String o){ ownerUid = o; }
    public Map<String,String> getMembers()  { return members; }  public void setMembers(Map<String,String> m){ members = m; }

    /*──────────── transient status helper ────────────*/
    private transient Status status = Status.AVAILABLE;
    @Exclude public Status getStatus()          { return status; }
    @Exclude public void   setStatus(Status s)  { status = s; }

    public enum Status { ACCEPTED, PENDING, AVAILABLE }
}

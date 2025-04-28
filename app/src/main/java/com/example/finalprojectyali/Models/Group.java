package com.example.finalprojectyali.Models;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a group.
 */
public class Group {

    /** The unique key of the group (push-key under /groups) */
    private String key;

    /** The name of the group */
    private String name;

    /** The description of the group */
    private String description;

    /** The creation date of the group */
    private Date creationDate;

    /** The owner's UID (admin) */
    private String ownerUid;

    /** The map of member UIDs to their membership status */
    private Map<String, Boolean> members = new HashMap<>();

    /** The number of members in the group */
    private int membersCount;

    /** The join code for the group */
    private String joinCode;

    /** The list of ingredients associated with the group */
    private List<Ingredient> ingredientList;

    /**
     * Default constructor for Firebase.
     */
    public Group() {
        // Required by Firebase
    }

    /**
     * Convenience constructor used when the creator makes a new group.
     */
    public Group(String name, String description, String ownerUid, String joinCode, List<Ingredient> ingredientList) {
        this.name = name;
        this.description = description;
        this.ownerUid = ownerUid;
        this.joinCode = joinCode;
        this.creationDate = new Date();
        this.membersCount = 1;
        this.members.put(ownerUid, true);
        this.ingredientList = ingredientList;
    }

    /**
     * Full constructor for all fields.
     */
    public Group(String name, String description, Date creationDate, int membersCount, List<Ingredient> ingredientList, String key, String ownerUid, Map<String, Boolean> members, String joinCode) {
        this.name = name;
        this.description = description;
        this.creationDate = creationDate;
        this.membersCount = membersCount;
        this.ingredientList = ingredientList;
        this.key = key;
        this.ownerUid = ownerUid;
        this.members = members != null ? members : new HashMap<>();
        this.joinCode = joinCode;
    }

    // ==== Getters and Setters ====

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    public int getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public List<Ingredient> getIngredientList() {
        return ingredientList;
    }

    public void setIngredientList(List<Ingredient> ingredientList) {
        this.ingredientList = ingredientList;
    }

    @Override
    public String toString() {
        return "Group{" +
                "key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", creationDate=" + creationDate +
                ", ownerUid='" + ownerUid + '\'' +
                ", members=" + members +
                ", membersCount=" + membersCount +
                ", joinCode='" + joinCode + '\'' +
                ", ingredientList=" + ingredientList +
                '}';
    }
}
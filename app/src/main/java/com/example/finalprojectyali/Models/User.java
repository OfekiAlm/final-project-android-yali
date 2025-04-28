package com.example.finalprojectyali.Models;

/**
 * Represents a user
 * Author: Yali Shem Tov
 */
public class User {

    /** The phone number of the user */
    private String phoneNumber;

    /** The full name of the user */
    private String name;

    /** The email of the user */
    private String email;

    /**
     * Default constructor for Firebase.
     */
    public User() { }

    /**
     * Constructor for creating a user with a given phone number.
     * @param phoneNum the phone number of the user
     * @param name the name of the user
     * @param email the email of the user
     */
    public User(String phoneNum, String name, String email) {
        this.phoneNumber = phoneNum;
        this.name = name;
        this.email = email;
    }

    /**
     * Sets the phone number of the user.
     * @param phoneNumber the phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Sets the full name of the user.
     * @param name the full name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the email of the user.
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the phone number of the user.
     * @return the phone number
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns the full name of the user.
     * @return the full name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the email of the user.
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns a string representation of the User object.
     * @return a string representation of the User object
     */
    @Override
    public String toString() {
        return "User{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}

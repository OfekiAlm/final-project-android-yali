package com.example.finalprojectyali.Models;

/**
 * Represents an ingredient.
 */
public class Ingredient {
    /** The name of the ingredient */
    private String name;
    /** The price of the ingredient */
    private double price;
    /** The quantity of the ingredient */
    private int quantity;

    /**
     * Default constructor for Firebase.
     */
    public Ingredient() {
    }

    /**
     * Constructor for creating an ingredient with given values.
     * @param name the name of the ingredient
     * @param price the price of the ingredient
     * @param quantity the quantity of the ingredient
     */
    public Ingredient(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    /**
     * Sets the name of the ingredient.
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the ingredient.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the price of the ingredient.
     * @param price the price to set
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Returns the price of the ingredient.
     * @return the price
     */
    public double getPrice() {
        return price;
    }

    /**
     * Sets the quantity of the ingredient.
     * @param quantity the quantity to set
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the quantity of the ingredient.
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns a string representation of the Ingredient object.
     * @return a string representation of the Ingredient object.
     */
    @Override
    public String toString() {
        return "Ingredient{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
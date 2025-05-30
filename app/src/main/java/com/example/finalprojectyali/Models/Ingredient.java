package com.example.finalprojectyali.Models;

/**
 * Represents a single ingredient in a group’s shopping list.
 *
 * <p>The {@code key} property holds the Firebase child-ID (push-ID) used
 * to store this ingredient under <code>/groups/&lt;groupKey&gt;/ingredients/&lt;key&gt;</code>.
 * Keep it <strong>public-getter / public-setter</strong> so Firebase can
 * (de)serialize it automatically.</p>
 */
public class Ingredient {

    /* Firebase child id (set by push().getKey()) */
    private String key;

    /* Basic fields ------------------------------------------------------- */
    private String name;
    private double price;
    private int quantity;

    /* Acquisition info --------------------------------------------------- */
    private boolean acquired;     // true if already bought
    private String acquiredBy;   // display name of the buyer
    private long acquiredAt;   // epoch-ms when bought

    /* ------------------------------------------------------------------- */
    /*  Constructors                                                       */
    /* ------------------------------------------------------------------- */

    /**
     * Empty constructor required by Firebase.
     */
    @SuppressWarnings("unused")
    public Ingredient() {
    }

    public Ingredient(String name,
                      double price,
                      int quantity,
                      boolean acquired,
                      String acquiredBy,
                      long acquiredAt) {

        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.acquired = acquired;
        this.acquiredBy = acquiredBy;
        this.acquiredAt = acquiredAt;
    }

    /* ------------------------------------------------------------------- */
    /*  Key (Firebase push-ID)                                             */
    /* ------------------------------------------------------------------- */
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /* ------------------------------------------------------------------- */
    /*  Basic getters / setters                                            */
    /* ------------------------------------------------------------------- */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAcquired() {
        return acquired;
    }

    public void setAcquired(boolean a) {
        this.acquired = a;
    }

    public String getAcquiredBy() {
        return acquiredBy;
    }

    public void setAcquiredBy(String u) {
        this.acquiredBy = u;
    }

    public long getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(long t) {
        this.acquiredAt = t;
    }

    /* ------------------------------------------------------------------- */
    /*  Debug print                                                        */
    /* ------------------------------------------------------------------- */
    @Override
    public String toString() {
        return "Ingredient{" +
                "key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", acquired=" + acquired +
                ", acquiredBy='" + acquiredBy + '\'' +
                ", acquiredAt=" + acquiredAt +
                '}';
    }
}

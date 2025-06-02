package com.example.finalprojectyali.Models;

/**
 * Represents a single ingredient in a group's shopping list.
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

    /* Creator info ------------------------------------------------------- */
    private String createdByUID;   // UID of the user who created this ingredient
    private String createdByName;  // Name of the user who created this ingredient
    private long createdAt;        // epoch-ms when created

    /* Acquisition info --------------------------------------------------- */
    private boolean acquired;     // true if already bought
    private String acquiredByUID;   // UId of the buyer
    private String acquiredByName; // Name of the buyer
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
                      String createdByUID,
                      String createdByName,
                      long createdAt,
                      String acquiredByUID,
                      String acquiredByName,
                      long acquiredAt) {

        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.acquired = acquired;
        this.createdByUID = createdByUID;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.acquiredByUID = acquiredByUID;
        this.acquiredByName = acquiredByName;
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

    public String getAcquiredByUID() {
        return acquiredByUID;
    }

    public void setAcquiredByUID(String u) {
        this.acquiredByUID = u;
    }

    public long getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(long t) {
        this.acquiredAt = t;
    }

    public String getAcquiredByName() {
        return acquiredByName;
    }

    public void setAcquiredByName(String acquiredByName) {
        this.acquiredByName = acquiredByName;
    }

    /* ------------------------------------------------------------------- */
    /*  Creator getters / setters                                          */
    /* ------------------------------------------------------------------- */
    public String getCreatedByUID() {
        return createdByUID;
    }

    public void setCreatedByUID(String createdByUID) {
        this.createdByUID = createdByUID;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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
                ", createdBy='" + createdByUID + '\'' +
                ", createdAt=" + createdAt +
                ", acquired=" + acquired +
                ", acquiredBy='" + acquiredByUID + '\'' +
                ", acquiredAt=" + acquiredAt +
                '}';
    }
}

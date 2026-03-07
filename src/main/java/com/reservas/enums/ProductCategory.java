// src/main/java/com/reservas/enums/ProductCategory.java
package com.reservas.enums;

public enum ProductCategory {
    BEBIDA("Bebida"),
    SNACK("Snack"),
    COMIDA("Comida"),
    OTROS("Otros");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
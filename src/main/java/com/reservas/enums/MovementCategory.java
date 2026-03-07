// src/main/java/com/reservas/enums/MovementCategory.java
package com.reservas.enums;

public enum MovementCategory {
    // Ingresos automáticos
    COURT_PAYMENT("Pago de turno"),
    PRODUCT_SALE("Venta de producto"),

    // Ingresos manuales
    OTHER_INCOME("Otro ingreso"),

    // Gastos
    PURCHASE_DRINKS("Compra bebidas"),
    PURCHASE_SUPPLIES("Compra insumos"),
    PURCHASE_OTHER("Otras compras"),
    SERVICE_ELECTRICITY("Luz"),
    SERVICE_WATER("Agua"),
    SERVICE_INTERNET("Internet"),
    SERVICE_OTHER("Otros servicios"),
    MAINTENANCE("Mantenimiento"),
    SALARY("Sueldos"),
    OWNER_WITHDRAWAL("Retiro del dueño"),
    OTHER_EXPENSE("Otro gasto");

    private final String displayName;

    MovementCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
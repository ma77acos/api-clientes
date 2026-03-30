// src/main/java/com/reservas/enums/TableSessionStatus.java
package com.reservas.enums;

public enum TableSessionStatus {
    OPEN,       // Mesa abierta, acumulando productos
    CLOSED,     // Pagada y cerrada
    CANCELLED   // Cancelada (sin cobrar)
}
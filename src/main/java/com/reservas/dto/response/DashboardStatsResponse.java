// src/main/java/com/reservas/dto/response/DashboardStatsResponse.java
package com.reservas.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    // Contadores
    private Integer confirmedReservations;
    private Integer pendingPaymentReservations; // CONFIRMED pero NO isFullyPaid
    private Integer totalCourts;

    // Ingresos del día
    private BigDecimal cashRevenue;        // Suma de pagos CASH
    private BigDecimal electronicRevenue;  // Suma de pagos ELECTRONIC
    private BigDecimal totalRevenue;       // cashRevenue + electronicRevenue

    // Desglose de productos
    private BigDecimal productsCash;
    private BigDecimal productsElectronic;
    private BigDecimal productsTotal;

    // Total general
    private BigDecimal grandTotalCash;
    private BigDecimal grandTotalElectronic;
    private BigDecimal grandTotal;
}
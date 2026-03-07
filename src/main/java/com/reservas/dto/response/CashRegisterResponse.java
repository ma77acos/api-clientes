// src/main/java/com/reservas/dto/response/CashRegisterResponse.java
package com.reservas.dto.response;

import com.reservas.enums.CashRegisterStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterResponse {

    private Long id;
    private LocalDate date;
    private CashRegisterStatus status;

    // Apertura
    private BigDecimal openingAmount;
    private LocalDateTime openedAt;
    private String openedByName;

    // Totales calculados
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal expectedAmount;

    // Desglose de ingresos
    private BigDecimal courtPaymentsTotal;
    private BigDecimal productSalesTotal;
    private BigDecimal otherIncomeTotal;

    // Desglose de egresos
    private BigDecimal purchasesTotal;
    private BigDecimal servicesTotal;
    private BigDecimal maintenanceTotal;
    private BigDecimal salariesTotal;
    private BigDecimal withdrawalsTotal;
    private BigDecimal otherExpensesTotal;

    // Cierre (si está cerrada)
    private BigDecimal actualAmount;
    private BigDecimal difference;
    private BigDecimal carryOverAmount;
    private LocalDateTime closedAt;
    private String closedByName;
    private String closingNotes;

    // Movimientos
    private List<CashMovementResponse> movements;
}
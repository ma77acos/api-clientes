// src/main/java/com/reservas/dto/response/CashRegisterSummaryResponse.java
package com.reservas.dto.response;

import com.reservas.enums.CashRegisterStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterSummaryResponse {

    private Long id;
    private LocalDate date;
    private CashRegisterStatus status;
    private BigDecimal openingAmount;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private BigDecimal difference;
}
// src/main/java/com/reservas/dto/response/RecurringDateDetailResponse.java
package com.reservas.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDateDetailResponse {

    private Long recurringId;
    private Long courtId;
    private String courtName;
    private String complexName;
    private LocalDate date;
    private LocalTime time;
    private BigDecimal price;
    private String status;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String notes;
    private String dayOfWeekDisplay;

    // Campos de cobro
    private Boolean isFullyPaid;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private BigDecimal productsTotal;
    private BigDecimal grandTotal;

    // Listas
    private List<PlayerPaymentResponse> playerPayments;
    private List<ExtraProductResponse> extraProducts;
}

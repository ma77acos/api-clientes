// src/main/java/com/reservas/dto/response/ReservationResponse.java
package com.reservas.dto.response;

import com.reservas.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private ReservationStatus status;
    private Long courtId;
    private String courtName;
    private String complexName;
    private LocalDate date;
    private LocalTime time;
    private BigDecimal price;
    private LocalDateTime createdAt;

    // ✅ Campos adicionales para admin
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String notes;
    private Boolean createdByAdmin;

    private Boolean isRecurring;
    private Long recurringId;

    private Boolean isFullyPaid;
}
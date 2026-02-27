// src/main/java/com/reservas/dto/response/RecurringReservationResponse.java
package com.reservas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.reservas.enums.RecurringStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringReservationResponse {
    private Long id;
    private Long courtId;
    private String courtName;
    private String complexName;
    private DayOfWeek dayOfWeek;
    private String dayOfWeekDisplay; // "Lunes", "Martes", etc.

    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    private BigDecimal price;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String notes;
    private LocalDate startDate;
    private LocalDate endDate;
    private RecurringStatus status;
    private LocalDateTime createdAt;
}
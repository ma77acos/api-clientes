// src/main/java/com/reservas/dto/response/ComplexConfigurationResponse.java
package com.reservas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class ComplexConfigurationResponse {
    private Long id;
    private Long complexId;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private Integer slotDurationMinutes;
    private Integer daysAdvanceBooking;
    private Integer cancellationHours;
}
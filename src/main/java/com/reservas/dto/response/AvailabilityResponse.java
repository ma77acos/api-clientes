// src/main/java/com/reservas/dto/response/AvailabilityResponse.java
package com.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {
    private Long courtId;
    private LocalDate date;
    private List<SlotResponse> slots;
}
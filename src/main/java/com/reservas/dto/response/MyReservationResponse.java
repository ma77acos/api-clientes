// src/main/java/com/reservas/dto/response/MyReservationResponse.java
package com.reservas.dto.response;

import com.reservas.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyReservationResponse {
    private Long id;
    private String complexName;
    private String courtName;
    private LocalDate date;
    private LocalTime time;
    private ReservationStatus status;
}
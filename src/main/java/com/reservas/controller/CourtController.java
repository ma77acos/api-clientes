// src/main/java/com/reservas/controller/CourtController.java
package com.reservas.controller;

import com.reservas.dto.response.AvailabilityResponse;
import com.reservas.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtController {

    private final AvailabilityService availabilityService;

    @GetMapping("/{courtId}/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AvailabilityResponse response = availabilityService.getAvailability(courtId, date);
        return ResponseEntity.ok(response);
    }
}
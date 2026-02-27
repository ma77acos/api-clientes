// src/main/java/com/reservas/controller/ReservationController.java
package com.reservas.controller;

import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.request.ReservationRequest;
import com.reservas.dto.response.MyReservationResponse;
import com.reservas.dto.response.ReservationResponse;
import com.reservas.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/recurring")
    public ResponseEntity<List<ReservationResponse>> createRecurringReservation(
            @Valid @RequestBody RecurringReservationRequest request) {
        List<ReservationResponse> responses = reservationService.createRecurringReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MyReservationResponse>> getMyReservations() {
        List<MyReservationResponse> response = reservationService.getMyReservations();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }
}
// src/main/java/com/reservas/controller/RecurringReservationController.java
package com.reservas.controller;

import com.reservas.dto.request.RecurringExceptionRequest;
import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.response.RecurringReservationResponse;
import com.reservas.dto.response.ReservationResponse;
import com.reservas.service.RecurringReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/recurring")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
public class RecurringReservationController {

    private final RecurringReservationService recurringService;

    /**
     * Crear reserva fija
     */
    @PostMapping
    public ResponseEntity<RecurringReservationResponse> create(
            @Valid @RequestBody RecurringReservationRequest request) {  // ← Cambio aquí
        RecurringReservationResponse response = recurringService.createRecurringReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Listar reservas fijas del complejo
     */
    @GetMapping
    public ResponseEntity<List<RecurringReservationResponse>> list() {
        List<RecurringReservationResponse> reservations = recurringService.getComplexRecurringReservations();
        return ResponseEntity.ok(reservations);
    }

    /**
     * Cancelar un día específico (excepción)
     */
    @PostMapping("/exception")
    public ResponseEntity<Void> cancelSpecificDate(
            @Valid @RequestBody RecurringExceptionRequest request) {
        recurringService.cancelSpecificDate(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Restaurar un día cancelado
     */
    @DeleteMapping("/{id}/exception")
    public ResponseEntity<Void> restoreDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        recurringService.restoreSpecificDate(id, date);
        return ResponseEntity.ok().build();
    }

    /**
     * Cancelar permanentemente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPermanently(@PathVariable Long id) {
        recurringService.cancelPermanently(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivar reserva cancelada
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        recurringService.reactivate(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringReservationResponse> getRecurringReservationById(@PathVariable Long id) {
        RecurringReservationResponse response = recurringService.getRecurringReservationById(id);
        return ResponseEntity.ok(response);
    }
}
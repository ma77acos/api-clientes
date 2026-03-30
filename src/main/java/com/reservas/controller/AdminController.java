// src/main/java/com/reservas/controller/AdminController.java
package com.reservas.controller;

import com.reservas.dto.request.AdminReservationRequest;
import com.reservas.dto.response.DashboardStatsResponse;
import com.reservas.dto.response.ReservationResponse;
import com.reservas.enums.ReservationStatus;
import com.reservas.service.AdminReservationService;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
public class AdminController {

    private final AdminReservationService adminReservationService;

    /**
     * Crear reserva administrativa (sin pago)
     */
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody AdminReservationRequest request) {
        ReservationResponse response = adminReservationService.createAdminReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener reservas del día
     */
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ReservationResponse> reservations = adminReservationService.getComplexReservations(date);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Obtener reservas con filtros
     */
    @GetMapping("/reservations/filter")
    public ResponseEntity<List<ReservationResponse>> getReservationsFiltered(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ReservationStatus status) {
        List<ReservationResponse> reservations = adminReservationService
                .getComplexReservations(startDate, endDate, status);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Cancelar reserva
     */
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        adminReservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener estadísticas del día para el dashboard
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DashboardStatsResponse stats = adminReservationService.getDashboardStats(date);
        return ResponseEntity.ok(stats);
    }
}
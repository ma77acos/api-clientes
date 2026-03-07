// src/main/java/com/reservas/controller/RecurringReservationController.java
package com.reservas.controller;

import com.reservas.dto.request.AddRecurringPaymentRequest;
import com.reservas.dto.request.AddRecurringProductRequest;
import com.reservas.dto.request.RecurringExceptionRequest;
import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.response.*;
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

    // ==================== ENDPOINTS EXISTENTES ====================

    @PostMapping
    public ResponseEntity<RecurringReservationResponse> create(
            @Valid @RequestBody RecurringReservationRequest request) {
        RecurringReservationResponse response = recurringService.createRecurringReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecurringReservationResponse>> list() {
        List<RecurringReservationResponse> reservations = recurringService.getComplexRecurringReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringReservationResponse> getRecurringReservationById(@PathVariable Long id) {
        RecurringReservationResponse response = recurringService.getRecurringReservationById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exception")
    public ResponseEntity<Void> cancelSpecificDate(
            @Valid @RequestBody RecurringExceptionRequest request) {
        recurringService.cancelSpecificDate(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/exception")
    public ResponseEntity<Void> restoreDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        recurringService.restoreSpecificDate(id, date);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPermanently(@PathVariable Long id) {
        recurringService.cancelPermanently(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        recurringService.reactivate(id);
        return ResponseEntity.ok().build();
    }

    // ==================== NUEVOS ENDPOINTS - DETALLE POR FECHA ====================

    @GetMapping("/{id}/date/{date}")
    public ResponseEntity<RecurringDateDetailResponse> getDateDetail(
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RecurringDateDetailResponse response = recurringService.getRecurringDateDetail(id, date);
        return ResponseEntity.ok(response);
    }

    // ==================== NUEVOS ENDPOINTS - PAGOS ====================

    @PostMapping("/{id}/payments")
    public ResponseEntity<PlayerPaymentResponse> addPayment(
            @PathVariable Long id,
            @Valid @RequestBody AddRecurringPaymentRequest request) {
        PlayerPaymentResponse response = recurringService.addRecurringPayment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<Void> removePayment(
            @PathVariable Long id,
            @PathVariable Long paymentId) {
        recurringService.removeRecurringPayment(id, paymentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== NUEVOS ENDPOINTS - PRODUCTOS ====================

    @PostMapping("/{id}/products")
    public ResponseEntity<ExtraProductResponse> addProduct(
            @PathVariable Long id,
            @Valid @RequestBody AddRecurringProductRequest request) {
        ExtraProductResponse response = recurringService.addRecurringProduct(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/products/{productId}")
    public ResponseEntity<Void> removeProduct(
            @PathVariable Long id,
            @PathVariable Long productId) {
        recurringService.removeRecurringProduct(id, productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/products/{productId}/pay")
    public ResponseEntity<ExtraProductResponse> markProductAsPaid(
            @PathVariable Long id,
            @PathVariable Long productId,
            @RequestParam String method) {
        ExtraProductResponse response = recurringService.markRecurringProductAsPaid(id, productId, method);
        return ResponseEntity.ok(response);
    }
}
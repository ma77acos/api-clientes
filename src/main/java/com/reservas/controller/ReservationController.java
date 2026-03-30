// src/main/java/com/reservas/controller/ReservationController.java
package com.reservas.controller;

import com.reservas.dto.request.AddExtraProductRequest;
import com.reservas.dto.request.AddPlayerPaymentRequest;
import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.request.ReservationRequest;
import com.reservas.dto.response.*;
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

    // ==================== ENDPOINTS EXISTENTES ====================

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

    // ==================== 🆕 NUEVO ENDPOINT - DETALLE ====================

    /**
     * Obtiene el detalle completo de una reserva incluyendo pagos y productos
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<ReservationDetailResponse> getReservationDetail(@PathVariable Long id) {
        ReservationDetailResponse response = reservationService.getReservationDetail(id);
        return ResponseEntity.ok(response);
    }

    // ==================== 🆕 ENDPOINTS - PAGOS DE JUGADORES ====================

    /**
     * Registra el pago de un jugador para el turno
     */
    @PostMapping("/{id}/player-payments")
    public ResponseEntity<PlayerPaymentResponse> addPlayerPayment(
            @PathVariable Long id,
            @Valid @RequestBody AddPlayerPaymentRequest request) {
        PlayerPaymentResponse response = reservationService.addPlayerPayment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Elimina un pago registrado
     */
    @DeleteMapping("/{id}/player-payments/{paymentId}")
    public ResponseEntity<Void> removePlayerPayment(
            @PathVariable Long id,
            @PathVariable Long paymentId) {
        reservationService.removePlayerPayment(id, paymentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 🆕 ENDPOINTS - PRODUCTOS EXTRAS ====================

    /**
     * Agrega un producto extra a la reserva (bebidas, snacks, etc.)
     */
    @PostMapping("/{id}/extra-products")
    public ResponseEntity<ExtraProductResponse> addExtraProduct(
            @PathVariable Long id,
            @Valid @RequestBody AddExtraProductRequest request) {
        ExtraProductResponse response = reservationService.addExtraProduct(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Elimina un producto extra
     */
    @DeleteMapping("/{id}/extra-products/{productId}")
    public ResponseEntity<Void> removeExtraProduct(
            @PathVariable Long id,
            @PathVariable Long productId) {
        reservationService.removeExtraProduct(id, productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marca un producto como pagado
     */
    @PatchMapping("/{id}/extra-products/{productId}/pay")
    public ResponseEntity<ExtraProductResponse> markProductAsPaid(
            @PathVariable Long id,
            @PathVariable Long productId,
            @RequestParam String method) {
        ExtraProductResponse response = reservationService.markProductAsPaid(id, productId, method);
        return ResponseEntity.ok(response);
    }
}
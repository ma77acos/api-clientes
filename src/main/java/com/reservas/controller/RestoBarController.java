// src/main/java/com/reservas/controller/RestoBarController.java
package com.reservas.controller;

import com.reservas.dto.request.*;
import com.reservas.dto.response.*;
import com.reservas.service.RestoBarService;
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
@RequestMapping("/admin/restobar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
public class RestoBarController {

    private final RestoBarService restoBarService;

    // ========================================================================
    // VENTA RÁPIDA
    // ========================================================================

    /**
     * Crear venta rápida (cobro inmediato)
     */
    @PostMapping("/quick-sale")
    public ResponseEntity<QuickSaleResponse> createQuickSale(
            @Valid @RequestBody QuickSaleRequest request) {
        QuickSaleResponse response = restoBarService.createQuickSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========================================================================
    // SESIONES DE MESA
    // ========================================================================

    /**
     * Abrir sesión de mesa
     */
    @PostMapping("/sessions")
    public ResponseEntity<TableSessionResponse> openSession(
            @Valid @RequestBody OpenTableSessionRequest request) {
        TableSessionResponse response = restoBarService.openTableSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener todas las sesiones abiertas
     */
    @GetMapping("/sessions/open")
    public ResponseEntity<List<TableSessionResponse>> getOpenSessions() {
        List<TableSessionResponse> sessions = restoBarService.getOpenSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtener sesión por ID
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<TableSessionResponse> getSessionById(@PathVariable Long id) {
        TableSessionResponse session = restoBarService.getSessionById(id);
        return ResponseEntity.ok(session);
    }

    /**
     * Agregar item a sesión
     */
    @PostMapping("/sessions/{id}/items")
    public ResponseEntity<TableSessionResponse> addItemToSession(
            @PathVariable Long id,
            @Valid @RequestBody AddTableSessionItemRequest request) {
        TableSessionResponse response = restoBarService.addItemToSession(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Eliminar item de sesión
     */
    @DeleteMapping("/sessions/{id}/items/{itemId}")
    public ResponseEntity<TableSessionResponse> removeItemFromSession(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        TableSessionResponse response = restoBarService.removeItemFromSession(id, itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * Agregar pago a sesión
     */
    @PostMapping("/sessions/{id}/payments")
    public ResponseEntity<TableSessionResponse> addPaymentToSession(
            @PathVariable Long id,
            @Valid @RequestBody AddTableSessionPaymentRequest request) {
        TableSessionResponse response = restoBarService.addPaymentToSession(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cerrar sesión (requiere estar pagada)
     */
    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<TableSessionResponse> closeSession(@PathVariable Long id) {
        TableSessionResponse response = restoBarService.closeSession(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancelar sesión (sin pagos registrados)
     */
    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<TableSessionResponse> cancelSession(@PathVariable Long id) {
        TableSessionResponse response = restoBarService.cancelSession(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Historial de sesiones cerradas por fecha
     */
    @GetMapping("/sessions/history")
    public ResponseEntity<List<TableSessionResponse>> getSessionHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TableSessionResponse> sessions = restoBarService.getSessionHistory(date);
        return ResponseEntity.ok(sessions);
    }

    // ========================================================================
    // VENTA A CANCHA
    // ========================================================================

    /**
     * Agregar producto a una cancha/turno
     */
    @PostMapping("/court-sale")
    public ResponseEntity<ExtraProductResponse> addProductToCourt(
            @Valid @RequestBody AddProductToCourtRequest request) {
        ExtraProductResponse response = restoBarService.addProductToCourt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
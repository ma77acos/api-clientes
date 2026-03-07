// src/main/java/com/reservas/controller/CashRegisterController.java
package com.reservas.controller;

import com.reservas.dto.request.*;
import com.reservas.dto.response.*;
import com.reservas.service.CashRegisterService;
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
@RequestMapping("/admin/cash-register")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    /**
     * Obtener caja del día actual
     */
    @GetMapping("/current")
    public ResponseEntity<CashRegisterResponse> getCurrentCashRegister() {
        CashRegisterResponse response = cashRegisterService.getCurrentCashRegister();
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener caja por fecha
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<CashRegisterResponse> getCashRegisterByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        CashRegisterResponse response = cashRegisterService.getCashRegisterByDate(date);
        return ResponseEntity.ok(response);
    }

    /**
     * Abrir caja del día
     */
    @PostMapping("/open")
    public ResponseEntity<CashRegisterResponse> openCashRegister(
            @Valid @RequestBody OpenCashRegisterRequest request) {
        CashRegisterResponse response = cashRegisterService.openCashRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cerrar caja del día
     */
    @PostMapping("/close")
    public ResponseEntity<CashRegisterResponse> closeCashRegister(
            @Valid @RequestBody CloseCashRegisterRequest request) {
        CashRegisterResponse response = cashRegisterService.closeCashRegister(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Agregar movimiento a la caja
     */
    @PostMapping("/movements")
    public ResponseEntity<CashMovementResponse> addMovement(
            @Valid @RequestBody CashMovementRequest request) {
        CashMovementResponse response = cashRegisterService.addMovement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Eliminar movimiento
     */
    @DeleteMapping("/movements/{id}")
    public ResponseEntity<Void> deleteMovement(@PathVariable Long id) {
        cashRegisterService.deleteMovement(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Historial de cajas
     */
    @GetMapping("/history")
    public ResponseEntity<List<CashRegisterSummaryResponse>> getCashRegisterHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CashRegisterSummaryResponse> response =
                cashRegisterService.getCashRegisterHistory(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
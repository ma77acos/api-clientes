// src/main/java/com/reservas/controller/ComplexConfigurationController.java
package com.reservas.controller;

import com.reservas.dto.request.ComplexConfigurationRequest;
import com.reservas.dto.response.ComplexConfigurationResponse;
import com.reservas.service.ComplexConfigurationService;
import com.reservas.service.MercadoPagoOAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/configuration")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
@Slf4j
public class ComplexConfigurationController {

    private final ComplexConfigurationService configService;
    private final MercadoPagoOAuthService mpOAuthService;

    @GetMapping
    public ResponseEntity<ComplexConfigurationResponse> getConfiguration() {
        return ResponseEntity.ok(configService.getConfiguration());
    }

    @PutMapping
    public ResponseEntity<ComplexConfigurationResponse> saveConfiguration(
            @Valid @RequestBody ComplexConfigurationRequest request) {
        return ResponseEntity.ok(configService.saveConfiguration(request));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<String>> getAvailableSlots() {
        List<LocalTime> slots = configService.getAvailableSlotTimes();

        List<String> formattedSlots = slots.stream()
                .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm")))
                .toList();

        return ResponseEntity.ok(formattedSlots);
    }

    // ========================================
    // 💳 Mercado Pago OAuth
    // ========================================

    /**
     * 1️⃣ Generar URL de autorización para vincular cuenta de MP
     */
    @GetMapping("/mercadopago/connect")
    public ResponseEntity<?> getMercadoPagoAuthUrl() {
        log.info("🔗 Generando URL de autorización de Mercado Pago");

        String authUrl = mpOAuthService.getAuthorizationUrl();

        return ResponseEntity.ok(Map.of(
                "authUrl", authUrl,
                "message", "Redirigir al usuario a esta URL para autorizar"
        ));
    }

    /**
     * 2️⃣ Verificar estado de vinculación de MP
     */
    @GetMapping("/mercadopago/status")
    public ResponseEntity<?> getMercadoPagoStatus() {
        log.info("📊 Verificando estado de vinculación MP");

        Map<String, Object> status = mpOAuthService.getConnectionStatus();

        return ResponseEntity.ok(status);
    }

    /**
     * 3️⃣ Desvincular cuenta de Mercado Pago
     */
    @DeleteMapping("/mercadopago/disconnect")
    public ResponseEntity<?> disconnectMercadoPago() {
        log.info("🔌 Desvinculando cuenta de Mercado Pago");

        mpOAuthService.disconnect();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cuenta de Mercado Pago desvinculada correctamente"
        ));
    }
}
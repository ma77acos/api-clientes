// src/main/java/com/reservas/controller/TestMPController.java
package com.reservas.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.reservas.entity.*;
import com.reservas.service.MercadoPagoService;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestMPController {

    @Autowired
    private MercadoPagoService mpService;

    @GetMapping("/mp-preference")
    public ResponseEntity<?> testMP() {
        try {
            log.info("🧪 TEST MercadoPago");

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Reserva Test - Cancha 1")
                    .quantity(1)
                    .unitPrice(new BigDecimal("1500.00"))
                    .currencyId("ARS")
                    .build();

            // SIN autoReturn, SIN backUrls
            PreferenceRequest request = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference("test-123")
                    .build();

            Preference pref = new PreferenceClient().create(request);

            log.info("✅ OK - ID: {}", pref.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "preferenceId", pref.getId(),
                    "initPoint", pref.getInitPoint(),
                    "sandboxInitPoint", pref.getSandboxInitPoint()
            ));

        } catch (MPApiException e) {
            log.error("❌ Error: {}", e.getApiResponse().getContent());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getApiResponse().getContent()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mp-service")
    public ResponseEntity<?> testService() {
        try {
            // Mock data
            User user = new User();
            user.setEmail("test@example.com");
            user.setDisplayName("Test User");

            Complex complex = new Complex();
            complex.setName("Complejo Test");

            Court court = new Court();
            court.setName("Cancha 1");
            court.setComplex(complex);

            Reservation reservation = new Reservation();
            reservation.setId(999L);
            reservation.setPrice(new BigDecimal("1500.00"));
            reservation.setDate(java.time.LocalDate.now());
            reservation.setTime(java.time.LocalTime.of(18, 0));
            reservation.setUser(user);
            reservation.setCourt(court);

            Payment payment = new Payment();
            payment.setId(999L);

            // Llamar al service (ahora retorna Map)
            Map<String, Object> result = mpService.createPaymentPreference(reservation, payment);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "preferenceId", result.get("id"),
                    "initPoint", result.get("init_point"),
                    "sandboxInitPoint", result.get("sandbox_init_point"),
                    "backUrls", result.get("back_urls")
            ));

        } catch (Exception e) {
            log.error("❌ Error:", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
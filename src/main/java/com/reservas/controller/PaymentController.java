// src/main/java/com/reservas/controller/PaymentController.java
package com.reservas.controller;

import com.reservas.dto.request.PaymentRequest;
import com.reservas.dto.response.PaymentResponse;
import com.reservas.entity.Complex;
import com.reservas.entity.Reservation;
import com.reservas.repository.ReservationRepository;
import com.reservas.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final ReservationRepository reservationRepository;

    /**
     * Crear pago para una reserva
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Webhook de Mercado Pago
     *
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId) {

        log.info("═══════════════════════════════════════════════════════════");
        log.info("📥 WEBHOOK RECIBIDO");
        log.info("   Query Params - topic: {}, id: {}, type: {}, data.id: {}", topic, id, type, dataId);
        log.info("   Body: {}", body);
        log.info("═══════════════════════════════════════════════════════════");

        try {
            // ═══════════════════════════════════════════════════════════
            // CASO 1: Merchant Order (topic=merchant_order)
            // ═══════════════════════════════════════════════════════════
            if ("merchant_order".equals(topic)) {
                String resource = body != null ? (String) body.get("resource") : null;
                if (resource == null && id != null) {
                    resource = "https://api.mercadolibre.com/merchant_orders/" + id;
                }
                if (resource != null) {
                    log.info("📦 Procesando Merchant Order: {}", resource);
                    paymentService.processMerchantOrderWebhook(resource);
                }
                return ResponseEntity.ok("OK");
            }

            // ═══════════════════════════════════════════════════════════
            // CASO 2: Payment por Query Param (type=payment, data.id=xxx)
            // ═══════════════════════════════════════════════════════════
            if ("payment".equals(type) && dataId != null) {
                log.info("💳 Procesando Payment (query param): {}", dataId);
                paymentService.processPaymentWebhook(dataId);
                return ResponseEntity.ok("OK");
            }

            // ═══════════════════════════════════════════════════════════
            // CASO 3: Payment en Body (type=payment en el JSON)
            // ═══════════════════════════════════════════════════════════
            if (body != null) {
                String bodyType = (String) body.get("type");
                String bodyTopic = (String) body.get("topic");

                // Merchant Order en body
                if ("merchant_order".equals(bodyTopic)) {
                    String resource = (String) body.get("resource");
                    if (resource != null) {
                        log.info("📦 Procesando Merchant Order (body): {}", resource);
                        paymentService.processMerchantOrderWebhook(resource);
                    }
                    return ResponseEntity.ok("OK");
                }

                // Payment en body
                if ("payment".equals(bodyType)) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data != null && data.get("id") != null) {
                        String paymentId = String.valueOf(data.get("id"));
                        log.info("💳 Procesando Payment (body): {}", paymentId);
                        paymentService.processPaymentWebhook(paymentId);
                    }
                    return ResponseEntity.ok("OK");
                }
            }

            log.info("ℹ️ Webhook no manejado - ignorando");
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("❌ Error en webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("ERROR");
        }
    }*/

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId) {

        log.info("═══════════════════════════════════════════════════════════");
        log.info("📥 WEBHOOK RECIBIDO");
        log.info("   Query Params - topic: {}, id: {}, type: {}, data.id: {}", topic, id, type, dataId);
        log.info("   Body: {}", body);
        log.info("═══════════════════════════════════════════════════════════");

        try {
            // ⚠️ IGNORAR merchant_order - solo procesar payment
            if ("merchant_order".equals(topic)) {
                log.info("⏭️ Merchant Order webhook ignorado (procesamos payment directamente)");
                return ResponseEntity.ok("OK");
            }

            // ✅ SOLO PROCESAR PAYMENT
            if ("payment".equals(type) && dataId != null) {
                log.info("💳 Procesando Payment: {}", dataId);
                paymentService.processPaymentWebhook(dataId);
                return ResponseEntity.ok("OK");
            }

            if (body != null && "payment".equals(body.get("type"))) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null && data.get("id") != null) {
                    String paymentId = String.valueOf(data.get("id"));
                    log.info("💳 Procesando Payment (body): {}", paymentId);
                    paymentService.processPaymentWebhook(paymentId);
                }
                return ResponseEntity.ok("OK");
            }

            log.info("ℹ️ Webhook no manejado");
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("❌ Error en webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("ERROR");
        }
    }

    /**
     * Obtener estado de pago por ID de reserva
     */
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<PaymentResponse> getPaymentByReservationId(
            @PathVariable Long reservationId) {
        PaymentResponse response = paymentService.getPaymentByReservationId(reservationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-preference/{reservationId}")
    public ResponseEntity<Map<String, Object>> testPreference(
            @PathVariable Long reservationId) {

        log.info("🧪 TEST: Creando preferencia de prueba para reserva {}", reservationId);

        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

            Complex complex = reservation.getCourt().getComplex();

            Map<String, Object> debug = new HashMap<>();
            debug.put("reservationId", reservationId);
            debug.put("complexId", complex.getId());
            debug.put("complexName", complex.getName());
            debug.put("price", reservation.getPrice());
            debug.put("userEmail", reservation.getUser().getEmail());
            debug.put("mpEnabled", complex.getMpEnabled());
            debug.put("hasComplexToken", complex.getMpAccessToken() != null);
            debug.put("collectorId", complex.getMpCollectorId());

            // Verificar qué token se usaría
            boolean useMarketplace = complex.getMpAccessToken() != null
                    && !complex.getMpAccessToken().isEmpty()
                    && Boolean.TRUE.equals(complex.getMpEnabled());

            debug.put("useMarketplace", useMarketplace);
            debug.put("tokenType", useMarketplace ? "COMPLEJO" : "PLATAFORMA");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            log.error("❌ Error en test: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            ));
        }
    }
}
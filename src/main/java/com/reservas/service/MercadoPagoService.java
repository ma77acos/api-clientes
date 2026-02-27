// src/main/java/com/reservas/service/MercadoPagoService.java
package com.reservas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.util.ApiMiddleware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.reservas.entity.Complex;
import com.reservas.entity.Payment;
import com.reservas.entity.Reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MercadoPagoService {

    @Value("${mercadopago.sponsor-id}")
    private Long sponsorId;

    @Value("${mercadopago.access-token}")
    private String platformAccessToken;

    @Value("${mercadopago.notification-url:}")
    private String notificationUrl;

    @Value("${mercadopago.platform-fee-percentage:5.0}")
    private BigDecimal platformFeePercentage;

    @Value("${mercadopago.platform-fee-percentage-new}")
    private float platformFeePercentageNew;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${mercadopago.buyer-pays-fee:true}")
    private boolean buyerPaysFee;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Crea una preferencia de pago en Mercado Pago
     * - Si el complejo tiene MP vinculado: MARKETPLACE (pago al vendedor, fee a plataforma)
     * - Si no: ESTÁNDAR (todo va a la cuenta de plataforma)
     */
    public Map<String, Object> createPaymentPreference(Reservation reservation, Payment payment) {
        log.info("🚀 Creando preferencia para Reservation ID: {}", reservation.getId());

        try {
            Complex complex = reservation.getCourt().getComplex();

            // ✅ VALIDACIÓN MEJORADA
            boolean useMarketplace = complex.getMpAccessToken() != null
                    && !complex.getMpAccessToken().trim().isEmpty()
                    && complex.getMpAccessToken().startsWith("APP_USR")
                    && Boolean.TRUE.equals(complex.getMpEnabled());

            String accessToken;
            BigDecimal feeAmount = BigDecimal.ZERO;  // ← Inicializar en ZERO, no null

            if (useMarketplace) {
                // MARKETPLACE: Usar token del vendedor
                accessToken = complex.getMpAccessToken();
                feeAmount = reservation.getPrice()
                        .multiply(platformFeePercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                log.info("🏢 MARKETPLACE - Vendedor: {} | Fee: {} ARS",
                        complex.getName(), feeAmount);
            } else {
                // ESTÁNDAR: Usar token de plataforma
                accessToken = platformAccessToken;
                log.info("🏪 ESTÁNDAR - Pago va a cuenta de plataforma (sin fee)");
            }

            // ✅ LOG ADICIONAL PARA DEBUG
            log.info("📋 Datos de la preferencia:");
            log.info("   - Complejo: {} (ID: {})", complex.getName(), complex.getId());
            log.info("   - Precio base: {} ARS", reservation.getPrice());
            log.info("   - Fee: {} ARS", feeAmount);
            log.info("   - Email pagador: {}", reservation.getUser().getEmail());
            log.info("   - Modo: {}", useMarketplace ? "MARKETPLACE" : "ESTÁNDAR");
            log.info("   - Token (primeros 30 chars): {}...",
                    accessToken.substring(0, Math.min(30, accessToken.length())));

            // Construir preferencia
            Map<String, Object> body = buildPreferenceBody(
                    reservation,
                    payment,
                    feeAmount,
                    useMarketplace
            );

            // Llamar a la API de MP
            String url = "https://api.mercadopago.com/checkout/preferences";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);


            // AGREGAR ESTO:
            log.info("═══════════════════════════════════════════");
            log.info("📤 REQUEST A MERCADOPAGO:");
            log.info("URL: {}", url);
            log.info("Headers: Authorization: Bearer {}...", accessToken.substring(0, 30));
            log.info("Body JSON:");
            try {
                ObjectMapper mapper = new ObjectMapper();
                String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
                log.info("{}", prettyJson);
            } catch (Exception e) {
                log.info("{}", body);
            }
            log.info("═══════════════════════════════════════════");

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> result = response.getBody();

            log.info("✅ Preferencia creada - ID: {} | Init Point: {}",
                    result.get("id"), result.get("init_point"));

            return result;

        } catch (Exception e) {
            log.error("❌ Error creando preferencia: {}", e.getMessage(), e);
            throw new RuntimeException("Error de MercadoPago: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPreferenceBody(
            Reservation reservation,
            Payment payment,
            BigDecimal feeAmount,
            boolean useMarketplace) {

        Complex complex = reservation.getCourt().getComplex();
        BigDecimal basePrice = reservation.getPrice();

        // ✅ Calcular precio final según quién paga el fee
        BigDecimal finalPrice;

        if (useMarketplace && buyerPaysFee && feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Opción: Jugador paga precio + fee
            finalPrice = basePrice.add(feeAmount);
            log.info("💰 Jugador paga fee adicional: {} + {} = {}", basePrice, feeAmount, finalPrice);
        } else {
            // Opción: Complejo absorbe el fee (o modo estándar sin fee)
            finalPrice = basePrice;
            log.info("💰 Precio final: {} (fee absorbido por complejo o sin fee)", finalPrice);
        }

        // Item
        Map<String, Object> item = new HashMap<>();
        item.put("id", String.valueOf(reservation.getId()));
        item.put("title", String.format("Reserva - %s - %s",
                complex.getName(),
                reservation.getCourt().getName()));
        item.put("description", String.format("Turnos Control - Fecha: %s - Hora: %s",
                reservation.getDate(),
                reservation.getTime()));
        item.put("quantity", 1);
        //item.put("category_id", "marketplace");
        item.put("unit_price", finalPrice);

        // Payer
        Map<String, Object> payer = new HashMap<>();
        payer.put("name", reservation.getUser().getDisplayName() != null
                ? reservation.getUser().getDisplayName()
                : "Usuario");
        payer.put("email", reservation.getUser().getEmail());

        // Back URLs
        Map<String, String> backUrls = new HashMap<>();
        backUrls.put("success", frontendUrl + "/payment/success?reservationId=" + reservation.getId());
        backUrls.put("failure", frontendUrl + "/payment/failure?reservationId=" + reservation.getId());
        backUrls.put("pending", frontendUrl + "/payment/pending?reservationId=" + reservation.getId());

        // Body principal
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        //body.put("payer", payer);
        //body.put("operation_type", "regular_payment");
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");
        body.put("external_reference", String.valueOf(payment.getId()));
        body.put("statement_descriptor", "Reserva Turnos Control");
        //body.put("expires", false);
        /*
        * String expirationDate = java.time.OffsetDateTime.now()
            .plusMinutes(5)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        body.put("expiration_date_to", expirationDate);
        * */



        // ⭐ MARKETPLACE FEE + SPONSOR (como objeto)
        if (useMarketplace && feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            body.put("marketplace", "TurnosOnline");
            body.put("marketplace_fee", feeAmount.setScale(2, RoundingMode.HALF_UP));  // ← CORREGIDO

            // Sponsor como OBJETO
            //Map<String, Object> sponsor = new HashMap<>();
            //sponsor.put("id", sponsorId);
            //body.put("sponsor", sponsor);

            log.info("📦 Marketplace fee: {} | Sponsor ID: {}", feeAmount, sponsorId);
        }

        // Webhook URL
        if (notificationUrl != null && !notificationUrl.isEmpty()
                && !notificationUrl.contains("localhost")) {
            body.put("notification_url", notificationUrl);
            log.info("🔔 Notification URL: {}", notificationUrl);
        }

        return body;
    }

    /**
     * Consultar información de un pago con un token específico
     */
    public Map<String, Object> getPaymentInfoWithToken(String mpPaymentId, String accessToken) {
        log.info("🔍 Consultando pago en MP: {} con token específico", mpPaymentId);

        String url = "https://api.mercadopago.com/v1/payments/" + mpPaymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
        );

        return response.getBody();
    }

    /**
     * Consultar información de un pago (intenta con token de plataforma)
     */
    public Map<String, Object> getPaymentInfo(String mpPaymentId) {
        return getPaymentInfoWithToken(mpPaymentId, platformAccessToken);
    }

    /**
     * Consultar Merchant Order desde URL completa
     */
    public Map<String, Object> getMerchantOrder(String resourceUrl) {
        log.info("🔍 Consultando Merchant Order: {}", resourceUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(platformAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                resourceUrl,  // La URL completa que viene en el webhook
                HttpMethod.GET,
                request,
                Map.class
        );

        return response.getBody();
    }
}
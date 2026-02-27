package com.reservas.service;

import com.reservas.entity.Complex;
import com.reservas.entity.User;
import com.reservas.exception.BadRequestException;
import com.reservas.repository.ComplexRepository;
import com.reservas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoOAuthService {

    @Value("${mercadopago.app-id}")
    private String appId;

    @Value("${mercadopago.redirect-uri}")
    private String redirectUri;

    @Value("${mercadopago.client-secret}")
    private String clientSecret;

    private final ComplexRepository complexRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private BidiMap<String, String> authMap = new DualHashBidiMap<>();

    /**
     * 🔗 Generar URL de autorización de Mercado Pago
     */
    public String getAuthorizationUrl() {
        User currentUser = getCurrentUser();
        Complex complex = getComplexByUser(currentUser);

        String state = String.valueOf(complex.getId());

        // ✅ Scopes completos para marketplace
        String scopes = "offline_access,payments,read,write,urn:mp:online:preference/read-write,urn:mp:instore:instore-order/read-write";

        // ✅ Forzar re-autorización con prompt=consent
        String authUrl = String.format(
                "https://auth.mercadopago.com.ar/authorization?client_id=%s&response_type=code&platform_id=mp&state=%s&redirect_uri=%s",
                appId,
                state,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        );

        log.info("🔗 URL de autorización generada para Complex ID: {}", complex.getId());

        return authUrl;
    }

    /**
     * 🔄 Procesar callback de OAuth (intercambiar code por access_token)
     */
    @Transactional
    public void processOAuthCallback(String code, String state) {
        log.info("📥 Procesando callback OAuth - State: {}, Code: {}...", state, code.substring(0, 10));

        try {
            Long complexId = Long.parseLong(state);
            Complex complex = complexRepository.findById(complexId)
                    .orElseThrow(() -> new BadRequestException("Complex no encontrado"));

            // Intercambiar code por access_token
            Map<String, Object> tokenResponse = exchangeCodeForToken(code);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            String publicKey = (String) tokenResponse.get("public_key");
            String userId = String.valueOf(tokenResponse.get("user_id"));
            String scopes = (String) tokenResponse.get("scope");  // ← Obtener scopes

            // Guardar en el Complex
            complex.setMpAccessToken(accessToken);
            complex.setMpRefreshToken(refreshToken);
            complex.setMpPublicKey(publicKey);
            complex.setMpCollectorId(userId);
            complex.setMpScopes(scopes);  // ← Guardar scopes
            complex.setMpEnabled(true);

            complexRepository.save(complex);

            log.info("✅ Cuenta de MP vinculada exitosamente:");
            log.info("   - Complex: {}", complex.getName());
            log.info("   - Collector ID: {}", userId);
            log.info("   - Scopes: {}", scopes);

        } catch (NumberFormatException e) {
            log.error("❌ State inválido: {}", state);
            throw new BadRequestException("State inválido en callback de OAuth");
        } catch (Exception e) {
            log.error("❌ Error procesando OAuth callback: {}", e.getMessage(), e);
            throw new RuntimeException("Error vinculando cuenta de Mercado Pago", e);
        }
    }

    /**
     * 🔄 Intercambiar authorization code por access token
     */
    private Map<String, Object> exchangeCodeForToken(String code) {
        String url = "https://api.mercadopago.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("client_id", appId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("grant_type", "authorization_code");
        body.put("redirect_uri", redirectUri);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("📤 Intercambiando code por access_token en MP...");

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Error obteniendo access token de Mercado Pago");
        }

        log.info("✅ Access token obtenido exitosamente");
        return response.getBody();
    }

    /**
     * 📊 Obtener estado de vinculación
     */
    public Map<String, Object> getConnectionStatus() {
        User currentUser = getCurrentUser();
        Complex complex = getComplexByUser(currentUser);

        boolean isConnected = complex.getMpEnabled() != null
                && complex.getMpEnabled()
                && complex.getMpAccessToken() != null;

        Map<String, Object> status = new HashMap<>();
        status.put("connected", isConnected);
        status.put("complexId", complex.getId());
        status.put("complexName", complex.getName());

        if (isConnected) {
            status.put("collectorId", complex.getMpCollectorId());
            status.put("publicKey", complex.getMpPublicKey());
        }

        return status;
    }

    /**
     * 🔌 Desvincular cuenta de Mercado Pago
     */
    @Transactional
    public void disconnect() {
        User currentUser = getCurrentUser();
        Complex complex = getComplexByUser(currentUser);

        complex.setMpEnabled(false);
        complex.setMpAccessToken(null);
        complex.setMpRefreshToken(null);
        complex.setMpPublicKey(null);
        complex.setMpCollectorId(null);
        complex.setMpScopes(null);

        complexRepository.save(complex);

        log.info("🔌 Cuenta de MP desvinculada - Complex: {}", complex.getName());
    }

    // ========================================
    // Helpers
    // ========================================

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
    }

    private Complex getComplexByUser(User user) {
        if (user.getComplex() == null) {
            throw new BadRequestException("El usuario no tiene un complejo asignado");
        }
        return user.getComplex();
    }
}
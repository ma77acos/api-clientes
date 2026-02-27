package com.reservas.controller;

import com.reservas.service.MercadoPagoOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/mercadopago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoCallbackController {

    private final MercadoPagoOAuthService mpOAuthService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * 📥 Callback público de OAuth (Mercado Pago redirige aquí)
     */
    @GetMapping("/callback")
    public RedirectView oauthCallback(
            @RequestParam String code,
            @RequestParam String state) {

        log.info("📥 Callback de Mercado Pago recibido - State: {}", state);
        log.info("📥 ========== CALLBACK DE MERCADO PAGO ==========");
        log.info("📥 Code: {}...", code.substring(0, Math.min(20, code.length())));
        log.info("📥 State (Complex ID): {}", state);

        try {
            // Procesar OAuth
            mpOAuthService.processOAuthCallback(code, state);

            // Redirigir al frontend con éxito
            return new RedirectView(frontendUrl+"/admin/configuration?mp=success");

        } catch (Exception e) {
            log.error("❌ Error en callback OAuth: {}", e.getMessage(), e);

            // Redirigir al frontend con error
            return new RedirectView(frontendUrl+"/admin/configuration?mp=error");
        }
    }
}

// src/main/java/com/reservas/config/MercadoPagoConfiguration.java
package com.reservas.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class MercadoPagoConfiguration {

    //@Value("${mercadopago.access-token}")
    //private String accessToken;

    //@Value("${mercadopago.public-key}")
    //private String publicKey;

    //@PostConstruct
    //public void init() {
    //    MercadoPagoConfig.setAccessToken(accessToken);
    //    System.out.println("✅ Mercado Pago configurado correctamente");
    //}

    // Getters si los necesitas
    //public String getPublicKey() {
    //    return publicKey;
    //}
}
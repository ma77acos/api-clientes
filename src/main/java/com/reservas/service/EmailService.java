// src/main/java/com/reservas/service/EmailService.java
package com.reservas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Restablecer Contraseña - Sistema de Reservas");
            message.setText(
                    "Hola " + userName + ",\n\n" +
                            "Has solicitado restablecer tu contraseña.\n\n" +
                            "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                            resetUrl + "\n\n" +
                            "Este enlace expirará en 30 minutos.\n\n" +
                            "Si no solicitaste este cambio, ignora este mensaje.\n\n" +
                            "Saludos,\n" +
                            "Equipo de Sistema de Reservas"
            );

            mailSender.send(message);
            log.info("Email de reset enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error enviando email de reset a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email de reset", e);
        }
    }

    public void sendPasswordChangedConfirmation(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Contraseña Actualizada - Sistema de Reservas");
            message.setText(
                    "Hola " + userName + ",\n\n" +
                            "Tu contraseña ha sido actualizada exitosamente.\n\n" +
                            "Si no realizaste este cambio, contacta inmediatamente al administrador.\n\n" +
                            "Saludos,\n" +
                            "Equipo de Sistema de Reservas"
            );

            mailSender.send(message);
            log.info("Email de confirmación enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error enviando email de confirmación a {}: {}", toEmail, e.getMessage());
            // No lanzar excepción aquí, solo loguear
        }
    }


    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("¡Bienvenido a Sistema de Reservas!");
            message.setText(
                    "Hola " + userName + ",\n\n" +
                            "¡Gracias por registrarte en nuestro sistema de reservas!\n\n" +
                            "Ya podés empezar a reservar canchas de tu deporte favorito.\n\n" +
                            "Si tenés alguna duda, no dudes en contactarnos.\n\n" +
                            "¡Que disfrutes!\n\n" +
                            "Equipo de Sistema de Reservas"
            );

            mailSender.send(message);
            log.info("Email de bienvenida enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error enviando email de bienvenida a {}: {}", toEmail, e.getMessage());
        }
    }
}
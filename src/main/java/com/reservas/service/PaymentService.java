// src/main/java/com/reservas/service/PaymentService.java
package com.reservas.service;

import com.reservas.dto.request.PaymentRequest;
import com.reservas.dto.response.PaymentResponse;
import com.reservas.entity.Complex;
import com.reservas.entity.Payment;
import com.reservas.entity.Reservation;
import com.reservas.enums.PaymentStatus;
import com.reservas.enums.ReservationStatus;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.PaymentRepository;
import com.reservas.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MercadoPagoService mercadoPagoService;

    /**
     * Crear pago y preferencia de Mercado Pago
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva", "id", request.getReservationId()));

        // Verificar si ya existe un pago
        if (paymentRepository.findByReservationId(request.getReservationId()).isPresent()) {
            throw new BadRequestException("Ya existe un pago para esta reserva");
        }

        // Verificar si la reserva expiró
        if (reservation.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(reservation.getExpiresAt())) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
            throw new BadRequestException("La reserva ha expirado. Por favor, crea una nueva.");
        }

        // Crear payment en BD
        Payment payment = Payment.builder()
                .reservation(reservation)
                .amount(request.getAmount())
                .method(request.getMethod())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        // Crear preference en Mercado Pago
        Map<String, Object> preference = mercadoPagoService.createPaymentPreference(reservation, payment);

        String preferenceId = (String) preference.get("id");
        String initPoint = (String) preference.get("init_point");
        String sandboxInitPoint = (String) preference.get("sandbox_init_point");

        // Actualizar payment con datos de MP
        payment.setExternalPaymentId(preferenceId);
        payment.setCheckoutUrl(initPoint);
        paymentRepository.save(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getExternalPaymentId())
                .status(payment.getStatus())
                .checkoutUrl(payment.getCheckoutUrl())
                .sandboxCheckoutUrl(sandboxInitPoint)
                .build();
    }

    /**
     * Procesar webhook de Merchant Order
     */
    @Transactional
    public void processMerchantOrderWebhook(String resourceUrl) {
        log.info("📦 Procesando Merchant Order: {}", resourceUrl);

        try {
            Map<String, Object> merchantOrder = mercadoPagoService.getMerchantOrder(resourceUrl);

            String status = (String) merchantOrder.get("status");
            String externalReference = (String) merchantOrder.get("external_reference");
            Number totalAmount = (Number) merchantOrder.get("total_amount");

            log.info("📦 MO Status: {} | External Ref: {}", status, externalReference);

            if (!"closed".equals(status)) {
                log.info("⏳ Orden aún no está cerrada (status: {}), esperando...", status);
                return;
            }

            // Calcular total pagado de pagos aprobados
            List<Map<String, Object>> payments = (List<Map<String, Object>>) merchantOrder.get("payments");
            double totalPaid = 0.0;
            String approvedPaymentId = null;

            if (payments != null) {
                for (Map<String, Object> payment : payments) {
                    String paymentStatus = (String) payment.get("status");
                    if ("approved".equals(paymentStatus)) {
                        Number transactionAmount = (Number) payment.get("transaction_amount");
                        totalPaid += transactionAmount.doubleValue();
                        approvedPaymentId = String.valueOf(payment.get("id"));
                    }
                }
            }

            log.info("💰 Total pagado: {} / Total orden: {}", totalPaid, totalAmount);

            if (totalPaid >= totalAmount.doubleValue()) {
                log.info("✅ Pago COMPLETO - Confirmando orden");
                confirmPayment(externalReference, approvedPaymentId);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando Merchant Order: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesar webhook de Payment directo
     */
    @Transactional
    public void processPaymentWebhook(String paymentId) {
        log.info("💳 Procesando Payment ID: {}", paymentId);

        try {
            // Primero intentar con token de plataforma para obtener external_reference
            Map<String, Object> mpPayment = mercadoPagoService.getPaymentInfo(paymentId);

            String status = (String) mpPayment.get("status");
            String externalReference = (String) mpPayment.get("external_reference");

            log.info("💳 Payment Status: {} | External Ref: {}", status, externalReference);

            if (externalReference == null || externalReference.isEmpty()) {
                log.warn("⚠️ External reference vacío, ignorando webhook");
                return;
            }

            // Obtener el Payment de la BD
            Long paymentDbId = Long.parseLong(externalReference);
            Payment payment = paymentRepository.findById(paymentDbId).orElse(null);

            if (payment == null) {
                log.warn("⚠️ Payment no encontrado: {}", paymentDbId);
                return;
            }

            // Si fue marketplace, re-consultar con el token del complejo
            Complex complex = payment.getReservation().getCourt().getComplex();
            boolean wasMarketplace = complex.getMpAccessToken() != null
                    && Boolean.TRUE.equals(complex.getMpEnabled());

            if (wasMarketplace) {
                log.info("🔄 Re-consultando pago con token del complejo...");
                mpPayment = mercadoPagoService.getPaymentInfoWithToken(paymentId, complex.getMpAccessToken());
                status = (String) mpPayment.get("status");
                log.info("💳 Payment Status (complejo): {}", status);
            }

            // Procesar según el estado
            if ("approved".equals(status)) {
                confirmPayment(externalReference, paymentId);
            } else if ("rejected".equals(status) || "cancelled".equals(status)) {
                rejectPayment(externalReference);
            } else {
                log.info("⏳ Payment en estado {}, esperando...", status);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando Payment: {}", e.getMessage(), e);
        }
    }

    /**
     * Confirmar pago y reserva
     */
    @Transactional
    public void confirmPayment(String externalReference, String mpPaymentId) {
        if (externalReference == null || externalReference.isEmpty()) {
            log.warn("⚠️ External reference vacío");
            return;
        }

        Long paymentId;
        try {
            paymentId = Long.parseLong(externalReference);
        } catch (NumberFormatException e) {
            log.error("❌ External reference inválido: {}", externalReference);
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);

        if (payment == null) {
            log.warn("⚠️ Payment no encontrado: {}", paymentId);
            return;
        }

        // Evitar procesar duplicados
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            log.info("ℹ️ Payment {} ya estaba aprobado, ignorando", paymentId);
            return;
        }

        // Actualizar Payment
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setMpPaymentId(mpPaymentId);
        paymentRepository.save(payment);

        // Actualizar Reservation
        Reservation reservation = payment.getReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        log.info("✅ Payment {} APROBADO | Reserva {} CONFIRMADA",
                paymentId, reservation.getId());
    }

    /**
     * Rechazar pago y cancelar reserva
     */
    @Transactional
    public void rejectPayment(String externalReference) {
        if (externalReference == null) return;

        try {
            Long paymentId = Long.parseLong(externalReference);
            Payment payment = paymentRepository.findById(paymentId).orElse(null);

            if (payment == null || payment.getStatus() == PaymentStatus.REJECTED) {
                return;
            }

            payment.setStatus(PaymentStatus.REJECTED);
            paymentRepository.save(payment);

            Reservation reservation = payment.getReservation();
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);

            log.info("❌ Payment {} RECHAZADO | Reserva {} CANCELADA",
                    paymentId, reservation.getId());

        } catch (Exception e) {
            log.error("Error rechazando pago: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReservationId(Long reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pago", "reservationId", reservationId));

        return PaymentResponse.builder()
                .paymentId(payment.getExternalPaymentId())
                .status(payment.getStatus())
                .checkoutUrl(payment.getCheckoutUrl())
                .build();
    }
}
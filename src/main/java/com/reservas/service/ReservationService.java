// src/main/java/com/reservas/service/ReservationService.java
package com.reservas.service;

import com.reservas.dto.request.AddExtraProductRequest;
import com.reservas.dto.request.AddPlayerPaymentRequest;
import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.request.ReservationRequest;
import com.reservas.dto.response.*;
import com.reservas.entity.*;
import com.reservas.enums.PaymentMethod;
import com.reservas.enums.ReservationStatus;
import com.reservas.enums.Role;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final PlayerPaymentRepository playerPaymentRepository;
    private final ExtraProductRepository extraProductRepository;
    private final CashRegisterService cashRegisterService;

    // ==================== MÉTODOS EXISTENTES ====================

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        if (reservationRepository.existsActiveReservation(
                request.getCourtId(), request.getDate(), request.getTime())) {
            throw new BadRequestException("El horario seleccionado ya está reservado");
        }

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUserId()));

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        Reservation reservation = Reservation.builder()
                .court(court)
                .user(user)
                .date(request.getDate())
                .time(request.getTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        reservation = reservationRepository.save(reservation);

        return mapToReservationResponse(reservation);
    }

    @Transactional
    public List<ReservationResponse> createRecurringReservation(RecurringReservationRequest request) {
        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUserId()));

        List<Reservation> reservations = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        int maxOccurrences = request.getOccurrences() != null ? request.getOccurrences() : 52;

        for (int i = 0; i < maxOccurrences; i++) {
            if (!reservationRepository.existsByCourtIdAndDateAndTime(
                    request.getCourtId(), currentDate, request.getTime())) {

                Reservation reservation = Reservation.builder()
                        .court(court)
                        .user(user)
                        .date(currentDate)
                        .time(request.getTime())
                        .price(request.getPrice())
                        .status(ReservationStatus.CONFIRMED)
                        .build();

                reservations.add(reservation);
            }

            switch (request.getRecurrenceType()) {
                case DAILY:
                    currentDate = currentDate.plusDays(1);
                    break;
                case WEEKLY:
                    currentDate = currentDate.plusWeeks(1);
                    break;
                case MONTHLY:
                    currentDate = currentDate.plusMonths(1);
                    break;
            }
        }

        List<Reservation> savedReservations = reservationRepository.saveAll(reservations);

        return savedReservations.stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MyReservationResponse> getMyReservations() {
        User currentUser = getCurrentUser();

        List<Reservation> reservations = reservationRepository
                .findByUserIdOrderByDateDescTimeDesc(currentUser.getId());

        return reservations.stream()
                .map(this::mapToMyReservationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
        return mapToReservationResponse(reservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));

        User currentUser = getCurrentUser();

        LocalDateTime reservationDateTime = LocalDateTime.of(reservation.getDate(), reservation.getTime());
        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("No se puede cancelar una reserva pasada");
        }

        boolean canCancel = false;

        switch (currentUser.getRole()) {
            case PLAYER:
                if (reservation.getUser() != null &&
                        reservation.getUser().getId().equals(currentUser.getId())) {
                    canCancel = true;
                }
                break;

            case BUSINESS:
                if (currentUser.getComplex() != null) {
                    Long businessComplexId = currentUser.getComplex().getId();
                    Long reservationComplexId = reservation.getCourt().getComplex().getId();

                    if (businessComplexId.equals(reservationComplexId)) {
                        canCancel = true;
                    }
                }
                break;

            case ADMIN:
                canCancel = true;
                break;
        }

        if (!canCancel) {
            throw new BadRequestException("No tienes permiso para cancelar esta reserva");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    // ==================== DETALLE ====================

    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationDetail(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));

        User currentUser = getCurrentUser();

        boolean isReservationOwner = reservation.getUser() != null &&
                reservation.getUser().getId().equals(currentUser.getId());

        boolean isComplexOwner = currentUser.getRole() == Role.BUSINESS &&
                currentUser.getComplex() != null &&
                currentUser.getComplex().getId().equals(reservation.getCourt().getComplex().getId());

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isReservationOwner && !isComplexOwner && !isAdmin) {
            throw new UnauthorizedException("No tienes acceso a esta reserva");
        }

        return mapToReservationDetailResponse(reservation);
    }

    // ==================== PAGOS JUGADORES ====================

    @Transactional
    public PlayerPaymentResponse addPlayerPayment(Long reservationId, AddPlayerPaymentRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", reservationId));

        User currentUser = getCurrentUser();
        verifyComplexOwnership(currentUser, reservation);

        int currentPaymentsCount = playerPaymentRepository.countByReservationId(reservationId);
        if (currentPaymentsCount >= 4) {
            throw new BadRequestException("Ya hay 4 pagos registrados para este turno");
        }

        PaymentMethod method = PaymentMethod.valueOf(request.getMethod());

        PlayerPayment payment = PlayerPayment.builder()
                .reservation(reservation)
                .playerName(request.getPlayerName())
                .amount(request.getAmount())
                .method(method)
                .paidAt(LocalDateTime.now())
                .createdBy(currentUser)
                .build();

        payment = playerPaymentRepository.save(payment);

        // Refrescar la lista de pagos en la reserva
        reservation.getPlayerPayments().add(payment);

        // Calcular si hay excedente que cubre productos
        BigDecimal totalPlayerPayments = reservation.getPlayerPayments().stream()
                .map(PlayerPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal courtPrice = reservation.getPrice();

        if (totalPlayerPayments.compareTo(courtPrice) > 0) {
            BigDecimal excessAmount = totalPlayerPayments.subtract(courtPrice);

            List<ExtraProduct> unpaidProducts = reservation.getExtraProducts().stream()
                    .filter(p -> !p.getPaid())
                    .sorted((a, b) -> a.getAddedAt().compareTo(b.getAddedAt()))
                    .collect(Collectors.toList());

            BigDecimal coveredAmount = BigDecimal.ZERO;

            for (ExtraProduct product : unpaidProducts) {
                BigDecimal newCoveredAmount = coveredAmount.add(product.getTotalPrice());

                if (newCoveredAmount.compareTo(excessAmount) <= 0) {
                    product.setPaid(true);
                    product.setPaymentMethod(method);
                    product.setPaidAt(LocalDateTime.now());
                    extraProductRepository.save(product);
                    coveredAmount = newCoveredAmount;

                    // ✅ FIX: NO registrar en caja - ya está incluido en el pago del jugador
                    log.info("✅ Producto {} marcado como pagado automáticamente (excedente)", product.getName());
                } else {
                    break;
                }
            }
        }

        updateReservationStatusIfFullyPaid(reservation);

        // Registrar en caja si es efectivo
        if (method == PaymentMethod.CASH) {
            String info = buildReservationInfo(reservation);
            cashRegisterService.registerAutomaticPayment(payment, info);
        }

        log.info("💰 Pago registrado: ${} de {} para reserva #{}",
                request.getAmount(), request.getPlayerName(), reservationId);

        return mapToPlayerPaymentResponse(payment);
    }

    @Transactional
    public void removePlayerPayment(Long reservationId, Long paymentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", reservationId));

        User currentUser = getCurrentUser();
        verifyComplexOwnership(currentUser, reservation);

        PlayerPayment payment = playerPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", paymentId));

        if (!payment.getReservation().getId().equals(reservationId)) {
            throw new BadRequestException("El pago no pertenece a esta reserva");
        }

        // Eliminar de caja si era efectivo
        if (payment.getMethod() == PaymentMethod.CASH) {
            cashRegisterService.removeAutomaticPayment(paymentId);
        }

        playerPaymentRepository.delete(payment);

        reservation.getPlayerPayments().removeIf(p -> p.getId().equals(paymentId));

        if (reservation.getStatus() == ReservationStatus.PAYED && !reservation.isFullyPaid()) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
            log.info("⚠️ Reserva #{} revertida a CONFIRMED (pago eliminado)", reservationId);
        }
    }

    // ==================== PRODUCTOS EXTRAS ====================

    @Transactional
    public ExtraProductResponse addExtraProduct(Long reservationId, AddExtraProductRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", reservationId));

        User currentUser = getCurrentUser();
        verifyComplexOwnership(currentUser, reservation);

        BigDecimal totalPrice = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        ExtraProduct product = ExtraProduct.builder()
                .reservation(reservation)
                .name(request.getName())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .paid(false)
                .addedAt(LocalDateTime.now())
                .build();

        product = extraProductRepository.save(product);

        return mapToExtraProductResponse(product);
    }

    @Transactional
    public void removeExtraProduct(Long reservationId, Long productId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", reservationId));

        User currentUser = getCurrentUser();
        verifyComplexOwnership(currentUser, reservation);

        ExtraProduct product = extraProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (!product.getReservation().getId().equals(reservationId)) {
            throw new BadRequestException("El producto no pertenece a esta reserva");
        }

        // Eliminar de caja si estaba pagado en efectivo
        if (product.getPaid() && product.getPaymentMethod() == PaymentMethod.CASH) {
            cashRegisterService.removeAutomaticProductSale(productId);
        }

        boolean wasPaid = product.getPaid();

        extraProductRepository.delete(product);

        reservation.getExtraProducts().removeIf(p -> p.getId().equals(productId));

        if (!wasPaid) {
            updateReservationStatusIfFullyPaid(reservation);
        }
    }

    @Transactional
    public ExtraProductResponse markProductAsPaid(Long reservationId, Long productId, String method) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", reservationId));

        User currentUser = getCurrentUser();
        verifyComplexOwnership(currentUser, reservation);

        ExtraProduct product = extraProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (!product.getReservation().getId().equals(reservationId)) {
            throw new BadRequestException("El producto no pertenece a esta reserva");
        }

        if (product.getPaid()) {
            throw new BadRequestException("El producto ya está pagado");
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(method);

        product.setPaid(true);
        product.setPaymentMethod(paymentMethod);
        product.setPaidAt(LocalDateTime.now());

        product = extraProductRepository.save(product);

        updateReservationStatusIfFullyPaid(reservation);

        // ✅ Registrar en caja SOLO cuando se paga manualmente
        if (paymentMethod == PaymentMethod.CASH) {
            String info = buildReservationInfo(reservation);
            cashRegisterService.registerAutomaticProductSale(product, info);
        }

        return mapToExtraProductResponse(product);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private void verifyComplexOwnership(User user, Reservation reservation) {
        boolean isComplexOwner = user.getRole() == Role.BUSINESS &&
                user.getComplex() != null &&
                user.getComplex().getId().equals(reservation.getCourt().getComplex().getId());

        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isComplexOwner && !isAdmin) {
            throw new UnauthorizedException("Solo el dueño del complejo puede realizar esta acción");
        }
    }

    private void updateReservationStatusIfFullyPaid(Reservation reservation) {
        if (reservation.isFullyPaid() && reservation.getStatus() != ReservationStatus.PAYED) {
            reservation.setStatus(ReservationStatus.PAYED);
            reservationRepository.save(reservation);
            log.info("✅ Reserva #{} marcada como PAYED (totalmente pagada)", reservation.getId());
        }
    }

    private String buildReservationInfo(Reservation reservation) {
        return String.format("%s - %s - %s",
                reservation.getCourt().getName(),
                reservation.getTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                reservation.getCustomerName() != null ? reservation.getCustomerName() : "Cliente");
    }

    // ==================== MAPPERS ====================

    private ReservationResponse mapToReservationResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .status(reservation.getStatus())
                .courtId(reservation.getCourt().getId())
                .courtName(reservation.getCourt().getName())
                .complexName(reservation.getCourt().getComplex().getName())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .price(reservation.getPrice())
                .customerName(reservation.getCustomerName())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    private MyReservationResponse mapToMyReservationResponse(Reservation reservation) {
        return MyReservationResponse.builder()
                .id(reservation.getId())
                .complexName(reservation.getCourt().getComplex().getName())
                .courtName(reservation.getCourt().getName())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .status(reservation.getStatus())
                .build();
    }

    private ReservationDetailResponse mapToReservationDetailResponse(Reservation reservation) {
        List<PlayerPaymentResponse> payments = reservation.getPlayerPayments().stream()
                .map(this::mapToPlayerPaymentResponse)
                .collect(Collectors.toList());

        List<ExtraProductResponse> products = reservation.getExtraProducts().stream()
                .map(this::mapToExtraProductResponse)
                .collect(Collectors.toList());

        return ReservationDetailResponse.builder()
                .id(reservation.getId())
                .courtId(reservation.getCourt().getId())
                .courtName(reservation.getCourt().getName())
                .complexId(reservation.getCourt().getComplex().getId())
                .complexName(reservation.getCourt().getComplex().getName())
                .complexAddress(reservation.getCourt().getComplex().getAddress())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .price(reservation.getPrice())
                .status(reservation.getStatus())
                .customerName(reservation.getCustomerName())
                .createdAt(reservation.getCreatedAt())
                .isFullyPaid(reservation.isFullyPaid())
                .totalPaid(reservation.getTotalPaid())
                .totalPending(reservation.getTotalPending())
                .productsTotal(reservation.getProductsTotal())
                .grandTotal(reservation.getGrandTotal())
                .playerPayments(payments)
                .extraProducts(products)
                .build();
    }

    private PlayerPaymentResponse mapToPlayerPaymentResponse(PlayerPayment payment) {
        return PlayerPaymentResponse.builder()
                .id(payment.getId())
                .playerName(payment.getPlayerName())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private ExtraProductResponse mapToExtraProductResponse(ExtraProduct product) {
        return ExtraProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .unitPrice(product.getUnitPrice())
                .quantity(product.getQuantity())
                .totalPrice(product.getTotalPrice())
                .paid(product.getPaid())
                .paymentMethod(product.getPaymentMethod() != null ? product.getPaymentMethod().name() : null)
                .addedAt(product.getAddedAt())
                .paidAt(product.getPaidAt())
                .build();
    }
}
// src/main/java/com/reservas/service/AdminReservationService.java
package com.reservas.service;

import com.reservas.dto.request.AdminReservationRequest;
import com.reservas.dto.response.DashboardStatsResponse;
import com.reservas.dto.response.ReservationResponse;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final RecurringReservationRepository recurringRepository;
    private final RecurringExceptionRepository exceptionRepository;
    private final PlayerPaymentRepository playerPaymentRepository; // 🆕
    private final ExtraProductRepository extraProductRepository; // 🆕

    /**
     * Crear reserva como administrador del complejo
     */
    @Transactional
    public ReservationResponse createAdminReservation(AdminReservationRequest request) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.BUSINESS && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos para crear reservas administrativas");
        }

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        if (currentUser.getRole() == Role.BUSINESS) {
            if (currentUser.getComplex() == null ||
                    !currentUser.getComplex().getId().equals(court.getComplex().getId())) {
                throw new UnauthorizedException("No administras este complejo");
            }
        }

        if (reservationRepository.existsActiveReservation(
                request.getCourtId(), request.getDate(), request.getTime())) {
            throw new BadRequestException("El horario seleccionado ya está reservado");
        }

        Reservation reservation = Reservation.builder()
                .court(court)
                .user(null)
                .date(request.getDate())
                .time(request.getTime())
                .price(request.getPrice())
                .status(ReservationStatus.CONFIRMED)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .notes(request.getNotes())
                .createdByAdmin(true)
                .createdBy(currentUser)
                .expiresAt(null)
                .build();

        reservation = reservationRepository.save(reservation);

        log.info("✅ Reserva admin #{} creada por {} para cliente: {}",
                reservation.getId(),
                currentUser.getEmail(),
                request.getCustomerName());

        return mapToReservationResponse(reservation);
    }

    /**
     * Obtener reservas del complejo (incluye reservas fijas con estado de pago)
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getComplexReservations(LocalDate date) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.BUSINESS && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos para ver estas reservas");
        }

        Long complexId;
        if (currentUser.getRole() == Role.ADMIN) {
            throw new BadRequestException("Especifica el complejo");
        } else {
            if (currentUser.getComplex() == null) {
                throw new BadRequestException("No tienes un complejo asignado");
            }
            complexId = currentUser.getComplex().getId();
        }

        // 1. Reservas normales
        List<Reservation> reservations = reservationRepository
                .findByComplexIdAndDate(complexId, date);

        List<ReservationResponse> responses = new ArrayList<>(
                reservations.stream()
                        .map(this::mapToReservationResponse)
                        .toList()
        );

        // 2. Agregar reservas fijas para esta fecha
        List<ReservationResponse> recurringResponses =
                expandRecurringReservationsForDate(complexId, date);
        responses.addAll(recurringResponses);

        // 3. Ordenar por hora
        responses.sort(Comparator.comparing(ReservationResponse::getTime));

        return responses;
    }

    /**
     * 🆕 Expande reservas fijas para una fecha específica CON cálculo de isFullyPaid
     */
    private List<ReservationResponse> expandRecurringReservationsForDate(Long complexId, LocalDate date) {
        List<RecurringReservation> activeRecurrings = recurringRepository
                .findActiveByComplexId(complexId);

        List<ReservationResponse> expanded = new ArrayList<>();

        for (RecurringReservation recurring : activeRecurrings) {
            if (date.getDayOfWeek() != recurring.getDayOfWeek()) {
                continue;
            }

            if (date.isBefore(recurring.getStartDate())) {
                continue;
            }

            if (recurring.getEndDate() != null && date.isAfter(recurring.getEndDate())) {
                continue;
            }

            boolean hasException = exceptionRepository
                    .existsByRecurringReservationIdAndExceptionDate(recurring.getId(), date);

            if (!hasException) {
                expanded.add(mapRecurringToResponseWithPaymentStatus(recurring, date));
            }
        }

        return expanded;
    }

    /**
     * 🆕 Mapea reserva recurrente calculando si está completamente pagada
     */
    private ReservationResponse mapRecurringToResponseWithPaymentStatus(
            RecurringReservation recurring, LocalDate date) {

        // Calcular si está completamente pagada
        boolean isFullyPaid = calculateIsFullyPaidForRecurring(recurring.getId(), date, recurring.getPrice());

        return ReservationResponse.builder()
                .id(recurring.getId())
                .status(isFullyPaid ? ReservationStatus.PAYED : ReservationStatus.CONFIRMED) // 🆕
                .courtId(recurring.getCourt().getId())
                .courtName(recurring.getCourt().getName())
                .complexName(recurring.getCourt().getComplex().getName())
                .date(date)
                .time(LocalTime.parse(recurring.getTime().format(DateTimeFormatter.ofPattern("HH:mm"))))
                .price(recurring.getPrice())
                .customerName(recurring.getCustomerName())
                .customerPhone(recurring.getCustomerPhone())
                .createdByAdmin(true)
                .isRecurring(true)
                .recurringId(recurring.getId())
                .isFullyPaid(isFullyPaid) // 🆕
                .createdAt(recurring.getCreatedAt())
                .build();
    }

    /**
     * 🆕 Calcula si una fecha de reserva recurrente está completamente pagada
     */
    private boolean calculateIsFullyPaidForRecurring(Long recurringId, LocalDate date, BigDecimal courtPrice) {
        // Obtener pagos para esta fecha
        List<PlayerPayment> payments = playerPaymentRepository
                .findByRecurringReservationIdAndPaymentDate(recurringId, date);

        BigDecimal totalPaidByPlayers = payments.stream()
                .map(PlayerPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Obtener productos para esta fecha
        List<ExtraProduct> products = extraProductRepository
                .findByRecurringReservationIdAndProductDate(recurringId, date);

        BigDecimal productsTotal = products.stream()
                .map(ExtraProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidProductsTotal = products.stream()
                .filter(ExtraProduct::getPaid)
                .map(ExtraProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total a cobrar = precio del turno + productos
        BigDecimal grandTotal = courtPrice.add(productsTotal);

        // Total pagado = pagos de jugadores + productos pagados
        BigDecimal totalPaid = totalPaidByPlayers.add(paidProductsTotal);

        // Está completamente pagado si no queda nada pendiente
        BigDecimal pending = grandTotal.subtract(totalPaid);
        return pending.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Obtener todas las reservas del complejo (con filtros)
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getComplexReservations(
            LocalDate startDate,
            LocalDate endDate,
            ReservationStatus status) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.BUSINESS && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos");
        }

        if (currentUser.getComplex() == null) {
            throw new BadRequestException("No tienes un complejo asignado");
        }

        Long complexId = currentUser.getComplex().getId();

        List<Reservation> reservations = reservationRepository
                .findByComplexIdAndDateBetween(complexId, startDate, endDate);

        if (status != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getStatus() == status)
                    .collect(Collectors.toList());
        }

        return reservations.stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancelar reserva del complejo
     */
    @Transactional
    public void cancelReservation(Long reservationId) {
        User currentUser = getCurrentUser();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", reservationId));

        if (currentUser.getRole() == Role.BUSINESS) {
            if (currentUser.getComplex() == null ||
                    !currentUser.getComplex().getId().equals(reservation.getCourt().getComplex().getId())) {
                throw new UnauthorizedException("No administras este complejo");
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("❌ Reserva #{} cancelada por admin {}", reservationId, currentUser.getEmail());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    /**
     * Obtener estadísticas del dashboard para una fecha
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(LocalDate date) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.BUSINESS && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos");
        }

        Long complexId;
        if (currentUser.getRole() == Role.ADMIN) {
            throw new BadRequestException("Especifica el complejo");
        } else {
            if (currentUser.getComplex() == null) {
                throw new BadRequestException("No tienes un complejo asignado");
            }
            complexId = currentUser.getComplex().getId();
        }

        // 1. Obtener todas las reservas del día (normales + fijas)
        List<Reservation> reservations = reservationRepository
                .findByComplexIdAndDate(complexId, date);

        List<RecurringReservation> activeRecurrings = recurringRepository
                .findActiveByComplexId(complexId);

        // Contar confirmadas y pendientes de pago
        int confirmedCount = 0;
        int pendingPaymentCount = 0;

        BigDecimal cashFromReservations = BigDecimal.ZERO;
        BigDecimal electronicFromReservations = BigDecimal.ZERO;
        BigDecimal cashFromProducts = BigDecimal.ZERO;
        BigDecimal electronicFromProducts = BigDecimal.ZERO;

        // Procesar reservas normales
        for (Reservation res : reservations) {
            if (res.getStatus() == ReservationStatus.CONFIRMED || res.getStatus() == ReservationStatus.PAYED) {
                confirmedCount++;

                if (!res.isFullyPaid()) {
                    pendingPaymentCount++;
                }

                // Sumar pagos de jugadores
                List<PlayerPayment> payments = playerPaymentRepository
                        .findByReservationId(res.getId());

                for (PlayerPayment payment : payments) {
                    if (payment.getMethod() == PaymentMethod.CASH) {
                        cashFromReservations = cashFromReservations.add(payment.getAmount());
                    } else {
                        electronicFromReservations = electronicFromReservations.add(payment.getAmount());
                    }
                }

                // Sumar productos pagados
                List<ExtraProduct> products = extraProductRepository
                        .findByReservationId(res.getId());

                for (ExtraProduct product : products) {
                    if (product.getPaid()) {
                        if (product.getPaymentMethod() == PaymentMethod.CASH) {
                            cashFromProducts = cashFromProducts.add(product.getTotalPrice());
                        } else {
                            electronicFromProducts = electronicFromProducts.add(product.getTotalPrice());
                        }
                    }
                }
            }
        }

        // Procesar reservas fijas para esta fecha
        for (RecurringReservation recurring : activeRecurrings) {
            if (date.getDayOfWeek() != recurring.getDayOfWeek()) continue;
            if (date.isBefore(recurring.getStartDate())) continue;
            if (recurring.getEndDate() != null && date.isAfter(recurring.getEndDate())) continue;

            boolean hasException = exceptionRepository
                    .existsByRecurringReservationIdAndExceptionDate(recurring.getId(), date);

            if (!hasException) {
                confirmedCount++;

                boolean isFullyPaid = calculateIsFullyPaidForRecurring(
                        recurring.getId(), date, recurring.getPrice());

                if (!isFullyPaid) {
                    pendingPaymentCount++;
                }

                // Sumar pagos
                List<PlayerPayment> payments = playerPaymentRepository
                        .findByRecurringReservationIdAndPaymentDate(recurring.getId(), date);

                for (PlayerPayment payment : payments) {
                    if (payment.getMethod() == PaymentMethod.CASH) {
                        cashFromReservations = cashFromReservations.add(payment.getAmount());
                    } else {
                        electronicFromReservations = electronicFromReservations.add(payment.getAmount());
                    }
                }

                // Sumar productos
                List<ExtraProduct> products = extraProductRepository
                        .findByRecurringReservationIdAndProductDate(recurring.getId(), date);

                for (ExtraProduct product : products) {
                    if (product.getPaid()) {
                        if (product.getPaymentMethod() == PaymentMethod.CASH) {
                            cashFromProducts = cashFromProducts.add(product.getTotalPrice());
                        } else {
                            electronicFromProducts = electronicFromProducts.add(product.getTotalPrice());
                        }
                    }
                }
            }
        }

        // Calcular totales
        BigDecimal totalCash = cashFromReservations.add(cashFromProducts);
        BigDecimal totalElectronic = electronicFromReservations.add(electronicFromProducts);
        BigDecimal grandTotal = totalCash.add(totalElectronic);

        // Contar canchas del complejo
        int courtsCount = courtRepository.countByComplexId(complexId);

        return DashboardStatsResponse.builder()
                .confirmedReservations(confirmedCount)
                .pendingPaymentReservations(pendingPaymentCount)
                .totalCourts(courtsCount)
                .cashRevenue(cashFromReservations)
                .electronicRevenue(electronicFromReservations)
                .totalRevenue(cashFromReservations.add(electronicFromReservations))
                .productsCash(cashFromProducts)
                .productsElectronic(electronicFromProducts)
                .productsTotal(cashFromProducts.add(electronicFromProducts))
                .grandTotalCash(totalCash)
                .grandTotalElectronic(totalElectronic)
                .grandTotal(grandTotal)
                .build();
    }

    /**
     * 🆕 Mapea reserva normal con cálculo de isFullyPaid
     */
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
                .createdAt(reservation.getCreatedAt())
                .customerName(reservation.getCustomerName())
                .customerPhone(reservation.getCustomerPhone())
                .customerEmail(reservation.getCustomerEmail())
                .notes(reservation.getNotes())
                .createdByAdmin(reservation.getCreatedByAdmin())
                .isRecurring(false)
                .isFullyPaid(reservation.isFullyPaid()) // 🆕 Usa el método de la entidad
                .build();
    }
}
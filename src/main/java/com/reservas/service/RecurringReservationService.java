// src/main/java/com/reservas/service/RecurringReservationService.java
package com.reservas.service;

import com.reservas.dto.request.AddRecurringPaymentRequest;
import com.reservas.dto.request.AddRecurringProductRequest;
import com.reservas.dto.request.RecurringExceptionRequest;
import com.reservas.dto.request.RecurringReservationRequest;
import com.reservas.dto.response.*;
import com.reservas.entity.*;
import com.reservas.enums.PaymentMethod;
import com.reservas.enums.RecurringStatus;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringReservationService {

    private final RecurringReservationRepository recurringRepository;
    private final RecurringExceptionRepository exceptionRepository;
    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final PlayerPaymentRepository playerPaymentRepository;
    private final ExtraProductRepository extraProductRepository;
    private final CashRegisterService cashRegisterService; // 🆕 AGREGAR

    // ==================== MÉTODOS EXISTENTES ====================

    @Transactional
    public RecurringReservationResponse createRecurringReservation(RecurringReservationRequest request) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", request.getCourtId()));

        validateComplexAccess(currentUser, court);

        if (recurringRepository.existsActiveForSlot(
                request.getCourtId(),
                request.getDayOfWeek(),
                request.getTime(),
                request.getStartDate())) {
            throw new BadRequestException("Ya existe una reserva fija para ese horario");
        }

        LocalDate firstOccurrence = findFirstOccurrence(request.getStartDate(), request.getDayOfWeek());
        if (reservationRepository.existsActiveReservation(
                request.getCourtId(),
                firstOccurrence,
                request.getTime())) {
            throw new BadRequestException("Ya existe una reserva para el " + firstOccurrence + " a las " + request.getTime());
        }

        RecurringReservation recurring = RecurringReservation.builder()
                .court(court)
                .dayOfWeek(request.getDayOfWeek())
                .time(request.getTime())
                .price(request.getPrice())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .notes(request.getNotes())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(RecurringStatus.ACTIVE)
                .createdBy(currentUser)
                .build();

        recurring = recurringRepository.save(recurring);

        log.info("✅ Reserva fija #{} creada: {} {} a las {} por {}",
                recurring.getId(),
                getDayName(recurring.getDayOfWeek()),
                recurring.getTime(),
                recurring.getCustomerName(),
                currentUser.getEmail());

        return mapToResponse(recurring);
    }

    private LocalDate findFirstOccurrence(LocalDate startDate, DayOfWeek targetDay) {
        LocalDate date = startDate;
        while (date.getDayOfWeek() != targetDay) {
            date = date.plusDays(1);
        }
        return date;
    }

    @Transactional
    public void cancelSpecificDate(RecurringExceptionRequest request) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(request.getRecurringReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva fija", "id", request.getRecurringReservationId()));

        validateComplexAccess(currentUser, recurring.getCourt());

        if (request.getExceptionDate().getDayOfWeek() != recurring.getDayOfWeek()) {
            throw new BadRequestException("La fecha no corresponde al día de la reserva fija");
        }

        if (exceptionRepository.existsByRecurringReservationIdAndExceptionDate(
                recurring.getId(), request.getExceptionDate())) {
            throw new BadRequestException("Ya existe una excepción para esa fecha");
        }

        RecurringException exception = RecurringException.builder()
                .recurringReservation(recurring)
                .exceptionDate(request.getExceptionDate())
                .reason(request.getReason())
                .cancelledBy(currentUser)
                .build();

        exceptionRepository.save(exception);

        log.info("📅 Excepción creada para reserva fija #{}: fecha {} - {}",
                recurring.getId(),
                request.getExceptionDate(),
                request.getReason());
    }

    @Transactional
    public void restoreSpecificDate(Long recurringId, LocalDate date) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        exceptionRepository.findByRecurringReservationIdAndExceptionDate(recurringId, date)
                .ifPresent(exception -> {
                    exceptionRepository.delete(exception);
                    log.info("✅ Excepción eliminada para reserva fija #{}: fecha {}",
                            recurringId, date);
                });
    }

    @Transactional
    public void cancelPermanently(Long recurringId) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        recurring.setStatus(RecurringStatus.CANCELLED);
        recurringRepository.save(recurring);

        log.info("❌ Reserva fija #{} cancelada permanentemente por {}",
                recurringId, currentUser.getEmail());
    }

    @Transactional
    public void reactivate(Long recurringId) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        recurring.setStatus(RecurringStatus.ACTIVE);
        recurringRepository.save(recurring);

        log.info("✅ Reserva fija #{} reactivada por {}", recurringId, currentUser.getEmail());
    }

    @Transactional(readOnly = true)
    public List<RecurringReservationResponse> getComplexRecurringReservations() {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        if (currentUser.getComplex() == null) {
            throw new BadRequestException("No tienes un complejo asignado");
        }

        List<RecurringReservation> reservations = recurringRepository
                .findActiveByComplexId(currentUser.getComplex().getId());

        return reservations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isSlotOccupiedByRecurring(Long courtId, LocalDate date, LocalTime time) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        var recurringOpt = recurringRepository.findActiveForSlot(courtId, dayOfWeek, time, date);

        if (recurringOpt.isEmpty()) {
            return false;
        }

        RecurringReservation recurring = recurringOpt.get();

        boolean hasException = exceptionRepository.existsByRecurringReservationIdAndExceptionDate(
                recurring.getId(), date);

        return !hasException;
    }

    @Transactional(readOnly = true)
    public RecurringReservationResponse getRecurringForSlot(Long courtId, LocalDate date, LocalTime time) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        var recurringOpt = recurringRepository.findActiveForSlot(courtId, dayOfWeek, time, date);

        if (recurringOpt.isEmpty()) {
            return null;
        }

        RecurringReservation recurring = recurringOpt.get();

        boolean hasException = exceptionRepository.existsByRecurringReservationIdAndExceptionDate(
                recurring.getId(), date);

        if (hasException) {
            return null;
        }

        return mapToResponse(recurring);
    }

    @Transactional(readOnly = true)
    public RecurringReservationResponse getRecurringReservationById(Long id) {
        RecurringReservation reservation = recurringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva Recurrente", "id", id));
        return mapToResponse(reservation);
    }

    // ==================== DETALLE POR FECHA ====================

    @Transactional(readOnly = true)
    public RecurringDateDetailResponse getRecurringDateDetail(Long recurringId, LocalDate date) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        if (date.getDayOfWeek() != recurring.getDayOfWeek()) {
            throw new BadRequestException("La fecha no corresponde al día de la reserva fija");
        }

        List<PlayerPayment> payments = playerPaymentRepository
                .findByRecurringReservationIdAndPaymentDate(recurringId, date);

        List<ExtraProduct> products = extraProductRepository
                .findByRecurringReservationIdAndProductDate(recurringId, date);

        BigDecimal totalPaidByPlayers = payments.stream()
                .map(PlayerPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal productsTotal = products.stream()
                .map(ExtraProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidProductsTotal = products.stream()
                .filter(ExtraProduct::getPaid)
                .map(ExtraProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = recurring.getPrice().add(productsTotal);
        BigDecimal totalPaid = totalPaidByPlayers.add(paidProductsTotal);
        BigDecimal totalPending = grandTotal.subtract(totalPaid).max(BigDecimal.ZERO);

        return RecurringDateDetailResponse.builder()
                .recurringId(recurring.getId())
                .courtId(recurring.getCourt().getId())
                .courtName(recurring.getCourt().getName())
                .complexName(recurring.getCourt().getComplex().getName())
                .date(date)
                .time(recurring.getTime())
                .price(recurring.getPrice())
                .status(recurring.getStatus().name())
                .customerName(recurring.getCustomerName())
                .customerPhone(recurring.getCustomerPhone())
                .customerEmail(recurring.getCustomerEmail())
                .notes(recurring.getNotes())
                .dayOfWeekDisplay(getDayName(recurring.getDayOfWeek()))
                .isFullyPaid(totalPending.compareTo(BigDecimal.ZERO) <= 0)
                .totalPaid(totalPaid)
                .totalPending(totalPending)
                .productsTotal(productsTotal)
                .grandTotal(grandTotal)
                .playerPayments(payments.stream().map(this::mapToPaymentResponse).collect(Collectors.toList()))
                .extraProducts(products.stream().map(this::mapToProductResponse).collect(Collectors.toList()))
                .build();
    }

    // ==================== PAGOS ====================

    @Transactional
    public PlayerPaymentResponse addRecurringPayment(Long recurringId, AddRecurringPaymentRequest request) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        if (request.getDate().getDayOfWeek() != recurring.getDayOfWeek()) {
            throw new BadRequestException("La fecha no corresponde al día de la reserva fija");
        }

        int currentCount = playerPaymentRepository.countByRecurringReservationIdAndPaymentDate(
                recurringId, request.getDate());
        if (currentCount >= 4) {
            throw new BadRequestException("Ya hay 4 pagos registrados para esta fecha");
        }

        PaymentMethod method = PaymentMethod.valueOf(request.getMethod());

        PlayerPayment payment = PlayerPayment.builder()
                .recurringReservation(recurring)
                .paymentDate(request.getDate())
                .playerName(request.getPlayerName())
                .amount(request.getAmount())
                .method(method)
                .paidAt(LocalDateTime.now())
                .createdBy(currentUser) // 🆕
                .build();

        payment = playerPaymentRepository.save(payment);

        // Verificar si el pago cubre productos extras
        markRecurringProductsAsPaidIfExcess(recurring, request.getDate(), request.getMethod());

        // 🆕 REGISTRAR EN CAJA SI ES EFECTIVO
        if (method == PaymentMethod.CASH) {
            String info = buildRecurringInfo(recurring, request.getDate());
            cashRegisterService.registerAutomaticPayment(payment, info);
        }

        log.info("💰 Pago registrado para reserva fija #{} fecha {}: {} - ${}",
                recurringId, request.getDate(), request.getPlayerName(), request.getAmount());

        return mapToPaymentResponse(payment);
    }

    @Transactional
    public void removeRecurringPayment(Long recurringId, Long paymentId) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        PlayerPayment payment = playerPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", paymentId));

        if (payment.getRecurringReservation() == null ||
                !payment.getRecurringReservation().getId().equals(recurringId)) {
            throw new BadRequestException("El pago no pertenece a esta reserva");
        }

        validateComplexAccess(currentUser, payment.getRecurringReservation().getCourt());

        // 🆕 ELIMINAR DE CAJA SI ERA EFECTIVO
        if (payment.getMethod() == PaymentMethod.CASH) {
            cashRegisterService.removeAutomaticPayment(paymentId);
        }

        playerPaymentRepository.delete(payment);

        log.info("🗑️ Pago #{} eliminado de reserva fija #{}", paymentId, recurringId);
    }

    // ==================== PRODUCTOS ====================

    @Transactional
    public ExtraProductResponse addRecurringProduct(Long recurringId, AddRecurringProductRequest request) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        RecurringReservation recurring = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva fija", "id", recurringId));

        validateComplexAccess(currentUser, recurring.getCourt());

        if (request.getDate().getDayOfWeek() != recurring.getDayOfWeek()) {
            throw new BadRequestException("La fecha no corresponde al día de la reserva fija");
        }

        BigDecimal totalPrice = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        ExtraProduct product = ExtraProduct.builder()
                .recurringReservation(recurring)
                .productDate(request.getDate())
                .name(request.getName())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .paid(false)
                .addedAt(LocalDateTime.now())
                .build();

        product = extraProductRepository.save(product);

        log.info("🛒 Producto agregado a reserva fija #{} fecha {}: {} x{} = ${}",
                recurringId, request.getDate(), request.getName(), request.getQuantity(), totalPrice);

        return mapToProductResponse(product);
    }

    @Transactional
    public void removeRecurringProduct(Long recurringId, Long productId) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        ExtraProduct product = extraProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (product.getRecurringReservation() == null ||
                !product.getRecurringReservation().getId().equals(recurringId)) {
            throw new BadRequestException("El producto no pertenece a esta reserva");
        }

        validateComplexAccess(currentUser, product.getRecurringReservation().getCourt());

        // 🆕 ELIMINAR DE CAJA SI ESTABA PAGADO EN EFECTIVO
        if (product.getPaid() && product.getPaymentMethod() == PaymentMethod.CASH) {
            cashRegisterService.removeAutomaticProductSale(productId);
        }

        extraProductRepository.delete(product);

        log.info("🗑️ Producto #{} eliminado de reserva fija #{}", productId, recurringId);
    }

    @Transactional
    public ExtraProductResponse markRecurringProductAsPaid(Long recurringId, Long productId, String method) {
        User currentUser = getCurrentUser();
        validateAdminAccess(currentUser);

        ExtraProduct product = extraProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productId));

        if (product.getRecurringReservation() == null ||
                !product.getRecurringReservation().getId().equals(recurringId)) {
            throw new BadRequestException("El producto no pertenece a esta reserva");
        }

        validateComplexAccess(currentUser, product.getRecurringReservation().getCourt());

        if (product.getPaid()) {
            throw new BadRequestException("El producto ya está pagado");
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(method);

        product.setPaid(true);
        product.setPaymentMethod(paymentMethod);
        product.setPaidAt(LocalDateTime.now());

        product = extraProductRepository.save(product);

        // 🆕 REGISTRAR EN CAJA SI ES EFECTIVO
        if (paymentMethod == PaymentMethod.CASH) {
            RecurringReservation recurring = product.getRecurringReservation();
            String info = buildRecurringInfo(recurring, product.getProductDate());
            cashRegisterService.registerAutomaticProductSale(product, info);
        }

        log.info("✅ Producto #{} marcado como pagado en reserva fija #{}", productId, recurringId);

        return mapToProductResponse(product);
    }

    // ==================== HELPERS ====================

    private void markRecurringProductsAsPaidIfExcess(RecurringReservation recurring, LocalDate date, String method) {
        List<PlayerPayment> payments = playerPaymentRepository
                .findByRecurringReservationIdAndPaymentDate(recurring.getId(), date);

        BigDecimal totalPaid = payments.stream()
                .map(PlayerPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(recurring.getPrice()) > 0) {
            BigDecimal excess = totalPaid.subtract(recurring.getPrice());

            PaymentMethod paymentMethod = PaymentMethod.valueOf(method);

            List<ExtraProduct> unpaidProducts = extraProductRepository
                    .findByRecurringReservationIdAndProductDate(recurring.getId(), date)
                    .stream()
                    .filter(p -> !p.getPaid())
                    .sorted((a, b) -> a.getAddedAt().compareTo(b.getAddedAt()))
                    .collect(Collectors.toList());

            BigDecimal covered = BigDecimal.ZERO;
            for (ExtraProduct product : unpaidProducts) {
                if (covered.add(product.getTotalPrice()).compareTo(excess) <= 0) {
                    product.setPaid(true);
                    product.setPaymentMethod(paymentMethod);
                    product.setPaidAt(LocalDateTime.now());
                    extraProductRepository.save(product);
                    covered = covered.add(product.getTotalPrice());

                    // ✅ FIX: NO registrar en caja - ya está incluido en el pago del jugador
                    log.info("✅ Producto {} marcado como pagado automáticamente (excedente)", product.getName());
                } else {
                    break;
                }
            }
        }
    }

    // 🆕 HELPER PARA GENERAR INFO DE RESERVA RECURRENTE
    private String buildRecurringInfo(RecurringReservation recurring, LocalDate date) {
        return String.format("%s - %s - %s (%s)",
                recurring.getCourt().getName(),
                recurring.getTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                recurring.getCustomerName(),
                date.format(DateTimeFormatter.ofPattern("dd/MM")));
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private void validateAdminAccess(User user) {
        if (user.getRole() != Role.BUSINESS && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tienes permisos para esta acción");
        }
    }

    private void validateComplexAccess(User user, Court court) {
        if (user.getRole() == Role.BUSINESS) {
            if (user.getComplex() == null ||
                    !user.getComplex().getId().equals(court.getComplex().getId())) {
                throw new UnauthorizedException("No administras este complejo");
            }
        }
    }

    private String getDayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
    }

    // ==================== MAPPERS ====================

    private RecurringReservationResponse mapToResponse(RecurringReservation recurring) {
        return RecurringReservationResponse.builder()
                .id(recurring.getId())
                .courtId(recurring.getCourt().getId())
                .courtName(recurring.getCourt().getName())
                .complexName(recurring.getCourt().getComplex().getName())
                .dayOfWeek(recurring.getDayOfWeek())
                .dayOfWeekDisplay(getDayName(recurring.getDayOfWeek()))
                .time(recurring.getTime())
                .price(recurring.getPrice())
                .customerName(recurring.getCustomerName())
                .customerPhone(recurring.getCustomerPhone())
                .customerEmail(recurring.getCustomerEmail())
                .notes(recurring.getNotes())
                .startDate(recurring.getStartDate())
                .endDate(recurring.getEndDate())
                .status(recurring.getStatus())
                .createdAt(recurring.getCreatedAt())
                .build();
    }

    private PlayerPaymentResponse mapToPaymentResponse(PlayerPayment payment) {
        return PlayerPaymentResponse.builder()
                .id(payment.getId())
                .playerName(payment.getPlayerName())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private ExtraProductResponse mapToProductResponse(ExtraProduct product) {
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
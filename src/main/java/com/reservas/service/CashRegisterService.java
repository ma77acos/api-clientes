// src/main/java/com/reservas/service/CashRegisterService.java
package com.reservas.service;

import com.reservas.dto.request.*;
import com.reservas.dto.response.*;
import com.reservas.entity.*;
import com.reservas.enums.*;
import com.reservas.exception.*;
import com.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    private final PlayerPaymentRepository playerPaymentRepository; // 🆕
    private final ExtraProductRepository extraProductRepository;   // 🆕
    private final ReservationRepository reservationRepository;     // 🆕
    private final RecurringReservationRepository recurringReservationRepository; // 🆕

    // ========================================
    // APERTURA DE CAJA
    // ========================================

    @Transactional
    public CashRegisterResponse openCashRegister(OpenCashRegisterRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);
        LocalDate today = LocalDate.now();

        // Verificar si ya existe caja para hoy
        if (cashRegisterRepository.existsByComplexIdAndStatus(complexId, CashRegisterStatus.OPEN)) {
            throw new BadRequestException("Ya existe una caja abierta. Cerrala primero antes de abrir una nueva.");
        }

        CashRegister cashRegister = CashRegister.builder()
                .complex(currentUser.getComplex())
                .date(today)
                .openingAmount(request.getOpeningAmount())
                .openedAt(LocalDateTime.now())
                .openedBy(currentUser)
                .status(CashRegisterStatus.OPEN)
                .build();

        cashRegister = cashRegisterRepository.save(cashRegister);

        // 🆕 SINCRONIZAR PAGOS EXISTENTES DEL DÍA
        int syncedPayments = syncExistingPayments(cashRegister, complexId, today);
        int syncedProducts = syncExistingProducts(cashRegister, complexId, today);

        log.info("✅ Caja abierta para {} con monto inicial ${}. Sincronizados: {} pagos, {} productos",
                today, request.getOpeningAmount(), syncedPayments, syncedProducts);

        return mapToResponse(cashRegister);
    }

    // ========================================
    // 🆕 SINCRONIZAR PAGOS EXISTENTES
    // ========================================

    private int syncExistingPayments(CashRegister cashRegister, Long complexId, LocalDate date) {
        int count = 0;

        // 1. Pagos de reservas normales del día (solo CASH)
        List<Reservation> reservations = reservationRepository.findByComplexIdAndDate(complexId, date);

        for (Reservation reservation : reservations) {
            List<PlayerPayment> payments = playerPaymentRepository.findByReservationId(reservation.getId());

            for (PlayerPayment payment : payments) {
                if (payment.getMethod() == PaymentMethod.CASH) {
                    // Verificar que no exista ya
                    if (!cashMovementRepository.existsByPlayerPaymentId(payment.getId())) {
                        String info = buildReservationInfo(reservation);

                        CashMovement movement = CashMovement.builder()
                                .cashRegister(cashRegister)
                                .type(MovementType.INCOME)
                                .category(MovementCategory.COURT_PAYMENT)
                                .description(info)
                                .amount(payment.getAmount())
                                .playerPayment(payment)
                                .createdBy(payment.getCreatedBy())
                                .build();

                        cashMovementRepository.save(movement);
                        count++;
                    }
                }
            }
        }

        // 2. Pagos de reservas recurrentes del día (solo CASH)
        List<RecurringReservation> recurrings = recurringReservationRepository.findActiveByComplexId(complexId);

        for (RecurringReservation recurring : recurrings) {
            // Verificar si la fecha corresponde al día de la semana
            if (date.getDayOfWeek() != recurring.getDayOfWeek()) {
                continue;
            }
            if (date.isBefore(recurring.getStartDate())) {
                continue;
            }
            if (recurring.getEndDate() != null && date.isAfter(recurring.getEndDate())) {
                continue;
            }

            List<PlayerPayment> payments = playerPaymentRepository
                    .findByRecurringReservationIdAndPaymentDate(recurring.getId(), date);

            for (PlayerPayment payment : payments) {
                if (payment.getMethod() == PaymentMethod.CASH) {
                    // Verificar que no exista ya
                    if (!cashMovementRepository.existsByPlayerPaymentId(payment.getId())) {
                        String info = buildRecurringInfo(recurring, date);

                        CashMovement movement = CashMovement.builder()
                                .cashRegister(cashRegister)
                                .type(MovementType.INCOME)
                                .category(MovementCategory.COURT_PAYMENT)
                                .description(info)
                                .amount(payment.getAmount())
                                .playerPayment(payment)
                                .createdBy(payment.getCreatedBy())
                                .build();

                        cashMovementRepository.save(movement);
                        count++;
                    }
                }
            }
        }

        return count;
    }

    // ========================================
    // 🆕 SINCRONIZAR PRODUCTOS EXISTENTES
    // ========================================

    private int syncExistingProducts(CashRegister cashRegister, Long complexId, LocalDate date) {
        int count = 0;

        // 1. Productos de reservas normales del día (solo CASH y pagados)
        List<Reservation> reservations = reservationRepository.findByComplexIdAndDate(complexId, date);

        for (Reservation reservation : reservations) {
            List<ExtraProduct> products = extraProductRepository.findByReservationId(reservation.getId());

            for (ExtraProduct product : products) {
                if (product.getPaid() && product.getPaymentMethod() == PaymentMethod.CASH) {
                    // Verificar que no exista ya
                    if (!cashMovementRepository.existsByExtraProductId(product.getId())) {
                        String info = product.getName() + " - " + buildReservationInfo(reservation);

                        CashMovement movement = CashMovement.builder()
                                .cashRegister(cashRegister)
                                .type(MovementType.INCOME)
                                .category(MovementCategory.PRODUCT_SALE)
                                .description(info)
                                .amount(product.getTotalPrice())
                                .extraProduct(product)
                                .createdBy(null)
                                .build();

                        cashMovementRepository.save(movement);
                        count++;
                    }
                }
            }
        }

        // 2. Productos de reservas recurrentes del día (solo CASH y pagados)
        List<RecurringReservation> recurrings = recurringReservationRepository.findActiveByComplexId(complexId);

        for (RecurringReservation recurring : recurrings) {
            if (date.getDayOfWeek() != recurring.getDayOfWeek()) {
                continue;
            }
            if (date.isBefore(recurring.getStartDate())) {
                continue;
            }
            if (recurring.getEndDate() != null && date.isAfter(recurring.getEndDate())) {
                continue;
            }

            List<ExtraProduct> products = extraProductRepository
                    .findByRecurringReservationIdAndProductDate(recurring.getId(), date);

            for (ExtraProduct product : products) {
                if (product.getPaid() && product.getPaymentMethod() == PaymentMethod.CASH) {
                    // Verificar que no exista ya
                    if (!cashMovementRepository.existsByExtraProductId(product.getId())) {
                        String info = product.getName() + " - " + buildRecurringInfo(recurring, date);

                        CashMovement movement = CashMovement.builder()
                                .cashRegister(cashRegister)
                                .type(MovementType.INCOME)
                                .category(MovementCategory.PRODUCT_SALE)
                                .description(info)
                                .amount(product.getTotalPrice())
                                .extraProduct(product)
                                .createdBy(null)
                                .build();

                        cashMovementRepository.save(movement);
                        count++;
                    }
                }
            }
        }

        return count;
    }

    // ========================================
    // 🆕 HELPERS PARA CONSTRUIR INFO
    // ========================================

    private String buildReservationInfo(Reservation reservation) {
        return String.format("%s - %s - %s",
                reservation.getCourt().getName(),
                reservation.getTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                reservation.getCustomerName() != null ? reservation.getCustomerName() : "Cliente");
    }

    private String buildRecurringInfo(RecurringReservation recurring, LocalDate date) {
        return String.format("%s - %s - %s (%s)",
                recurring.getCourt().getName(),
                recurring.getTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                recurring.getCustomerName(),
                date.format(DateTimeFormatter.ofPattern("dd/MM")));
    }

    // ========================================
    // CIERRE DE CAJA
    // ========================================

    @Transactional
    public CashRegisterResponse closeCashRegister(CloseCashRegisterRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        CashRegister cashRegister = cashRegisterRepository
                .findFirstByComplexIdAndStatusOrderByIdDesc(complexId, CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BadRequestException("No hay caja abierta para cerrar"));

        BigDecimal expectedAmount = cashRegister.calculateExpectedAmount();
        BigDecimal difference = request.getActualAmount().subtract(expectedAmount);

        cashRegister.setExpectedAmount(expectedAmount);
        cashRegister.setActualAmount(request.getActualAmount());
        cashRegister.setDifference(difference);
        cashRegister.setCarryOverAmount(request.getCarryOverAmount());
        cashRegister.setClosedAt(LocalDateTime.now());
        cashRegister.setClosedBy(currentUser);
        cashRegister.setClosingNotes(request.getClosingNotes());
        cashRegister.setStatus(CashRegisterStatus.CLOSED);

        cashRegister = cashRegisterRepository.save(cashRegister);

        String diffStr = difference.compareTo(BigDecimal.ZERO) >= 0
                ? "+$" + difference : "-$" + difference.abs();
        log.info("✅ Caja cerrada. Esperado: ${}, Real: ${}, Diferencia: {}",
                expectedAmount, request.getActualAmount(), diffStr);

        return mapToResponse(cashRegister);
    }

    // ========================================
    // OBTENER CAJA ACTUAL
    // ========================================

    @Transactional(readOnly = true)
    public CashRegisterResponse getCurrentCashRegister() {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);
        LocalDate today = LocalDate.now();

        CashRegister cashRegister = cashRegisterRepository
                .findByComplexIdAndDate(complexId, today)
                .orElse(null);

        if (cashRegister == null) {
            BigDecimal suggested = getSuggestedOpeningAmount(complexId);
            return CashRegisterResponse.builder()
                    .date(today)
                    .status(null)
                    .openingAmount(suggested)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .expectedAmount(BigDecimal.ZERO)
                    .courtPaymentsTotal(BigDecimal.ZERO)
                    .productSalesTotal(BigDecimal.ZERO)
                    .otherIncomeTotal(BigDecimal.ZERO)
                    .purchasesTotal(BigDecimal.ZERO)
                    .servicesTotal(BigDecimal.ZERO)
                    .maintenanceTotal(BigDecimal.ZERO)
                    .salariesTotal(BigDecimal.ZERO)
                    .withdrawalsTotal(BigDecimal.ZERO)
                    .otherExpensesTotal(BigDecimal.ZERO)
                    .build();
        }

        return mapToResponse(cashRegister);
    }

    // ========================================
    // OBTENER CAJA POR FECHA
    // ========================================

    @Transactional(readOnly = true)
    public CashRegisterResponse getCashRegisterByDate(LocalDate date) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        CashRegister cashRegister = cashRegisterRepository
                .findByComplexIdAndDate(complexId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Caja", "fecha", date.toString()));

        return mapToResponse(cashRegister);
    }

    // ========================================
    // HISTORIAL DE CAJAS
    // ========================================

    @Transactional(readOnly = true)
    public List<CashRegisterSummaryResponse> getCashRegisterHistory(
            LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        List<CashRegister> registers = cashRegisterRepository
                .findByComplexIdAndDateBetween(complexId, startDate, endDate);

        return registers.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    // ========================================
    // AGREGAR MOVIMIENTO
    // ========================================

    @Transactional
    public CashMovementResponse addMovement(CashMovementRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        CashRegister cashRegister = cashRegisterRepository
                .findFirstByComplexIdAndStatusOrderByIdDesc(complexId, CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BadRequestException(
                        "No hay caja abierta. Abrí la caja primero."));

        CashMovement movement = CashMovement.builder()
                .cashRegister(cashRegister)
                .type(request.getType())
                .category(request.getCategory())
                .description(request.getDescription())
                .amount(request.getAmount())
                .createdBy(currentUser)
                .build();

        movement = cashMovementRepository.save(movement);

        log.info("📝 Movimiento registrado: {} ${} - {}",
                request.getType(), request.getAmount(), request.getCategory());

        return mapMovementToResponse(movement);
    }

    // ========================================
    // ELIMINAR MOVIMIENTO
    // ========================================

    @Transactional
    public void deleteMovement(Long movementId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        CashMovement movement = cashMovementRepository.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Movimiento", "id", movementId));

        if (!movement.getCashRegister().getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a este movimiento");
        }

        if (movement.getCashRegister().getStatus() != CashRegisterStatus.OPEN) {
            throw new BadRequestException("No se puede eliminar movimientos de una caja cerrada");
        }

        if (movement.getPlayerPayment() != null || movement.getExtraProduct() != null) {
            throw new BadRequestException(
                    "No se puede eliminar movimientos automáticos. " +
                            "Eliminá el pago o producto desde la reserva.");
        }

        cashMovementRepository.delete(movement);
        log.info("🗑️ Movimiento #{} eliminado", movementId);
    }

    // ========================================
    // REGISTRAR MOVIMIENTO AUTOMÁTICO (desde pagos nuevos)
    // ========================================

    @Transactional
    public void registerAutomaticPayment(PlayerPayment payment, String reservationInfo) {
        if (payment.getMethod() != PaymentMethod.CASH) {
            return;
        }

        Long complexId;
        try {
            complexId = getComplexIdFromPayment(payment);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo determinar el complejo del pago: {}", e.getMessage());
            return;
        }

        CashRegister cashRegister = cashRegisterRepository
                .findFirstByComplexIdAndStatusOrderByIdDesc(complexId, CashRegisterStatus.OPEN)
                .orElse(null);

        if (cashRegister == null) {
            log.warn("⚠️ No hay caja abierta para registrar pago automático");
            return;
        }

        if (cashMovementRepository.existsByPlayerPaymentId(payment.getId())) {
            return;
        }

        CashMovement movement = CashMovement.builder()
                .cashRegister(cashRegister)
                .type(MovementType.INCOME)
                .category(MovementCategory.COURT_PAYMENT)
                .description(reservationInfo)
                .amount(payment.getAmount())
                .playerPayment(payment)
                .createdBy(payment.getCreatedBy())
                .build();

        cashMovementRepository.save(movement);
        log.info("💰 Pago automático registrado en caja: ${}", payment.getAmount());
    }

    @Transactional
    public void registerAutomaticProductSale(ExtraProduct product, String reservationInfo) {
        if (product.getPaymentMethod() != PaymentMethod.CASH) {
            return;
        }

        Long complexId;
        try {
            complexId = getComplexIdFromProduct(product);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo determinar el complejo del producto: {}", e.getMessage());
            return;
        }

        CashRegister cashRegister = cashRegisterRepository
                .findFirstByComplexIdAndStatusOrderByIdDesc(complexId, CashRegisterStatus.OPEN)
                .orElse(null);

        if (cashRegister == null) {
            log.warn("⚠️ No hay caja abierta para registrar venta automática");
            return;
        }

        if (cashMovementRepository.existsByExtraProductId(product.getId())) {
            return;
        }

        CashMovement movement = CashMovement.builder()
                .cashRegister(cashRegister)
                .type(MovementType.INCOME)
                .category(MovementCategory.PRODUCT_SALE)
                .description(product.getName() + " - " + reservationInfo)
                .amount(product.getTotalPrice())
                .extraProduct(product)
                .createdBy(null)
                .build();

        cashMovementRepository.save(movement);
        log.info("🛒 Venta automática registrada en caja: ${}", product.getTotalPrice());
    }

    @Transactional
    public void removeAutomaticPayment(Long playerPaymentId) {
        cashMovementRepository.findByPlayerPaymentId(playerPaymentId)
                .ifPresent(movement -> {
                    cashMovementRepository.delete(movement);
                    log.info("🗑️ Movimiento de pago #{} eliminado de caja", playerPaymentId);
                });
    }

    @Transactional
    public void removeAutomaticProductSale(Long extraProductId) {
        cashMovementRepository.findByExtraProductId(extraProductId)
                .ifPresent(movement -> {
                    cashMovementRepository.delete(movement);
                    log.info("🗑️ Movimiento de producto #{} eliminado de caja", extraProductId);
                });
    }

    // ========================================
    // HELPERS
    // ========================================

    private BigDecimal getSuggestedOpeningAmount(Long complexId) {
        return cashRegisterRepository
                .findLastClosedBefore(complexId, LocalDate.now())
                .map(CashRegister::getCarryOverAmount)
                .orElse(BigDecimal.ZERO);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private Long getComplexId(User user) {
        if (user.getRole() != Role.BUSINESS && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("No tenés permisos");
        }
        if (user.getComplex() == null) {
            throw new BadRequestException("No tenés un complejo asignado");
        }
        return user.getComplex().getId();
    }

    private Long getComplexIdFromPayment(PlayerPayment payment) {
        if (payment.getReservation() != null) {
            return payment.getReservation().getCourt().getComplex().getId();
        }
        if (payment.getRecurringReservation() != null) {
            return payment.getRecurringReservation().getCourt().getComplex().getId();
        }
        throw new BadRequestException("No se pudo determinar el complejo del pago");
    }

    private Long getComplexIdFromProduct(ExtraProduct product) {
        if (product.getReservation() != null) {
            return product.getReservation().getCourt().getComplex().getId();
        }
        if (product.getRecurringReservation() != null) {
            return product.getRecurringReservation().getCourt().getComplex().getId();
        }
        throw new BadRequestException("No se pudo determinar el complejo del producto");
    }

    // ========================================
    // MAPPERS
    // ========================================

    private CashRegisterResponse mapToResponse(CashRegister cr) {
        List<CashMovement> movements = cashMovementRepository
                .findByCashRegisterIdOrderByCreatedAtDesc(cr.getId());

        BigDecimal courtPayments = movements.stream()
                .filter(m -> m.getCategory() == MovementCategory.COURT_PAYMENT)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal productSales = movements.stream()
                .filter(m -> m.getCategory() == MovementCategory.PRODUCT_SALE)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal otherIncome = movements.stream()
                .filter(m -> m.getCategory() == MovementCategory.OTHER_INCOME)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal purchases = movements.stream()
                .filter(m -> m.getCategory() != null && m.getCategory().name().startsWith("PURCHASE"))
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal services = movements.stream()
                .filter(m -> m.getCategory() != null && m.getCategory().name().startsWith("SERVICE"))
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maintenance = movements.stream()
                .filter(m -> m.getCategory() == MovementCategory.MAINTENANCE)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal salaries = movements.stream()
                .filter(m -> m.getCategory() == MovementCategory.SALARY)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal withdrawals = movements.stream()
                .filter(m -> m.getCategory() == MovementCategory.OWNER_WITHDRAWAL)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal otherExpenses = movements.stream()
                .filter(m -> m.getCategory() == MovementCategory.OTHER_EXPENSE)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CashRegisterResponse.builder()
                .id(cr.getId())
                .date(cr.getDate())
                .status(cr.getStatus())
                .openingAmount(cr.getOpeningAmount())
                .openedAt(cr.getOpenedAt())
                .openedByName(cr.getOpenedBy() != null ? cr.getOpenedBy().getDisplayName() : null)
                .totalIncome(cr.calculateTotalIncome())
                .totalExpense(cr.calculateTotalExpense())
                .expectedAmount(cr.calculateExpectedAmount())
                .courtPaymentsTotal(courtPayments)
                .productSalesTotal(productSales)
                .otherIncomeTotal(otherIncome)
                .purchasesTotal(purchases)
                .servicesTotal(services)
                .maintenanceTotal(maintenance)
                .salariesTotal(salaries)
                .withdrawalsTotal(withdrawals)
                .otherExpensesTotal(otherExpenses)
                .actualAmount(cr.getActualAmount())
                .difference(cr.getDifference())
                .carryOverAmount(cr.getCarryOverAmount())
                .closedAt(cr.getClosedAt())
                .closedByName(cr.getClosedBy() != null ? cr.getClosedBy().getDisplayName() : null)
                .closingNotes(cr.getClosingNotes())
                .movements(movements.stream()
                        .map(this::mapMovementToResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private CashMovementResponse mapMovementToResponse(CashMovement m) {
        String reservationInfo = null;
        boolean isAutomatic = false;

        if (m.getPlayerPayment() != null || m.getExtraProduct() != null) {
            isAutomatic = true;
            reservationInfo = m.getDescription();
        }

        return CashMovementResponse.builder()
                .id(m.getId())
                .type(m.getType())
                .category(m.getCategory())
                .categoryDisplayName(m.getCategory() != null ? m.getCategory().getDisplayName() : "Desconocido")
                .description(m.getDescription())
                .amount(m.getAmount())
                .createdAt(m.getCreatedAt())
                .createdByName(m.getCreatedBy() != null ? m.getCreatedBy().getDisplayName() : null)
                .isAutomatic(isAutomatic)
                .reservationInfo(reservationInfo)
                .build();
    }

    private CashRegisterSummaryResponse mapToSummary(CashRegister cr) {
        return CashRegisterSummaryResponse.builder()
                .id(cr.getId())
                .date(cr.getDate())
                .status(cr.getStatus())
                .openingAmount(cr.getOpeningAmount())
                .totalIncome(cr.calculateTotalIncome())
                .totalExpense(cr.calculateTotalExpense())
                .expectedAmount(cr.calculateExpectedAmount())
                .actualAmount(cr.getActualAmount())
                .difference(cr.getDifference())
                .build();
    }
}
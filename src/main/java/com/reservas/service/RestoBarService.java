// src/main/java/com/reservas/service/RestoBarService.java
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestoBarService {

    private final ProductRepository productRepository;
    private final RestaurantTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final TableSessionItemRepository itemRepository;
    private final TableSessionPaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final RecurringReservationRepository recurringRepository;
    private final ExtraProductRepository extraProductRepository;
    private final CashRegisterService cashRegisterService;

    // ========================================================================
    // VENTA RÁPIDA
    // ========================================================================

    @Transactional
    public QuickSaleResponse createQuickSale(QuickSaleRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod());

        List<QuickSaleItemResponse> itemResponses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder description = new StringBuilder("Venta rápida: ");

        for (QuickSaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto", "id", itemRequest.getProductId()));

            if (!product.getComplex().getId().equals(complexId)) {
                throw new UnauthorizedException("Producto no pertenece al complejo");
            }

            if (!product.getAvailable()) {
                throw new BadRequestException("El producto '" + product.getName() + "' no está disponible");
            }

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(itemTotal);

            itemResponses.add(QuickSaleItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .totalPrice(itemTotal)
                    .build());

            description.append(product.getName())
                    .append(" x")
                    .append(itemRequest.getQuantity())
                    .append(", ");
        }

        // Quitar última coma
        if (description.length() > 2) {
            description.setLength(description.length() - 2);
        }

        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            description.append(" - ").append(request.getCustomerName());
        }

        // Registrar en caja si es efectivo
        Long movementId = null;
        if (method == PaymentMethod.CASH) {
            CashMovementRequest movementRequest = CashMovementRequest.builder()
                    .type(MovementType.INCOME)
                    .category(MovementCategory.PRODUCT_SALE)
                    .description(description.toString())
                    .amount(total)
                    .build();

            CashMovementResponse movement = cashRegisterService.addMovement(movementRequest);
            movementId = movement.getId();
        }

        log.info("💰 Venta rápida: ${} - {} - {}",
                total, method, description);

        return QuickSaleResponse.builder()
                .id(movementId)
                .customerName(request.getCustomerName())
                .paymentMethod(method.name())
                .total(total)
                .items(itemResponses)
                .createdAt(LocalDateTime.now())
                .createdByName(currentUser.getDisplayName())
                .build();
    }

    // ========================================================================
    // SESIONES DE MESA
    // ========================================================================

    // -------------------- ABRIR SESIÓN --------------------

    @Transactional
    public TableSessionResponse openTableSession(OpenTableSessionRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        RestaurantTable table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Mesa", "id", request.getTableId()));

        if (!table.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta mesa");
        }

        if (!table.getActive()) {
            throw new BadRequestException("La mesa no está activa");
        }

        // Verificar que no tenga sesión abierta
        if (sessionRepository.existsByTableIdAndStatus(request.getTableId(), TableSessionStatus.OPEN)) {
            throw new BadRequestException("La mesa ya tiene una sesión abierta");
        }

        TableSession session = TableSession.builder()
                .complex(currentUser.getComplex())
                .table(table)
                .customerName(request.getCustomerName())
                .notes(request.getNotes())
                .status(TableSessionStatus.OPEN)
                .openedBy(currentUser)
                .build();

        session = sessionRepository.save(session);

        log.info("🪑 Sesión abierta: Mesa {} - {}",
                table.getName(),
                request.getCustomerName() != null ? request.getCustomerName() : "Sin nombre");

        return mapSessionToResponse(session);
    }

    // -------------------- OBTENER SESIÓN --------------------

    @Transactional(readOnly = true)
    public TableSessionResponse getSessionById(Long sessionId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión", "id", sessionId));

        if (!session.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta sesión");
        }

        return mapSessionToResponse(session);
    }

    // -------------------- OBTENER SESIONES ABIERTAS --------------------

    @Transactional(readOnly = true)
    public List<TableSessionResponse> getOpenSessions() {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        return sessionRepository
                .findByComplexIdAndStatusOrderByOpenedAtAsc(complexId, TableSessionStatus.OPEN)
                .stream()
                .map(this::mapSessionToResponse)
                .collect(Collectors.toList());
    }

    // -------------------- AGREGAR ITEM --------------------

    @Transactional
    public TableSessionResponse addItemToSession(Long sessionId, AddTableSessionItemRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión", "id", sessionId));

        if (!session.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta sesión");
        }

        if (session.getStatus() != TableSessionStatus.OPEN) {
            throw new BadRequestException("La sesión no está abierta");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", request.getProductId()));

        if (!product.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("Producto no pertenece al complejo");
        }

        if (!product.getAvailable()) {
            throw new BadRequestException("El producto '" + product.getName() + "' no está disponible");
        }

        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        TableSessionItem item = TableSessionItem.builder()
                .session(session)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .addedBy(currentUser)
                .build();

        itemRepository.save(item);
        session.getItems().add(item);

        log.info("➕ Item agregado a sesión #{}: {} x{} = ${}",
                sessionId, product.getName(), request.getQuantity(), totalPrice);

        return mapSessionToResponse(session);
    }

    // -------------------- ELIMINAR ITEM --------------------

    @Transactional
    public TableSessionResponse removeItemFromSession(Long sessionId, Long itemId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión", "id", sessionId));

        if (!session.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta sesión");
        }

        if (session.getStatus() != TableSessionStatus.OPEN) {
            throw new BadRequestException("La sesión no está abierta");
        }

        TableSessionItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        if (!item.getSession().getId().equals(sessionId)) {
            throw new BadRequestException("El item no pertenece a esta sesión");
        }

        session.getItems().removeIf(i -> i.getId().equals(itemId));
        itemRepository.delete(item);

        log.info("➖ Item eliminado de sesión #{}: {}", sessionId, item.getProductName());

        return mapSessionToResponse(session);
    }

    // -------------------- AGREGAR PAGO --------------------

    @Transactional
    public TableSessionResponse addPaymentToSession(Long sessionId, AddTableSessionPaymentRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión", "id", sessionId));

        if (!session.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta sesión");
        }

        if (session.getStatus() != TableSessionStatus.OPEN) {
            throw new BadRequestException("La sesión no está abierta");
        }

        // Verificar que no se pague más de lo pendiente
        BigDecimal pending = session.getTotalPending();
        if (request.getAmount().compareTo(pending) > 0) {
            throw new BadRequestException("El monto excede el pendiente ($" + pending + ")");
        }

        PaymentMethod method = PaymentMethod.valueOf(request.getMethod());

        TableSessionPayment payment = TableSessionPayment.builder()
                .session(session)
                .amount(request.getAmount())
                .method(method)
                .payerName(request.getPayerName())
                .receivedBy(currentUser)
                .build();

        payment = paymentRepository.save(payment);
        session.getPayments().add(payment);

        // Registrar en caja si es efectivo
        if (method == PaymentMethod.CASH) {
            String description = String.format("Mesa %s - %s",
                    session.getTable().getName(),
                    request.getPayerName() != null ? request.getPayerName() : session.getCustomerName());

            CashMovementRequest movementRequest = CashMovementRequest.builder()
                    .type(MovementType.INCOME)
                    .category(MovementCategory.PRODUCT_SALE)
                    .description(description)
                    .amount(request.getAmount())
                    .build();

            cashRegisterService.addMovement(movementRequest);
        }

        log.info("💵 Pago registrado en sesión #{}: ${} - {}",
                sessionId, request.getAmount(), method);

        // Si está completamente pagada, cerrar automáticamente
        session = sessionRepository.findById(sessionId).orElse(session);
        if (session.isFullyPaid()) {
            return closeSession(sessionId);
        }

        return mapSessionToResponse(session);
    }

    // -------------------- CERRAR SESIÓN --------------------

    @Transactional
    public TableSessionResponse closeSession(Long sessionId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión", "id", sessionId));

        if (!session.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta sesión");
        }

        if (session.getStatus() != TableSessionStatus.OPEN) {
            throw new BadRequestException("La sesión no está abierta");
        }

        if (!session.isFullyPaid()) {
            throw new BadRequestException("La sesión tiene un pendiente de $" + session.getTotalPending());
        }

        session.setStatus(TableSessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        session.setClosedBy(currentUser);

        session = sessionRepository.save(session);

        log.info("✅ Sesión #{} cerrada - Mesa {} - Total: ${}",
                sessionId, session.getTable().getName(), session.getTotal());

        return mapSessionToResponse(session);
    }

    // -------------------- CANCELAR SESIÓN --------------------

    @Transactional
    public TableSessionResponse cancelSession(Long sessionId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        TableSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión", "id", sessionId));

        if (!session.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta sesión");
        }

        if (session.getStatus() != TableSessionStatus.OPEN) {
            throw new BadRequestException("La sesión no está abierta");
        }

        // Si tiene pagos, no se puede cancelar fácilmente
        if (!session.getPayments().isEmpty()) {
            throw new BadRequestException(
                    "No se puede cancelar una sesión con pagos registrados. " +
                            "Pendiente: $" + session.getTotalPending());
        }

        session.setStatus(TableSessionStatus.CANCELLED);
        session.setClosedAt(LocalDateTime.now());
        session.setClosedBy(currentUser);

        session = sessionRepository.save(session);

        log.info("❌ Sesión #{} cancelada - Mesa {}", sessionId, session.getTable().getName());

        return mapSessionToResponse(session);
    }

    // ========================================================================
    // VENTA A CANCHA (usa servicios existentes)
    // ========================================================================

    @Transactional
    public ExtraProductResponse addProductToCourt(AddProductToCourtRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        // Obtener producto
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", request.getProductId()));

        if (!product.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("Producto no pertenece al complejo");
        }

        if (!product.getAvailable()) {
            throw new BadRequestException("El producto '" + product.getName() + "' no está disponible");
        }

        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // Determinar si es reserva normal o recurrente
        if (request.getReservationId() != null) {
            // Reserva normal
            Reservation reservation = reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Reserva", "id", request.getReservationId()));

            if (!reservation.getCourt().getComplex().getId().equals(complexId)) {
                throw new UnauthorizedException("La reserva no pertenece al complejo");
            }

            ExtraProduct extraProduct = ExtraProduct.builder()
                    .reservation(reservation)
                    .name(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(request.getQuantity())
                    .totalPrice(totalPrice)
                    .paid(false)
                    .addedAt(LocalDateTime.now())
                    .build();

            extraProduct = extraProductRepository.save(extraProduct);

            log.info("🛒 Producto agregado a reserva #{}: {} x{} = ${}",
                    reservation.getId(), product.getName(), request.getQuantity(), totalPrice);

            return mapExtraProductToResponse(extraProduct);

        } else if (request.getRecurringReservationId() != null && request.getDate() != null) {
            // Reserva recurrente
            RecurringReservation recurring = recurringRepository.findById(request.getRecurringReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Reserva fija", "id", request.getRecurringReservationId()));

            if (!recurring.getCourt().getComplex().getId().equals(complexId)) {
                throw new UnauthorizedException("La reserva fija no pertenece al complejo");
            }

            // Verificar que la fecha corresponda al día
            if (request.getDate().getDayOfWeek() != recurring.getDayOfWeek()) {
                throw new BadRequestException("La fecha no corresponde al día de la reserva fija");
            }

            ExtraProduct extraProduct = ExtraProduct.builder()
                    .recurringReservation(recurring)
                    .productDate(request.getDate())
                    .name(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(request.getQuantity())
                    .totalPrice(totalPrice)
                    .paid(false)
                    .addedAt(LocalDateTime.now())
                    .build();

            extraProduct = extraProductRepository.save(extraProduct);

            log.info("🛒 Producto agregado a reserva fija #{} ({}): {} x{} = ${}",
                    recurring.getId(),
                    request.getDate().format(DateTimeFormatter.ofPattern("dd/MM")),
                    product.getName(),
                    request.getQuantity(),
                    totalPrice);

            return mapExtraProductToResponse(extraProduct);

        } else {
            throw new BadRequestException(
                    "Debe indicar reservationId o (recurringReservationId + date)");
        }
    }

    // ========================================================================
    // HISTORIAL
    // ========================================================================

    @Transactional(readOnly = true)
    public List<TableSessionResponse> getSessionHistory(LocalDate date) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        return sessionRepository
                .findByComplexIdAndStatusAndClosedAtBetween(
                        complexId, TableSessionStatus.CLOSED, startOfDay, endOfDay)
                .stream()
                .map(this::mapSessionToResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

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

    // ========================================================================
    // MAPPERS
    // ========================================================================

    private TableSessionResponse mapSessionToResponse(TableSession session) {
        List<TableSessionItemResponse> items = session.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        List<TableSessionPaymentResponse> payments = session.getPayments().stream()
                .map(this::mapPaymentToResponse)
                .collect(Collectors.toList());

        return TableSessionResponse.builder()
                .id(session.getId())
                .tableId(session.getTable().getId())
                .tableName(session.getTable().getName())
                .customerName(session.getCustomerName())
                .status(session.getStatus())
                .openedAt(session.getOpenedAt())
                .openedByName(session.getOpenedBy() != null ? session.getOpenedBy().getDisplayName() : null)
                .closedAt(session.getClosedAt())
                .closedByName(session.getClosedBy() != null ? session.getClosedBy().getDisplayName() : null)
                .notes(session.getNotes())
                .total(session.getTotal())
                .totalPaid(session.getTotalPaid())
                .totalPending(session.getTotalPending())
                .fullyPaid(session.isFullyPaid())
                .items(items)
                .payments(payments)
                .build();
    }

    private TableSessionItemResponse mapItemToResponse(TableSessionItem item) {
        return TableSessionItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .addedAt(item.getAddedAt())
                .addedByName(item.getAddedBy() != null ? item.getAddedBy().getDisplayName() : null)
                .build();
    }

    private TableSessionPaymentResponse mapPaymentToResponse(TableSessionPayment payment) {
        return TableSessionPaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .payerName(payment.getPayerName())
                .paidAt(payment.getPaidAt())
                .receivedByName(payment.getReceivedBy() != null ? payment.getReceivedBy().getDisplayName() : null)
                .build();
    }

    private ExtraProductResponse mapExtraProductToResponse(ExtraProduct product) {
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
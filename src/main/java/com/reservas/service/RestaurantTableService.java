// src/main/java/com/reservas/service/RestaurantTableService.java
package com.reservas.service;

import com.reservas.dto.request.RestaurantTableRequest;
import com.reservas.dto.response.RestaurantTableResponse;
import com.reservas.entity.RestaurantTable;
import com.reservas.entity.TableSession;
import com.reservas.entity.User;
import com.reservas.enums.Role;
import com.reservas.enums.TableSessionStatus;
import com.reservas.exception.BadRequestException;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.exception.UnauthorizedException;
import com.reservas.repository.RestaurantTableRepository;
import com.reservas.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;

    // ==================== CREAR MESA ====================

    @Transactional
    public RestaurantTableResponse createTable(RestaurantTableRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        // Verificar nombre duplicado
        if (tableRepository.existsByComplexIdAndNameIgnoreCase(complexId, request.getName())) {
            throw new BadRequestException("Ya existe una mesa con ese nombre");
        }

        RestaurantTable table = RestaurantTable.builder()
                .complex(currentUser.getComplex())
                .name(request.getName())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        table = tableRepository.save(table);

        log.info("✅ Mesa creada: {}", table.getName());

        return mapToResponse(table, null);
    }

    // ==================== ACTUALIZAR MESA ====================

    @Transactional
    public RestaurantTableResponse updateTable(Long tableId, RestaurantTableRequest request) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa", "id", tableId));

        if (!table.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta mesa");
        }

        // Verificar nombre duplicado
        if (tableRepository.existsByComplexIdAndNameIgnoreCaseAndIdNot(
                complexId, request.getName(), tableId)) {
            throw new BadRequestException("Ya existe otra mesa con ese nombre");
        }

        table.setName(request.getName());
        if (request.getDisplayOrder() != null) {
            table.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            table.setActive(request.getActive());
        }

        table = tableRepository.save(table);

        log.info("✏️ Mesa actualizada: {}", table.getName());

        // Obtener sesión abierta si existe
        Optional<TableSession> openSession = sessionRepository
                .findByTableIdAndStatus(tableId, TableSessionStatus.OPEN);

        return mapToResponse(table, openSession.orElse(null));
    }

    // ==================== ELIMINAR MESA ====================

    @Transactional
    public void deleteTable(Long tableId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa", "id", tableId));

        if (!table.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta mesa");
        }

        // Verificar que no tenga sesión abierta
        if (sessionRepository.existsByTableIdAndStatus(tableId, TableSessionStatus.OPEN)) {
            throw new BadRequestException("No se puede eliminar una mesa con sesión abierta");
        }

        // Desactivar en lugar de eliminar
        table.setActive(false);
        tableRepository.save(table);

        log.info("🗑️ Mesa desactivada: {}", table.getName());
    }

    // ==================== LISTAR MESAS ====================

    @Transactional(readOnly = true)
    public List<RestaurantTableResponse> getAllTables() {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        List<RestaurantTable> tables = tableRepository
                .findByComplexIdOrderByDisplayOrderAscNameAsc(complexId);

        return tables.stream()
                .map(table -> {
                    Optional<TableSession> openSession = sessionRepository
                            .findByTableIdAndStatus(table.getId(), TableSessionStatus.OPEN);
                    return mapToResponse(table, openSession.orElse(null));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RestaurantTableResponse> getActiveTables() {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        List<RestaurantTable> tables = tableRepository
                .findByComplexIdAndActiveTrueOrderByDisplayOrderAscNameAsc(complexId);

        return tables.stream()
                .map(table -> {
                    Optional<TableSession> openSession = sessionRepository
                            .findByTableIdAndStatus(table.getId(), TableSessionStatus.OPEN);
                    return mapToResponse(table, openSession.orElse(null));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantTableResponse getTableById(Long tableId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa", "id", tableId));

        if (!table.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta mesa");
        }

        Optional<TableSession> openSession = sessionRepository
                .findByTableIdAndStatus(tableId, TableSessionStatus.OPEN);

        return mapToResponse(table, openSession.orElse(null));
    }

    // ==================== TOGGLE ACTIVO ====================

    @Transactional
    public RestaurantTableResponse toggleActive(Long tableId) {
        User currentUser = getCurrentUser();
        Long complexId = getComplexId(currentUser);

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa", "id", tableId));

        if (!table.getComplex().getId().equals(complexId)) {
            throw new UnauthorizedException("No tenés acceso a esta mesa");
        }

        // Si tiene sesión abierta, no se puede desactivar
        if (table.getActive() && sessionRepository.existsByTableIdAndStatus(tableId, TableSessionStatus.OPEN)) {
            throw new BadRequestException("No se puede desactivar una mesa con sesión abierta");
        }

        table.setActive(!table.getActive());
        table = tableRepository.save(table);

        log.info("🔄 Mesa {} ahora está {}",
                table.getName(),
                table.getActive() ? "activa" : "inactiva");

        return mapToResponse(table, null);
    }

    // ==================== HELPERS ====================

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

    private RestaurantTableResponse mapToResponse(RestaurantTable table, TableSession openSession) {
        return RestaurantTableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .displayOrder(table.getDisplayOrder())
                .active(table.getActive())
                .createdAt(table.getCreatedAt())
                .hasOpenSession(openSession != null)
                .openSessionId(openSession != null ? openSession.getId() : null)
                .openSessionCustomerName(openSession != null ? openSession.getCustomerName() : null)
                .openSessionStartedAt(openSession != null ? openSession.getOpenedAt() : null)
                .build();
    }
}
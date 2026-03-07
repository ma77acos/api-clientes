// src/main/java/com/reservas/controller/RestaurantTableController.java
package com.reservas.controller;

import com.reservas.dto.request.RestaurantTableRequest;
import com.reservas.dto.response.RestaurantTableResponse;
import com.reservas.service.RestaurantTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tables")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
public class RestaurantTableController {

    private final RestaurantTableService tableService;

    /**
     * Crear mesa
     */
    @PostMapping
    public ResponseEntity<RestaurantTableResponse> createTable(
            @Valid @RequestBody RestaurantTableRequest request) {
        RestaurantTableResponse response = tableService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualizar mesa
     */
    @PutMapping("/{id}")
    public ResponseEntity<RestaurantTableResponse> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantTableRequest request) {
        RestaurantTableResponse response = tableService.updateTable(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Eliminar (desactivar) mesa
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        tableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener todas las mesas (incluye inactivas)
     */
    @GetMapping
    public ResponseEntity<List<RestaurantTableResponse>> getAllTables() {
        List<RestaurantTableResponse> tables = tableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * Obtener solo mesas activas
     */
    @GetMapping("/active")
    public ResponseEntity<List<RestaurantTableResponse>> getActiveTables() {
        List<RestaurantTableResponse> tables = tableService.getActiveTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * Obtener mesa por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantTableResponse> getTableById(@PathVariable Long id) {
        RestaurantTableResponse table = tableService.getTableById(id);
        return ResponseEntity.ok(table);
    }

    /**
     * Toggle activo
     */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<RestaurantTableResponse> toggleActive(@PathVariable Long id) {
        RestaurantTableResponse response = tableService.toggleActive(id);
        return ResponseEntity.ok(response);
    }
}
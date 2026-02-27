// src/main/java/com/reservas/controller/AdminCourtController.java
package com.reservas.controller;

import com.reservas.dto.request.CourtRequest;
import com.reservas.dto.response.CourtResponse;
import com.reservas.service.AdminCourtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/courts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
public class AdminCourtController {

    private final AdminCourtService courtService;

    @GetMapping
    public ResponseEntity<List<CourtResponse>> getCourts() {
        return ResponseEntity.ok(courtService.getComplexCourts());
    }

    @PostMapping
    public ResponseEntity<CourtResponse> createCourt(@Valid @RequestBody CourtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courtService.createCourt(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable Long id,
            @Valid @RequestBody CourtRequest request) {
        return ResponseEntity.ok(courtService.updateCourt(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourt(@PathVariable Long id) {
        courtService.deleteCourt(id);
        return ResponseEntity.noContent().build();
    }
}
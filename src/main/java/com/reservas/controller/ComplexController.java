// src/main/java/com/reservas/controller/ComplexController.java
package com.reservas.controller;

import com.reservas.dto.response.ComplexDetailResponse;
import com.reservas.dto.response.ComplexResponse;
import com.reservas.dto.response.CourtResponse;
import com.reservas.service.ComplexService;
import com.reservas.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/complexes")
@RequiredArgsConstructor
public class ComplexController {

    private final ComplexService complexService;
    private final CourtService courtService;

    @GetMapping
    public ResponseEntity<List<ComplexResponse>> getAllComplexes(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search) {
        List<ComplexResponse> response = complexService.getAllComplexes(city, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplexDetailResponse> getComplexById(@PathVariable Long id) {
        ComplexDetailResponse response = complexService.getComplexById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/courts")
    public ResponseEntity<List<CourtResponse>> getCourtsByComplexId(@PathVariable Long id) {
        List<CourtResponse> response = courtService.getCourtsByComplexId(id);
        return ResponseEntity.ok(response);
    }

    // Complejos destacados (público)
    @GetMapping("/featured")
    public ResponseEntity<List<ComplexResponse>> getFeaturedComplexes() {
        return ResponseEntity.ok(complexService.getFeaturedComplexes());
    }
}
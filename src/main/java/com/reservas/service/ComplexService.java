// src/main/java/com/reservas/service/ComplexService.java
package com.reservas.service;

import com.reservas.dto.response.ComplexDetailResponse;
import com.reservas.dto.response.ComplexResponse;
import com.reservas.entity.Complex;
import com.reservas.enums.ReservationStatus;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.ComplexRepository;
import com.reservas.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplexService {

    private final ComplexRepository complexRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<ComplexResponse> getAllComplexes(String city, String search) {
        List<Complex> complexes = complexRepository.findByFilters(city, search);
        return complexes.stream()
                .map(this::mapToComplexResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplexDetailResponse getComplexById(Long id) {
        Complex complex = complexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complejo", "id", id));
        return mapToComplexDetailResponse(complex);
    }

    private ComplexResponse mapToComplexResponse(Complex complex) {
        // Calcular ocupación del día
        int occupationToday = calculateOccupationToday(complex);

        return ComplexResponse.builder()
                .id(complex.getId())
                .name(complex.getName())
                .city(complex.getCity())
                .rating(complex.getRating())
                .priceFrom(complex.getMinPrice())
                .coveredCourts(complex.hasCoveredCourts())
                .occupationToday(occupationToday)
                .imageUrl(complex.getImageUrl())
                .build();
    }

    private ComplexDetailResponse mapToComplexDetailResponse(Complex complex) {
        return ComplexDetailResponse.builder()
                .id(complex.getId())
                .name(complex.getName())
                .city(complex.getCity())
                .description(complex.getDescription())
                .rating(complex.getRating())
                .address(complex.getAddress())
                .phone(complex.getPhone())
                .imageUrl(complex.getImageUrl())
                .build();
    }

    private int calculateOccupationToday(Complex complex) {
        LocalDate today = LocalDate.now();
        Long bookedCount = reservationRepository.countByComplexIdAndDateAndStatus(
                complex.getId(), today, ReservationStatus.CONFIRMED);

        // Asumiendo 12 slots por cancha (8am a 8pm, cada hora)
        int totalSlots = complex.getCourts().size() * 12;

        if (totalSlots == 0) return 0;

        return (int) ((bookedCount * 100) / totalSlots);
    }

    public List<ComplexResponse> getFeaturedComplexes() {
        // Traer los primeros 6 complejos activos, ordenados por rating
        List<Complex> complexes = complexRepository.findTop6ByActiveOrderByRatingDesc(true);

        return complexes.stream()
                .map(this::mapToComplexResponse)
                .collect(Collectors.toList());
    }
}
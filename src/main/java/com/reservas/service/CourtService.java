// src/main/java/com/reservas/service/CourtService.java
package com.reservas.service;

import com.reservas.dto.response.CourtResponse;
import com.reservas.entity.Court;
import com.reservas.exception.ResourceNotFoundException;
import com.reservas.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public List<CourtResponse> getCourtsByComplexId(Long complexId) {
        List<Court> courts = courtRepository.findByComplexId(complexId);
        return courts.stream()
                .map(this::mapToCourtResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Court getCourtById(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancha", "id", id));
    }

    private CourtResponse mapToCourtResponse(Court court) {
        return CourtResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .covered(court.getCovered())
                .surface(court.getSurface())
                .price(court.getPrice())
                .imageUrl(court.getImageUrl())
                .build();
    }
}
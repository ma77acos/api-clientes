// src/main/java/com/reservas/repository/AvailabilitySlotRepository.java
package com.reservas.repository;

import com.reservas.entity.AvailabilitySlot;
import com.reservas.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByCourtIdAndDate(Long courtId, LocalDate date);

    Optional<AvailabilitySlot> findByCourtIdAndDateAndTime(Long courtId, LocalDate date, LocalTime time);

    List<AvailabilitySlot> findByCourtIdAndDateAndStatus(Long courtId, LocalDate date, SlotStatus status);
}
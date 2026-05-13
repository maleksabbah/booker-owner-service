package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Schedule view for a single court within a window
    List<Booking> findByCourt_IdAndSlotStartBetweenOrderBySlotStartAsc(
            Long courtId, LocalDateTime from, LocalDateTime to);

    // Schedule view across all courts in a facility, grouped by court then time
    @Query("""
            SELECT b FROM Booking b
            WHERE b.court.facility.id = :facilityId
            ORDER BY b.court.id ASC, b.slotStart ASC
            """)
    List<Booking> findByFacilityOrderedByCourtAndTime(@Param("facilityId") Long facilityId);

    Optional<Booking> findById(Long id);

    // Atomic state transition (compare-and-swap)
    @Modifying
    @Query("""
            UPDATE Booking b
            SET b.state = :newState,
                b.seatedByUserId    = CASE WHEN :newState = 'SEATED'    THEN :actorId ELSE b.seatedByUserId    END,
                b.completedByUserId = CASE WHEN :newState = 'COMPLETED' THEN :actorId ELSE b.completedByUserId END,
                b.noShowByUserId    = CASE WHEN :newState = 'NO_SHOW'   THEN :actorId ELSE b.noShowByUserId    END,
                b.updatedAt = CURRENT_TIMESTAMP
            WHERE b.id = :bookingId AND b.state = :expectedState
            """)
    int transitionState(
            @Param("bookingId") Long bookingId,
            @Param("expectedState") String expectedState,
            @Param("newState") String newState,
            @Param("actorId") Long actorId);
}
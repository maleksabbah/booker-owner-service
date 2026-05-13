package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.CourtBlackout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CourtBlackoutRepository extends JpaRepository<CourtBlackout, Long> {

    List<CourtBlackout> findByCourt_IdOrderByStartsAtAsc(Long courtId);

    // For schedule view: blackouts in a window
    List<CourtBlackout> findByCourt_IdAndStartsAtBeforeAndEndsAtAfter(
            Long courtId, LocalDateTime windowEnd, LocalDateTime windowStart);
}
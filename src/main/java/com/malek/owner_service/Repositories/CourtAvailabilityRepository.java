package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.CourtAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourtAvailabilityRepository extends JpaRepository<CourtAvailability, Long> {

    List<CourtAvailability> findByCourt_IdOrderByDayOfWeekAscOpensAtAsc(Long courtId);

    @Modifying
    @Query("DELETE FROM CourtAvailability ca WHERE ca.court.id = :courtId")
    void deleteAllByCourtId(Long courtId);
}
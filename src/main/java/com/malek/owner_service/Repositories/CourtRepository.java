package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.Court;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourtRepository extends JpaRepository<Court, Long> {

    List<Court> findByFacility_IdAndActiveTrue(Long facilityId);

    Optional<Court> findByIdAndActiveTrue(Long id);
}

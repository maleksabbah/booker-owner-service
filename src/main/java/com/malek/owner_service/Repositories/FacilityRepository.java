package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    // Note: "owned by me" via primary owner column. The full membership-aware
    // check (any role at this facility) is done via FacilityMemberRepository.
    List<Facility> findByOwnerUserIdAndActiveTrue(Long ownerUserId);

    Optional<Facility> findByIdAndActiveTrue(Long id);
}

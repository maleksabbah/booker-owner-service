package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.FacilityMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityMemberRepository
        extends JpaRepository<FacilityMember, FacilityMember.FacilityMemberId> {

    Optional<FacilityMember> findByFacility_IdAndUser_Id(Long facilityId, Long userId);

    List<FacilityMember> findByFacility_IdOrderByAddedAtAsc(Long facilityId);

    List<FacilityMember> findByUser_Id(Long userId);

    long countByFacility_IdAndRole(Long facilityId, String role);
}

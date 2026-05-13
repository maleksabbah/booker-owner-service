package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.FacilityInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FacilityInvitationRepository extends JpaRepository<FacilityInvitation, Long> {

    Optional<FacilityInvitation> findByToken(String token);

    // List pending invites for a facility (not accepted, not revoked, not expired)
    List<FacilityInvitation> findByFacility_IdAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            Long facilityId, Instant now);

    // List pending invites for an email (used by user-service when accepting)
    List<FacilityInvitation> findByEmailAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
            String email, Instant now);
}
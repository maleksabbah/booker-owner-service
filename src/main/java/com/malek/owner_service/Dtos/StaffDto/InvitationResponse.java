package com.malek.owner_service.Dtos.StaffDto;

import java.time.Instant;

public record InvitationResponse(
        Long id,
        Long facilityId,
        String email,
        String role,
        String token,
        Long invitedByUserId,
        Instant expiresAt,
        Instant acceptedAt,
        Instant revokedAt,
        Instant createdAt,
        String status   // 'PENDING' | 'ACCEPTED' | 'REVOKED' | 'EXPIRED' (computed)
) {}
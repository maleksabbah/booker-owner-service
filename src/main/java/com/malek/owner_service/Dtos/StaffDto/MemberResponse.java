package com.malek.owner_service.Dtos.StaffDto;

import java.time.Instant;

public record MemberResponse(
        Long facilityId,
        Long userId,
        String userEmail,
        String userDisplayName,
        String role,
        Long addedByUserId,
        Instant addedAt
) {}
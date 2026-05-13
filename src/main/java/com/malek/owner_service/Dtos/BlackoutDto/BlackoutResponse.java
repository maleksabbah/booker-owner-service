package com.malek.owner_service.Dtos.BlackoutDto;

import java.time.Instant;
import java.time.LocalDateTime;

public record BlackoutResponse(
        Long id,
        Long courtId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String reason,
        Instant createdAt
) {}
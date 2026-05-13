package com.malek.owner_service.Dtos.OwnerDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerBookingResponse (
        Long id,
        Long courtId,
        String courtName,
        Long creatorUserId,
        LocalDateTime slotStart,
        LocalDateTime slotEnd,
        String visibility,
        String state,
        Integer slotsTotal,
        Integer slotsFilled,
        Integer minPlayers,
        String description,
        BigDecimal priceTotal,
        LocalDateTime holdExpiresAt,
        List<Long> participantUserIds,
        Long seatedByUserId,
        Long completedByUserId,
        Long noShowByUserId,
        Instant createdAt,
        Instant updatedAt

) {}





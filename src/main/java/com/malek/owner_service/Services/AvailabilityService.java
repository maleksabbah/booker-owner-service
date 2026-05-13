package com.malek.owner_service.Services;

import com.malek.owner_service.Dtos.AvailabilityDto.AvailabilityResponse;
import com.malek.owner_service.Dtos.AvailabilityDto.AvailabilitySlot;
import com.malek.owner_service.Dtos.AvailabilityDto.SetAvailabilityRequest;
import com.malek.owner_service.Entities.Court;
import com.malek.owner_service.Entities.CourtAvailability;
import com.malek.owner_service.Repositories.CourtAvailabilityRepository;
import com.malek.owner_service.Repositories.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final CourtAvailabilityRepository availabilityRepository;
    private final CourtRepository courtRepository;
    private final FacilityService facilityService;

    public List<AvailabilityResponse> getSchedule(Long courtId, Long callerUserId) {
        Court court = loadCourt(courtId);
        facilityService.requireRole(callerUserId, court.getFacility().getId(), "STAFF");

        return availabilityRepository
                .findByCourt_IdOrderByDayOfWeekAscOpensAtAsc(courtId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Replace the court's whole weekly schedule atomically.
     * Owner sends the full week; we delete what's there and insert the new set.
     */
    @Transactional
    public List<AvailabilityResponse> replaceSchedule(
            Long courtId, SetAvailabilityRequest req, Long callerUserId) {
        Court court = loadCourt(courtId);
        facilityService.requireRole(callerUserId, court.getFacility().getId(), "MANAGER");

        // Validate each slot's times before touching the DB
        for (AvailabilitySlot slot : req.slots()) {
            if (!slot.opensAt().isBefore(slot.closesAt())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "opensAt must be before closesAt for day " + slot.dayOfWeek());
            }
        }

        availabilityRepository.deleteAllByCourtId(courtId);

        List<CourtAvailability> rows = new ArrayList<>();
        for (AvailabilitySlot slot : req.slots()) {
            rows.add(CourtAvailability.builder()
                    .court(court)
                    .dayOfWeek(slot.dayOfWeek())
                    .opensAt(slot.opensAt())
                    .closesAt(slot.closesAt())
                    .build());
        }
        List<CourtAvailability> saved = availabilityRepository.saveAll(rows);

        return saved.stream().map(this::toResponse).toList();
    }

    // ─── internals ──────────────────────────────────────────────────

    private Court loadCourt(Long id) {
        return courtRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
    }

    private AvailabilityResponse toResponse(CourtAvailability ca) {
        return new AvailabilityResponse(
                ca.getId(),
                ca.getCourt().getId(),
                ca.getDayOfWeek(),
                ca.getOpensAt(),
                ca.getClosesAt()
        );
    }
}

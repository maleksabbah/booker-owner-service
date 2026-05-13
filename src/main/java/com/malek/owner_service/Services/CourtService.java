package com.malek.owner_service.Services;

import com.malek.owner_service.Dtos.CourtDto.CourtRequest;
import com.malek.owner_service.Dtos.CourtDto.CourtResponse;
import com.malek.owner_service.Entities.Court;
import com.malek.owner_service.Entities.Facility;
import com.malek.owner_service.Repositories.CourtRepository;
import com.malek.owner_service.Repositories.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityService facilityService;   // for requireRole(...)

    // ─── CRUD ────────────────────────────────────────────────────────

    @Transactional
    public CourtResponse createCourt(Long facilityId, CourtRequest req, Long callerUserId) {
        facilityService.requireRole(callerUserId, facilityId, "MANAGER");
        Facility facility = facilityRepository.findByIdAndActiveTrue(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));

        Court court = Court.builder()
                .facility(facility)
                .name(req.name())
                .sport(req.sport())
                .surface(req.surface())
                .capacity(req.capacity())
                .pricePerHour(req.pricePerHour())
                .currency(req.currency())
                .active(true)
                .build();
        return toResponse(courtRepository.save(court));
    }

    public CourtResponse getCourt(Long courtId, Long callerUserId) {
        Court court = loadActive(courtId);
        facilityService.requireRole(callerUserId, court.getFacility().getId(), "STAFF");
        return toResponse(court);
    }

    public List<CourtResponse> listCourtsAtFacility(Long facilityId, Long callerUserId) {
        facilityService.requireRole(callerUserId, facilityId, "STAFF");
        return courtRepository.findByFacility_IdAndActiveTrue(facilityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CourtResponse updateCourt(Long courtId, CourtRequest req, Long callerUserId) {
        Court court = loadActive(courtId);
        facilityService.requireRole(callerUserId, court.getFacility().getId(), "MANAGER");

        // PATCH semantics — only overwrite fields that were provided
        if (req.name()         != null) court.setName(req.name());
        if (req.sport()        != null) court.setSport(req.sport());
        if (req.surface()      != null) court.setSurface(req.surface());
        if (req.capacity()     != null) court.setCapacity(req.capacity());
        if (req.pricePerHour() != null) court.setPricePerHour(req.pricePerHour());
        if (req.currency()     != null) court.setCurrency(req.currency());

        return toResponse(courtRepository.save(court));
    }

    @Transactional
    public void softDeleteCourt(Long courtId, Long callerUserId) {
        Court court = loadActive(courtId);
        facilityService.requireRole(callerUserId, court.getFacility().getId(), "MANAGER");
        court.setActive(false);
        courtRepository.save(court);
    }

    // ─── Internals ──────────────────────────────────────────────────

    private Court loadActive(Long id) {
        return courtRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
    }

    private CourtResponse toResponse(Court c) {
        return new CourtResponse(
                c.getId(),
                c.getFacility().getId(),
                c.getName(),
                c.getSport(),
                c.getSurface(),
                c.getCapacity(),
                c.getPricePerHour(),
                c.getCurrency(),
                c.getActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}

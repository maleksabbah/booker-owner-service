package com.malek.owner_service.Services;

import com.malek.owner_service.Dtos.BlackoutDto.BlackoutRequest;
import com.malek.owner_service.Dtos.BlackoutDto.BlackoutResponse;
import com.malek.owner_service.Entities.Court;
import com.malek.owner_service.Entities.CourtBlackout;
import com.malek.owner_service.Repositories.CourtBlackoutRepository;
import com.malek.owner_service.Repositories.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Service
@RequiredArgsConstructor
public class BlackoutService {
    private final CourtBlackoutRepository blackoutRepository;
    private final CourtRepository courtRepository;
    private final FacilityService facilityService;

    @Transactional
    public BlackoutResponse addBlackout(Long courtId, BlackoutRequest req, Long callerUserId) {
        Court court = loadCourt(courtId);
        facilityService.requireRole(callerUserId, court.getFacility().getId(), "MANAGER");

        if (!req.startsAt().isBefore(req.endsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startsAt must be before endsAt");
        }
        CourtBlackout blackout = CourtBlackout.builder()
                .court(court)
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .reason(req.reason())
                .build();
        return toResponse(blackoutRepository.save(blackout));
    }

    public List<BlackoutResponse> listBlackouts(Long courtId, Long callerUserId) {
        Court court = loadCourt(courtId);
        facilityService.requireRole(callerUserId, court.getFacility().getId(), "STAFF");
        return blackoutRepository.findByCourt_IdOrderByStartsAtAsc(courtId)
                .stream()
                .map(this::toResponse)
                .toList();


    }

    @Transactional
    public void deleteBlackout(Long blackoutId, Long callerUserId) {
        CourtBlackout blackout = blackoutRepository.findById(blackoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blackout not found"));
        facilityService.requireRole(
                callerUserId,
                blackout.getCourt().getFacility().getId(),
                "MANAGER");
        blackoutRepository.delete(blackout);

    }

    // ─── internals ──────────────────────────────────────────────────
    private Court loadCourt(Long id) {
        return courtRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
    }

    private BlackoutResponse toResponse(CourtBlackout b) {
        return new BlackoutResponse(
                b.getId(),
                b.getCourt().getId(),
                b.getStartsAt(),
                b.getEndsAt(),
                b.getReason(),
                b.getCreatedAt()

        );
    }
}




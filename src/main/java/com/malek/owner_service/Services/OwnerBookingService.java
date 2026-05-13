package com.malek.owner_service.Services;

import com.malek.owner_service.Dtos.OwnerDto.OwnerBookingResponse;
import com.malek.owner_service.Entities.Booking;
import com.malek.owner_service.Entities.BookingParticipant;
import com.malek.owner_service.Entities.Facility;
import com.malek.owner_service.Repositories.BookingParticipantRepository;
import com.malek.owner_service.Repositories.BookingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerBookingService {
    @PersistenceContext
    private EntityManager entityManager;
    private final BookingRepository bookingRepository;
    private final BookingParticipantRepository participantRepository;
    private final FacilityService facilityService;
    private final IdempotencyService idempotencyService;

    // ─── Read views ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OwnerBookingResponse> getFacilitySchedule(Long facilityId,Long callerUserId) {
        facilityService.requireRole(callerUserId,facilityId,"STAFF");
        return bookingRepository.findByFacilityOrderedByCourtAndTime(facilityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    public OwnerBookingResponse getBooking(Long bookingId,Long callerUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found"));
        facilityService.requireRole(
                callerUserId,
                booking.getCourt().getFacility().getId(),
                "STAFF"
        );
        return toResponse(booking);
    }

    // ─── State transitions ──────────────────────────────────────────

    public OwnerBookingResponse markSeated(Long bookingId, String idempotencyKey, Long callerUserId) {
        return transition(bookingId, idempotencyKey, callerUserId,
                "CONFIRMED", "SEATED", "/bookings/seat");
    }

    public OwnerBookingResponse markCompleted(Long bookingId, String idempotencyKey, Long callerUserId) {
        return transition(bookingId, idempotencyKey, callerUserId,
                "SEATED", "COMPLETED", "/bookings/complete");
    }

    public OwnerBookingResponse markNoShow(Long bookingId, String idempotencyKey, Long callerUserId) {
        return transition(bookingId, idempotencyKey, callerUserId,
                "CONFIRMED", "NO_SHOW", "/bookings/no-show");
    }

    /**
     * Shared state-transition flow for SEATED / COMPLETED / NO_SHOW.
     *  1. Idempotency wrap (replay if duplicate)
     *  2. Permission check (STAFF+ at the facility)
     *  3. Compare-and-swap UPDATE (atomic)
     *  4. Reload + return
     */
    @Transactional
    protected OwnerBookingResponse transition(
            Long bookingId, String idempotencyKey, Long callerUserId,
            String expectedState, String newState, String endpoint) {

        return idempotencyService.executeIdempotent(
                idempotencyKey, callerUserId, endpoint, bookingId,
                OwnerBookingResponse.class,
                () -> {
                    Booking booking = bookingRepository.findById(bookingId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

                    facilityService.requireRole(
                            callerUserId,
                            booking.getCourt().getFacility().getId(),
                            "STAFF");

                    int rows = bookingRepository.transitionState(
                            bookingId, expectedState, newState, callerUserId);

                    if (rows == 0) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Booking is not in " + expectedState + " state — cannot transition to " + newState);
                    }

                    entityManager.flush();
                    entityManager.clear();

                    Booking updated = bookingRepository.findById(bookingId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Booking vanished after update"));
                    return toResponse(updated);
                });
    }

    // ─── Mapping ────────────────────────────────────────────────────

    private OwnerBookingResponse toResponse(Booking b) {
        List<Long> participantIds = participantRepository.findByBooking_Id(b.getId()).stream()
                .map(BookingParticipant::getUserId)
                .toList();

        return new OwnerBookingResponse(
                b.getId(),
                b.getCourt().getId(),
                b.getCourt().getName(),
                b.getCreatorUserId(),
                b.getSlotStart(),
                b.getSlotEnd(),
                b.getVisibility(),
                b.getState(),
                b.getSlotsTotal(),
                b.getSlotsFilled(),
                b.getMinPlayers(),
                b.getDescription(),
                b.getPriceTotal(),
                b.getHoldExpiresAt(),
                participantIds,
                b.getSeatedByUserId(),
                b.getCompletedByUserId(),
                b.getNoShowByUserId(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}




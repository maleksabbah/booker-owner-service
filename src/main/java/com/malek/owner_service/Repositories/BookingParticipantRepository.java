package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.BookingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingParticipantRepository
        extends JpaRepository<BookingParticipant, BookingParticipant.BookingParticipantId> {

    List<BookingParticipant> findByBooking_Id(Long bookingId);
}

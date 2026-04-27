package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingParticipantRepository extends JpaRepository<BookingParticipant, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM booking_participant WHERE booking_id = :bookingId ORDER BY created_at ASC")
    List<BookingParticipant> findByBookingId(@Param("bookingId") Long bookingId);

    @Query(nativeQuery = true, value = "SELECT * FROM booking_participant WHERE id = :id AND booking_id = :bookingId")
    Optional<BookingParticipant> findByIdAndBookingId(@Param("id") Long id, @Param("bookingId") Long bookingId);
}

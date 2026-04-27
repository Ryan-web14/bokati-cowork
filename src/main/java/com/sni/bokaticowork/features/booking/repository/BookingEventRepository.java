package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM booking_event WHERE event_number = :eventNumber")
    Optional<BookingEvent> findByEventNumber(@Param("eventNumber") String eventNumber);

    @Query(nativeQuery = true, value = "SELECT * FROM booking_event WHERE booking_id = :bookingId ORDER BY created_at DESC")
    List<BookingEvent> findByBookingId(@Param("bookingId") Long bookingId);
}

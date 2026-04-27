package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingLineRepository extends JpaRepository<BookingLine, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM booking_line WHERE booking_id = :bookingId ORDER BY created_at ASC")
    List<BookingLine> findByBookingId(@Param("bookingId") Long bookingId);
}

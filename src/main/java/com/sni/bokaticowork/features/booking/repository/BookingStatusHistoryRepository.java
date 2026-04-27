package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM booking_status_history WHERE booking_id = :bookingId ORDER BY changed_at ASC")
    List<BookingStatusHistory> findByBookingId(@Param("bookingId") Long bookingId);
}

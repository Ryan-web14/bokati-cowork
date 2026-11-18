package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingEndReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Lot R5d : etats des lieux de fin d'utilisation. */
@Repository
public interface BookingEndReportRepository extends JpaRepository<BookingEndReport, Long> {

    List<BookingEndReport> findAllByBookingOrderByCreatedAtDesc(Booking booking);
}

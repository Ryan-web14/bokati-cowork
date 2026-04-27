package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingRecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRecurrenceGroupRepository extends JpaRepository<BookingRecurrenceGroup, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM booking_recurrence_group WHERE group_number = :groupNumber")
    Optional<BookingRecurrenceGroup> findByGroupNumber(@Param("groupNumber") String groupNumber);
}

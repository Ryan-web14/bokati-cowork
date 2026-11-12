package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingBatchRepository extends JpaRepository<BookingBatch, Long> {

    Optional<BookingBatch> findByBatchNumber(String batchNumber);

    Optional<BookingBatch> findByIdempotencyKey(String idempotencyKey);
}

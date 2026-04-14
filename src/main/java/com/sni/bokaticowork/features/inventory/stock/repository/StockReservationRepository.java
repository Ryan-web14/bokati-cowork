package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import com.sni.bokaticowork.features.inventory.stock.model.StockReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    boolean existsByReservationCode(String reservationCode);

    Optional<StockReservation> findByReservationCode(String reservationCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT reservation FROM StockReservation reservation WHERE reservation.reservationCode = :reservationCode")
    Optional<StockReservation> findByReservationCodeForUpdate(@Param("reservationCode") String reservationCode);

    List<StockReservation> findAllByStatusAndExpiresAtBefore(StockReservationStatus status, Instant expiresAt);

    org.springframework.data.domain.Page<StockReservation> findAllByStatus(StockReservationStatus status, org.springframework.data.domain.Pageable pageable);
}

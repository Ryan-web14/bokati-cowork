package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingStockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingStockReservationRepository extends JpaRepository<BillingStockReservation, Long> {

    List<BillingStockReservation> findByDocumentNumberAndStatus(String documentNumber, String status);

    List<BillingStockReservation> findByDocumentNumberOrderByCreatedAtAsc(String documentNumber);
}

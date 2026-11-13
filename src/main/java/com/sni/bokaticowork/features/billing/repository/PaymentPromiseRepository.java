package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.PaymentPromise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C12 : promesses de paiement. */
@Repository
public interface PaymentPromiseRepository extends JpaRepository<PaymentPromise, Long> {

    Optional<PaymentPromise> findByPromiseNumber(String promiseNumber);

    List<PaymentPromise> findByDocumentNumberOrderByCreatedAtDesc(String documentNumber);

    List<PaymentPromise> findByDocumentNumberAndStatus(String documentNumber, String status);

    List<PaymentPromise> findByStatusOrderByPromisedDateAsc(String status);
}

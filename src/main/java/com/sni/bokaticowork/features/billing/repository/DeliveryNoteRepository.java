package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.DeliveryNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C4 : bons de livraison. */
@Repository
public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, Long> {

    Optional<DeliveryNote> findByNoteNumber(String noteNumber);

    List<DeliveryNote> findBySourceDocumentNumberOrderByCreatedAtAsc(String sourceDocumentNumber);

    List<DeliveryNote> findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(String customerType, String customerCode);

    List<DeliveryNote> findByStatusOrderByCreatedAtDesc(String status);

    List<DeliveryNote> findTop200ByOrderByCreatedAtDesc();
}

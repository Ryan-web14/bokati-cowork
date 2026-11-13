package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.DeliveryNoteLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** Lot C4 : lignes des bons de livraison. */
@Repository
public interface DeliveryNoteLineRepository extends JpaRepository<DeliveryNoteLine, Long> {

    List<DeliveryNoteLine> findByNoteIdOrderByLineOrderAsc(Long noteId);

    List<DeliveryNoteLine> findByNoteIdIn(Collection<Long> noteIds);
}

package com.sni.bokaticowork.features.billing.element.repository;

import com.sni.bokaticowork.features.billing.element.model.BillingDocumentElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingDocumentElementRepository extends JpaRepository<BillingDocumentElement, Long> {

    List<BillingDocumentElement> findByDocumentNumberAndRemovedAtIsNullOrderByLinkedAtAsc(String documentNumber);

    List<BillingDocumentElement> findByElementTypeAndElementCodeAndRemovedAtIsNullOrderByLinkedAtAsc(String elementType, String elementCode);

    Optional<BillingDocumentElement> findFirstByDocumentNumberAndElementTypeAndElementCodeAndRemovedAtIsNull(String documentNumber,
                                                                                                             String elementType, String elementCode);
}

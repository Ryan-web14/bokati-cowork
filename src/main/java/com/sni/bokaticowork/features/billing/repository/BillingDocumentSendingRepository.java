package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocumentSending;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingDocumentSendingRepository extends JpaRepository<BillingDocumentSending, Long> {

    List<BillingDocumentSending> findByDocumentNumberOrderBySentAtDesc(String documentNumber);
}

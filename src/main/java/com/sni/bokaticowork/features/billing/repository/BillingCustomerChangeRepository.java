package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingCustomerChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingCustomerChangeRepository extends JpaRepository<BillingCustomerChange, Long> {

    List<BillingCustomerChange> findByOriginalDocumentNumberOrderByCreatedAtDesc(String originalDocumentNumber);
}

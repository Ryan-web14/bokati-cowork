package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.enums.BillingChangeRequestStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocumentChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingDocumentChangeRequestRepository extends JpaRepository<BillingDocumentChangeRequest, Long> {

    List<BillingDocumentChangeRequest> findByDocumentNumberOrderByRequestedAtDesc(String documentNumber);

    List<BillingDocumentChangeRequest> findByDocumentNumberAndStatus(String documentNumber, BillingChangeRequestStatus status);

    Optional<BillingDocumentChangeRequest> findFirstByApprovalNumber(String approvalNumber);
}

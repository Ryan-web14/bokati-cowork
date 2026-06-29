package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KycCaseRepository extends JpaRepository<KycCase, Long>, JpaSpecificationExecutor<KycCase> {

    Optional<KycCase> findByCode(String code);

    Optional<KycCase> findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType ownerType, Long ownerId);

    List<KycCase> findAllByOwnerTypeOrderByStartedAtDesc(DocumentOwnerType ownerType);

    List<KycCase> findAllByAssignedToOrderBySubmittedAtAsc(Long assignedTo);

    Page<KycCase> findAllByAssignedToOrderBySubmittedAtAsc(Long assignedTo, Pageable pageable);

    List<KycCase> findAllByStatusInAndLastReminderSentAtBeforeOrStatusInAndLastReminderSentAtIsNull(
            Collection<KycCaseStatus> statuses1,
            Instant before,
            Collection<KycCaseStatus> statuses2
    );
}

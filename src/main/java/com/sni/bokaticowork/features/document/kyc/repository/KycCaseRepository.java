package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycCaseRepository extends JpaRepository<KycCase, Long> {

    Optional<KycCase> findByCode(String code);

    Optional<KycCase> findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType ownerType, Long ownerId);

    List<KycCase> findAllByOwnerTypeOrderByStartedAtDesc(DocumentOwnerType ownerType);
}

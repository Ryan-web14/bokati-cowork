package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycCaseNoteRepository extends JpaRepository<KycCaseNote, Long> {
    List<KycCaseNote> findAllByKycCaseOrderByCreatedAtDesc(KycCase kycCase);
}

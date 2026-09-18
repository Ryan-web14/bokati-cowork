package com.sni.bokaticowork.features.payment.compliance.repository;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplianceCaseNoteRepository extends JpaRepository<ComplianceCaseNote, Long> {

    List<ComplianceCaseNote> findByComplianceCase_IdOrderByCreatedAtAsc(Long caseId);
}

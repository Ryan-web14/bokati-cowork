package com.sni.bokaticowork.features.payment.compliance.repository;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceAuditEntryRepository extends JpaRepository<ComplianceAuditEntry, Long> {

    /** Le dernier maillon · la piste est une seule chaine, pas une par sujet. */
    @Query(nativeQuery = true, value = "SELECT current_hash FROM compliance_audit_entry ORDER BY id DESC LIMIT 1")
    Optional<String> findLastHash();

    List<ComplianceAuditEntry> findBySubjectTypeAndSubjectCodeOrderByIdAsc(String subjectType, String subjectCode);

    @Query(nativeQuery = true, value = "SELECT * FROM compliance_audit_entry ORDER BY id ASC")
    List<ComplianceAuditEntry> findChain();
}

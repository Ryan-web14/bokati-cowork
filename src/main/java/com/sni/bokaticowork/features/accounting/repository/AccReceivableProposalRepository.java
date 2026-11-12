package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccReceivableProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccReceivableProposalRepository extends JpaRepository<AccReceivableProposal, Long> {
    List<AccReceivableProposal> findAllByStatusOrderByCreatedAtAsc(String status);
    List<AccReceivableProposal> findAllByDocumentNumberAndStatusIn(String documentNumber, java.util.Collection<String> statuses);
}

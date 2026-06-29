package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.enums.ContractDraftStatus;
import com.sni.bokaticowork.features.contract.model.ContractDraft;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ContractDraftRepository extends JpaRepository<ContractDraft, Long> {
    Optional<ContractDraft> findByCode(String code);

    Page<ContractDraft> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<ContractDraft> findAllByStatus(ContractDraftStatus status, Pageable pageable);

    Page<ContractDraft> findAllByOwnerTypeAndOwnerCode(DocumentOwnerType ownerType, String ownerCode, Pageable pageable);

    @Query("SELECT d FROM ContractDraft d WHERE " +
            "LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ContractDraft> search(String query, Pageable pageable);
}

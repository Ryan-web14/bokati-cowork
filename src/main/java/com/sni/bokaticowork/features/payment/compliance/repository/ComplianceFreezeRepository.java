package com.sni.bokaticowork.features.payment.compliance.repository;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceFreeze;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceFreezeRepository extends JpaRepository<ComplianceFreeze, Long> {

    Optional<ComplianceFreeze> findByFreezeNumber(String freezeNumber);

    Optional<ComplianceFreeze> findFirstByWallet_IdAndLiftedAtIsNull(Long walletId);

    /** Le portefeuille est lu avec le gel : la vue du gel le lit hors transaction. */
    @EntityGraph(attributePaths = {"wallet"})
    List<ComplianceFreeze> findByWallet_IdOrderByFrozenAtDesc(Long walletId);

    /** Le portefeuille est lu avec le gel : la vue du gel le lit hors transaction. */
    @EntityGraph(attributePaths = {"wallet"})
    Page<ComplianceFreeze> findByLiftedAtIsNullOrderByFrozenAtDesc(Pageable pageable);
}

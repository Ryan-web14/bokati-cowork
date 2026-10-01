package com.sni.bokaticowork.features.payment.cash.repository;

import com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashPaymentDeclarationRepository extends JpaRepository<CashPaymentDeclaration, Long> {

    Optional<CashPaymentDeclaration> findByDeclarationNumber(String declarationNumber);

    Page<CashPaymentDeclaration> findByStatusOrderByDeclaredAtAsc(CashDeclarationStatus status, Pageable pageable);

    Page<CashPaymentDeclaration> findAllByOrderByDeclaredAtDesc(Pageable pageable);

    Page<CashPaymentDeclaration> findByCustomerTypeAndCustomerCodeOrderByDeclaredAtDesc(
            String customerType, String customerCode, Pageable pageable);

    /** L annonce en cours sur cette facture · une seule a la fois, sinon la caisse encaisse deux fois. */
    Optional<CashPaymentDeclaration> findFirstByDocumentNumberAndStatusOrderByDeclaredAtDesc(
            String documentNumber, CashDeclarationStatus status);

    /** Les annonces dont le delai est passe · ce que le worker ramasse. */
    @Query("""
            SELECT d FROM CashPaymentDeclaration d
            WHERE d.status = com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus.AWAITING_CONFIRMATION
              AND d.expiresAt < :now
            ORDER BY d.expiresAt ASC
            """)
    List<CashPaymentDeclaration> findExpired(@Param("now") Instant now, Pageable pageable);

    long countByStatus(CashDeclarationStatus status);
}

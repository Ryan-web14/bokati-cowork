package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.Withholding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C6 : retenues a la source. */
@Repository
public interface WithholdingRepository extends JpaRepository<Withholding, Long> {

    Optional<Withholding> findByWithholdingNumber(String withholdingNumber);

    List<Withholding> findByDocumentNumberOrderByCreatedAtAsc(String documentNumber);

    List<Withholding> findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(String customerType, String customerCode);

    /** Retenues enregistrees dont l'attestation n'est pas encore rattachee. */
    @Query("SELECT w FROM Withholding w WHERE w.status = 'RECORDED' AND w.certificateDocument IS NULL ORDER BY w.withheldOn")
    List<Withholding> findAwaitingCertificate();

    List<Withholding> findTop200ByOrderByCreatedAtDesc();
}

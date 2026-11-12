package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashVarianceReviewRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashVarianceReview, Long> {

    Optional<com.sni.bokaticowork.features.payment.model.CashVarianceReview> findByReviewNumber(String reviewNumber);

    List<com.sni.bokaticowork.features.payment.model.CashVarianceReview> findBySessionNumberOrderByCreatedAtAsc(String sessionNumber);

    List<com.sni.bokaticowork.features.payment.model.CashVarianceReview> findByStatusOrderByCreatedAtAsc(com.sni.bokaticowork.features.payment.enums.CashVarianceStatus status);

    List<com.sni.bokaticowork.features.payment.model.CashVarianceReview> findByRegisterCodeOrderByCreatedAtDesc(String registerCode);

    @Query("SELECT r FROM CashVarianceReview r WHERE (:registerCode IS NULL OR r.registerCode = :registerCode) AND r.createdAt >= :from AND r.createdAt < :to ORDER BY r.createdAt ASC")
    List<com.sni.bokaticowork.features.payment.model.CashVarianceReview> findBetween(@Param("registerCode") String registerCode, @Param("from") java.time.Instant from,
                                                               @Param("to") java.time.Instant to);

    /** Lot F3 : constats d'ecart a transmettre a la comptabilite. */
    java.util.List<com.sni.bokaticowork.features.payment.model.CashVarianceReview> findByAccountingStatusOrderByCreatedAtAsc(
            String accountingStatus, org.springframework.data.domain.Pageable pageable);

    /** Lot F3 : imputations decidees pas encore transmises. */
    @org.springframework.data.jpa.repository.Query("select r from CashVarianceReview r where r.decision is not null "
            + "and (r.decisionAccountingStatus is null or r.decisionAccountingStatus = 'PENDING') order by r.decidedAt asc")
    java.util.List<com.sni.bokaticowork.features.payment.model.CashVarianceReview> decisionsToPost(
            org.springframework.data.domain.Pageable pageable);

    /** Lot F3 : ecarts deja constates en comptabilite puis remplaces par un recomptage. */
    @org.springframework.data.jpa.repository.Query("select r from CashVarianceReview r where r.status = com.sni.bokaticowork.features.payment.enums.CashVarianceStatus.DECIDED "
            + "and r.decision is null and r.accountingStatus in ('QUEUED', 'POSTED') ")
    java.util.List<com.sni.bokaticowork.features.payment.model.CashVarianceReview> supersededAfterPosting();
}

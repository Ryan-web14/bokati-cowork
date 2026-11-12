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
}

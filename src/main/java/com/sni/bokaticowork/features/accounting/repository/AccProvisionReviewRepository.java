package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccProvisionReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccProvisionReviewRepository extends JpaRepository<AccProvisionReview, Long> {
    List<AccProvisionReview> findAllByFiscalYearOrderByCreatedAtDesc(String fiscalYear);
}

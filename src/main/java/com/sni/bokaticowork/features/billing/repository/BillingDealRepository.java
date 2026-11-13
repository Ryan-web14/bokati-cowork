package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.enums.BillingDealStage;
import com.sni.bokaticowork.features.billing.model.BillingDeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BillingDealRepository extends JpaRepository<BillingDeal, Long> {

    Optional<BillingDeal> findByCode(String code);

    List<BillingDeal> findByCustomerTypeAndCustomerCodeAndStageInOrderByCreatedAtDesc(String customerType, String customerCode,
                                                                                     Collection<BillingDealStage> stages);

    @Query("""
            SELECT d FROM BillingDeal d
            WHERE d.stage IN :stages
              AND (:owner = '' OR lower(d.owner) = lower(:owner))
              AND (:customerCode = '' OR d.customerCode = :customerCode)
              AND (:search = '' OR lower(d.title) LIKE lower(CONCAT('%', :search, '%')) OR lower(d.customerName) LIKE lower(CONCAT('%', :search, '%'))
                   OR lower(d.code) LIKE lower(CONCAT('%', :search, '%')))
              AND d.createdAt >= :from AND d.createdAt < :to
            ORDER BY d.createdAt DESC
            """)
    Page<BillingDeal> search(@Param("stages") Collection<BillingDealStage> stages, @Param("owner") String owner,
                             @Param("customerCode") String customerCode, @Param("search") String search,
                             @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}

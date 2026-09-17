package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponBatchRepository extends JpaRepository<CouponBatch, Long> {

    @Query("SELECT b FROM CouponBatch b WHERE lower(b.batchCode) = lower(:batchCode)")
    Optional<CouponBatch> findByBatchCode(@Param("batchCode") String batchCode);
}

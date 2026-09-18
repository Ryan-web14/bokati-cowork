package com.sni.bokaticowork.features.subscription.promotion.pricing.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.model.AppliedDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppliedDiscountRepository extends JpaRepository<AppliedDiscount, Long> {

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM applied_discount
            WHERE document_type = CAST(:documentType AS VARCHAR)
              AND document_code = CAST(:documentCode AS VARCHAR)
              AND reversed_at IS NULL
            """)
    List<AppliedDiscount> findActiveByDocument(@Param("documentType") String documentType,
                                               @Param("documentCode") String documentCode);

    /** Nombre de fois qu'un abonne a deja beneficie de cette source, contre-passations exclues. */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM applied_discount
            WHERE source_type = CAST(:sourceType AS VARCHAR)
              AND source_code = CAST(:sourceCode AS VARCHAR)
              AND subscriber_code = CAST(:subscriberCode AS VARCHAR)
              AND reversed_at IS NULL
            """)
    long countForSubscriber(@Param("sourceType") String sourceType,
                            @Param("sourceCode") String sourceCode,
                            @Param("subscriberCode") String subscriberCode);
}

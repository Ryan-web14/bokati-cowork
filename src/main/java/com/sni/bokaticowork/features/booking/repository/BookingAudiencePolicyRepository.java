package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingAudiencePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingAudiencePolicyRepository extends JpaRepository<BookingAudiencePolicy, Long> {

    List<BookingAudiencePolicy> findAllByOrderByCreatedAtDesc();

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking_audience_policy
            WHERE audience_type = :audienceType
              AND active = true
              AND (:resourceTypeCode IS NULL OR resource_type_code IS NULL OR resource_type_code = :resourceTypeCode)
              AND (:resourceGroupCode IS NULL OR resource_group_code IS NULL OR resource_group_code = :resourceGroupCode)
            ORDER BY
              CASE WHEN resource_type_code IS NOT NULL THEN 1 ELSE 0 END DESC,
              CASE WHEN resource_group_code IS NOT NULL THEN 1 ELSE 0 END DESC,
              created_at DESC
            LIMIT 1
            """)
    Optional<BookingAudiencePolicy> findEffective(@Param("audienceType") String audienceType,
                                                  @Param("resourceTypeCode") String resourceTypeCode,
                                                  @Param("resourceGroupCode") String resourceGroupCode);
}

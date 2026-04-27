package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingQuotaOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingQuotaOverrideRepository extends JpaRepository<BookingQuotaOverride, Long> {

    List<BookingQuotaOverride> findAllByOrderByCreatedAtDesc();

    @Query(nativeQuery = true, value = "SELECT * FROM booking_quota_override WHERE override_number = :overrideNumber")
    Optional<BookingQuotaOverride> findByOverrideNumber(@Param("overrideNumber") String overrideNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM booking_quota_override
            WHERE owner_type = :ownerType
              AND owner_code = :ownerCode
              AND active = true
              AND valid_from <= now()
              AND (valid_until IS NULL OR valid_until >= now())
              AND (:resourceCode IS NULL OR resource_code IS NULL OR resource_code = :resourceCode)
            ORDER BY created_at DESC
            """)
    List<BookingQuotaOverride> findActive(@Param("ownerType") String ownerType,
                                          @Param("ownerCode") String ownerCode,
                                          @Param("resourceCode") String resourceCode);
}

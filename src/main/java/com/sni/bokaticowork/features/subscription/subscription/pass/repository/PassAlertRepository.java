package com.sni.bokaticowork.features.subscription.subscription.pass.repository;

import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassAlertRepository extends JpaRepository<PassAlert, Long> {

    @Query(nativeQuery = true, value = """
            SELECT EXISTS(
                SELECT 1 FROM pass_alert
                WHERE pass_id = :passId
                  AND alert_type = CAST(:alertType AS VARCHAR)
                  AND threshold_key = CAST(:thresholdKey AS VARCHAR)
            )
            """)
    boolean alreadyAnnounced(@Param("passId") Long passId,
                             @Param("alertType") String alertType,
                             @Param("thresholdKey") String thresholdKey);
}

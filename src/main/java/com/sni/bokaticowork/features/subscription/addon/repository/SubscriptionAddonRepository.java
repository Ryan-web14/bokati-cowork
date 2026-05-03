package com.sni.bokaticowork.features.subscription.addon.repository;

import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionAddonRepository extends JpaRepository<SubscriptionAddon, Long>, JpaSpecificationExecutor<SubscriptionAddon> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_addon WHERE subscription_id = :subscriptionId ORDER BY created_at DESC")
    List<SubscriptionAddon> findAllBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionAddon> findAllByContractCode(String contractCode);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE subscription_addon
            SET status = 'EXPIRED',
                updated_at = now()
            WHERE status = 'ACTIVE'
              AND ends_at IS NOT NULL
              AND ends_at < :date
            """)
    int expireEndedAddons(@Param("date") LocalDate date);
}

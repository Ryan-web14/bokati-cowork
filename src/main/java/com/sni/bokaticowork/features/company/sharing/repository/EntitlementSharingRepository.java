package com.sni.bokaticowork.features.company.sharing.repository;

import com.sni.bokaticowork.features.company.sharing.model.EntitlementSharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntitlementSharingRepository extends JpaRepository<EntitlementSharing, Long> {

    Optional<EntitlementSharing> findBySubscription_IdAndEntitlementCodeIgnoreCase(Long subscriptionId, String entitlementCode);

    List<EntitlementSharing> findAllBySubscription_Id(Long subscriptionId);
}

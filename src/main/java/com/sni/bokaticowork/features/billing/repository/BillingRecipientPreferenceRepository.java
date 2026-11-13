package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingRecipientPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingRecipientPreferenceRepository extends JpaRepository<BillingRecipientPreference, Long> {

    Optional<BillingRecipientPreference> findByCustomerTypeAndCustomerCode(String customerType, String customerCode);
}

package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassPlanPriceRepository extends JpaRepository<PassPlanPrice, Long> {

    Optional<PassPlanPrice> findByPassVersionAndCurrency(PassPlanVersion version, String currency);

    List<PassPlanPrice> findAllByPassVersion(PassPlanVersion version);
}

package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.ServiceCatalogPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceCatalogPriceRepository extends JpaRepository<ServiceCatalogPrice, Long> {

    List<ServiceCatalogPrice> findByItemCodeOrderByValidFromDescIdDesc(String itemCode);

    List<ServiceCatalogPrice> findByItemCodeAndSegmentOrderByValidFromDescIdDesc(String itemCode, String segment);
}

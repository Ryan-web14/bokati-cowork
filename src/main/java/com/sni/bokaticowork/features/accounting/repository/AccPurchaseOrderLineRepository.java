package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPurchaseOrderLineRepository extends JpaRepository<AccPurchaseOrderLine, Long> {

    java.util.List<AccPurchaseOrderLine> findAllByOrderIdOrderByLineNoAsc(Long orderId);
}

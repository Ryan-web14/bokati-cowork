package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPurchaseOrderRepository extends JpaRepository<AccPurchaseOrder, Long> {

    java.util.Optional<AccPurchaseOrder> findByNumber(String number);

    java.util.List<AccPurchaseOrder> findTop200ByOrderByCreatedAtDesc();

    long countByNumberStartingWith(String prefix);
}

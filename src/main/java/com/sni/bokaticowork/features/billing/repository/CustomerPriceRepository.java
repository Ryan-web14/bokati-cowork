package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.CustomerPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Lot C8 : tarifs negocies par client. */
@Repository
public interface CustomerPriceRepository extends JpaRepository<CustomerPrice, Long> {

    List<CustomerPrice> findByCustomerTypeAndCustomerCodeOrderByItemCodeAscValidFromDesc(String customerType, String customerCode);

    List<CustomerPrice> findByCustomerTypeAndCustomerCodeAndItemCodeOrderByValidFromDesc(String customerType, String customerCode, String itemCode);
}

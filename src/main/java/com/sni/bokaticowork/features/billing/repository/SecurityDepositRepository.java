package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.SecurityDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C7 : depots de garantie. */
@Repository
public interface SecurityDepositRepository extends JpaRepository<SecurityDeposit, Long> {

    Optional<SecurityDeposit> findByDepositNumber(String depositNumber);

    Optional<SecurityDeposit> findByDocumentNumber(String documentNumber);

    List<SecurityDeposit> findBySubscriptionNumberOrderByCreatedAtAsc(String subscriptionNumber);

    List<SecurityDeposit> findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(String customerType, String customerCode);

    List<SecurityDeposit> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
}

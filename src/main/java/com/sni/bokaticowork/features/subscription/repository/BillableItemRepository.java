package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface BillableItemRepository extends JpaRepository<BillableItem, Long>, JpaSpecificationExecutor<BillableItem> {

    @Query(nativeQuery = true, value = "SELECT * FROM billable_item WHERE billable_number = :billableNumber")
    Optional<BillableItem> findByBillableNumber(@Param("billableNumber") String billableNumber);

    /** Les elements factures sur un document · pour retrouver l'abonnement derriere une facture. */
    @Query(nativeQuery = true, value = "SELECT * FROM billable_item WHERE invoice_id = :invoiceId")
    List<BillableItem> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billable_item
            WHERE UPPER(TRIM(subscriber_type)) = UPPER(TRIM(CAST(:subscriberType AS VARCHAR)))
              AND UPPER(TRIM(subscriber_code)) = UPPER(TRIM(CAST(:subscriberCode AS VARCHAR)))
              AND status IN ('PENDING', 'SENT_TO_INVOICE')
            ORDER BY created_at ASC
            """)
    List<BillableItem> findRecoverableItems(@Param("subscriberType") String subscriberType,
                                            @Param("subscriberCode") String subscriberCode);

    Page<BillableItem> findAllByStatus(BillableItemStatus status, Pageable pageable);
}

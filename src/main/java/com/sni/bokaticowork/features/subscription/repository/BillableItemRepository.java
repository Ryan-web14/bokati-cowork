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

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billable_item
            WHERE subscriber_type = CAST(:subscriberType AS VARCHAR)
              AND subscriber_code = :subscriberCode
              AND status IN ('PENDING', 'SENT_TO_INVOICE')
            ORDER BY created_at ASC
            """)
    List<BillableItem> findRecoverableItems(@Param("subscriberType") String subscriberType,
                                            @Param("subscriberCode") String subscriberCode);

    Page<BillableItem> findAllByStatus(BillableItemStatus status, Pageable pageable);
}

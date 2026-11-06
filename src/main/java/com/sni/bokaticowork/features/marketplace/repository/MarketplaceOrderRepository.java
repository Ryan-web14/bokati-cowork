package com.sni.bokaticowork.features.marketplace.repository;

import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long> {

    Optional<MarketplaceOrder> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM MarketplaceOrder o WHERE o.orderNumber = :orderNumber")
    Optional<MarketplaceOrder> lockByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("""
            SELECT o FROM MarketplaceOrder o
            WHERE lower(o.buyerMemberCode) = lower(:memberCode)
              AND o.status = com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder.Status.CART
            """)
    Optional<MarketplaceOrder> findCart(@Param("memberCode") String memberCode);

    @Query("""
            SELECT o FROM MarketplaceOrder o
            WHERE lower(o.buyerMemberCode) = lower(:memberCode)
              AND o.status <> com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder.Status.CART
            ORDER BY o.createdAt DESC
            """)
    List<MarketplaceOrder> findOfMember(@Param("memberCode") String memberCode, Pageable pageable);

    @Query("""
            SELECT o FROM MarketplaceOrder o
            WHERE lower(o.billingCompanyCode) = lower(:companyCode)
              AND o.status <> com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder.Status.CART
            ORDER BY o.createdAt DESC
            """)
    List<MarketplaceOrder> findOfCompany(@Param("companyCode") String companyCode, Pageable pageable);

    @Query("""
            SELECT o FROM MarketplaceOrder o
            WHERE o.status <> com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder.Status.CART
              AND (:status IS NULL OR o.status = :status)
            ORDER BY o.createdAt DESC
            """)
    List<MarketplaceOrder> search(@Param("status") MarketplaceOrder.Status status, Pageable pageable);

    /** Les commandes validees non encore reglees · le passage periodique les solde ou les expire. */
    @Query("""
            SELECT o.orderNumber FROM MarketplaceOrder o
            WHERE o.status = com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder.Status.PENDING_PAYMENT
            ORDER BY o.placedAt ASC
            """)
    List<String> findPendingNumbers(Pageable pageable);

    /** Les commandes reglees dont un article n'est pas encore sorti du stock. */
    @Query("""
            SELECT DISTINCT o.orderNumber FROM MarketplaceOrder o JOIN o.lines l
            WHERE o.status = com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder.Status.PAID
              AND l.stockReservationCode IS NOT NULL AND l.stockConsumedAt IS NULL
            """)
    List<String> findStockPendingNumbers(Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM MarketplaceOrder o JOIN o.lines l
            WHERE o.status = com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder.Status.PAID
              AND l.stockReservationCode IS NOT NULL AND l.stockConsumedAt IS NULL
            """)
    List<MarketplaceOrder> findStockPending(Pageable pageable);
}

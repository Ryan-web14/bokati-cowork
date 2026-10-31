package com.sni.bokaticowork.features.company.sharing.repository;

import com.sni.bokaticowork.features.company.sharing.model.SeatUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatUsageRepository extends JpaRepository<SeatUsage, Long> {

    Optional<SeatUsage> findBySeatIdAndEntitlementCodeAndReferenceTypeAndReferenceId(Long seatId, String entitlementCode,
                                                                                    String referenceType, String referenceId);

    List<SeatUsage> findAllByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    /** Ce que la place a pris sur sa part dans la periode · retenu ou consomme. */
    @Query("""
            SELECT COALESCE(SUM(u.fromQuota), 0) FROM SeatUsage u
            WHERE u.seatId = :seatId AND u.entitlementCode = :code AND u.periodStart = :period
              AND u.status <> com.sni.bokaticowork.features.company.sharing.model.SeatUsage.Status.RELEASED
            """)
    BigDecimal sumSeatQuota(@Param("seatId") Long seatId, @Param("code") String code, @Param("period") LocalDate period);

    /** Tout ce que la place a pris dans la periode · pour son plafond. */
    @Query("""
            SELECT COALESCE(SUM(u.quantity), 0) FROM SeatUsage u
            WHERE u.seatId = :seatId AND u.entitlementCode = :code AND u.periodStart = :period
              AND u.status <> com.sni.bokaticowork.features.company.sharing.model.SeatUsage.Status.RELEASED
            """)
    BigDecimal sumSeatTotal(@Param("seatId") Long seatId, @Param("code") String code, @Param("period") LocalDate period);

    /** Ce que toutes les places ont pris sur le pot commun dans la periode. */
    @Query("""
            SELECT COALESCE(SUM(u.fromPool), 0) FROM SeatUsage u
            WHERE u.subscriptionId = :subscriptionId AND u.entitlementCode = :code AND u.periodStart = :period
              AND u.status <> com.sni.bokaticowork.features.company.sharing.model.SeatUsage.Status.RELEASED
            """)
    BigDecimal sumPool(@Param("subscriptionId") Long subscriptionId, @Param("code") String code, @Param("period") LocalDate period);

    @Query("""
            SELECT u FROM SeatUsage u
            WHERE u.subscriptionId = :subscriptionId AND u.periodStart = :period
              AND u.status <> com.sni.bokaticowork.features.company.sharing.model.SeatUsage.Status.RELEASED
            """)
    List<SeatUsage> findPeriodUsage(@Param("subscriptionId") Long subscriptionId, @Param("period") LocalDate period);

    /** Ce qui a ete consomme sur l'abonnement pendant une periode · reservations closes seulement. */
    @Query("""
            SELECT u FROM SeatUsage u
            WHERE u.subscriptionId = :subscriptionId AND u.periodStart = :period
              AND u.status = com.sni.bokaticowork.features.company.sharing.model.SeatUsage.Status.CONSUMED
            ORDER BY u.memberCode, u.entitlementCode
            """)
    List<SeatUsage> findConsumedInPeriod(@Param("subscriptionId") Long subscriptionId, @Param("period") LocalDate period);

    /** Le debut de la derniere periode consommee avant une date · la periode qui vient de s'achever. */
    @Query("""
            SELECT MAX(u.periodStart) FROM SeatUsage u
            WHERE u.subscriptionId = :subscriptionId AND u.periodStart < :before AND u.periodStart >= :notBefore
              AND u.status = com.sni.bokaticowork.features.company.sharing.model.SeatUsage.Status.CONSUMED
            """)
    LocalDate findLastPeriodBefore(@Param("subscriptionId") Long subscriptionId, @Param("before") LocalDate before,
                                   @Param("notBefore") LocalDate notBefore);
}

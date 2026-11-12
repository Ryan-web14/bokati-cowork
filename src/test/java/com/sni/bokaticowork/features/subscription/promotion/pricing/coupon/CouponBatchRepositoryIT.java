package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponBatchRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les requetes de consultation des lots sur le schema de production : la promotion est lue avec le
 * lot (sans open-in-view, la vue la lirait trop tard), les compteurs se groupent par lot et par
 * statut, et les codes d'un lot se paginent dans l'ordre alphabetique.
 */
class CouponBatchRepositoryIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private CouponBatchRepository batches;
    @Autowired private CouponRepository coupons;

    @BeforeEach
    void seed() {
        entityManager.createNativeQuery("""
                INSERT INTO promotion (id, code, name, discount_type, discount_value, starts_at, status)
                VALUES (993000001, 'PROMO-IT-A', 'Rentree', 'PERCENTAGE', 10, now(), 'ACTIVE'),
                       (993000002, 'PROMO-IT-B', 'Partenaire', 'PERCENTAGE', 5, now(), 'ACTIVE')
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO coupon_batch (id, batch_code, promotion_id, name, requested_quantity, generated_quantity,
                                          coupon_kind, created_at, updated_at)
                VALUES (993000011, 'CPB-IT-OLD', 993000001, 'Ancien lot', 4, 4, 'SINGLE_USE', now() - interval '2 days', now()),
                       (993000012, 'CPB-IT-NEW', 993000001, 'Nouveau lot', 1, 1, 'MULTI_USE', now() - interval '1 day', now()),
                       (993000013, 'CPB-IT-OTHER', 993000002, 'Autre promotion', 1, 1, 'SINGLE_USE', now(), now())
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO coupon (id, code, promotion_id, batch_id, coupon_kind, max_redemptions, redemption_count,
                                    reserved_count, status)
                VALUES (993000021, 'IT-DDDD', 993000001, 993000011, 'SINGLE_USE', 1, 0, 1, 'ACTIVE'),
                       (993000022, 'IT-BBBB', 993000001, 993000011, 'SINGLE_USE', 1, 1, 0, 'USED'),
                       (993000023, 'IT-AAAA', 993000001, 993000011, 'SINGLE_USE', 1, 0, 0, 'ACTIVE'),
                       (993000024, 'IT-CCCC', 993000001, 993000011, 'SINGLE_USE', 1, 0, 0, 'REVOKED'),
                       (993000025, 'IT-EEEE', 993000001, 993000012, 'MULTI_USE', NULL, 7, 2, 'ACTIVE')
                """).executeUpdate();
        entityManager.clear();
    }

    @Test
    @DisplayName("Un lot se relit par son code avec sa promotion deja chargee")
    void batchIsReadWithItsPromotion() {
        CouponBatch batch = batches.findByBatchCode("cpb-it-old").orElseThrow();
        assertThat(Hibernate.isInitialized(batch.getPromotion())).isTrue();
        assertThat(batch.getPromotion().getCode()).isEqualTo("PROMO-IT-A");
    }

    @Test
    @DisplayName("Les lots se listent du plus recent au plus ancien, filtres ou non par promotion")
    void batchesAreListedMostRecentFirst() {
        Page<CouponBatch> all = batches.findRecent(PageRequest.of(0, 50));
        List<String> codes = all.getContent().stream().map(CouponBatch::getBatchCode)
                .filter(code -> code.startsWith("CPB-IT-")).toList();
        assertThat(codes).containsExactly("CPB-IT-OTHER", "CPB-IT-NEW", "CPB-IT-OLD");
        assertThat(Hibernate.isInitialized(all.getContent().getFirst().getPromotion())).isTrue();
        entityManager.clear();

        Page<CouponBatch> ofPromotion = batches.findRecentByPromotionCode("promo-it-a", PageRequest.of(0, 1));
        assertThat(ofPromotion.getTotalElements()).isEqualTo(2);
        assertThat(ofPromotion.getContent()).extracting(CouponBatch::getBatchCode).containsExactly("CPB-IT-NEW");
        assertThat(Hibernate.isInitialized(ofPromotion.getContent().getFirst().getPromotion())).isTrue();
    }

    @Test
    @DisplayName("Les compteurs se groupent par lot et par statut, utilisations et retenues additionnees")
    void countsAreGroupedByBatchAndStatus() {
        List<CouponRepository.BatchStatusCount> rows = coupons.countByStatusForBatches(List.of(993000011L, 993000012L, 993000013L));

        Map<String, long[]> byKey = rows.stream().collect(Collectors.toMap(
                row -> row.getBatchId() + ":" + row.getStatus(),
                row -> new long[]{row.getCouponCount().longValue(), row.getRedemptions().longValue(), row.getReserved().longValue()}));
        assertThat(byKey).hasSize(4);
        assertThat(byKey.get("993000011:ACTIVE")).containsExactly(2, 0, 1);
        assertThat(byKey.get("993000011:USED")).containsExactly(1, 1, 0);
        assertThat(byKey.get("993000011:REVOKED")).containsExactly(1, 0, 0);
        assertThat(byKey.get("993000012:ACTIVE")).containsExactly(1, 7, 2);
    }

    @Test
    @DisplayName("Les codes d'un lot se paginent dans l'ordre alphabetique, avec ou sans filtre de statut")
    void batchCouponsArePagedByCode() {
        Page<Coupon> first = coupons.findPageByBatchId(993000011L, PageRequest.of(0, 3));
        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getContent()).extracting(Coupon::getCode).containsExactly("IT-AAAA", "IT-BBBB", "IT-CCCC");
        assertThat(coupons.findPageByBatchId(993000011L, PageRequest.of(1, 3)).getContent())
                .extracting(Coupon::getCode).containsExactly("IT-DDDD");

        Page<Coupon> active = coupons.findPageByBatchIdAndStatus(993000011L, CouponStatus.ACTIVE, PageRequest.of(0, 50));
        assertThat(active.getTotalElements()).isEqualTo(2);
        assertThat(active.getContent()).extracting(Coupon::getCode).containsExactly("IT-AAAA", "IT-DDDD");

        assertThat(coupons.findAllByBatchIdOrderByCode(993000011L)).extracting(Coupon::getCode)
                .containsExactly("IT-AAAA", "IT-BBBB", "IT-CCCC", "IT-DDDD");
    }
}

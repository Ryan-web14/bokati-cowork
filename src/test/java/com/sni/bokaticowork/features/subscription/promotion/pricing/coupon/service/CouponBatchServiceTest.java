package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponBatchRevocation;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponBatchView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponBatchRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Consultation et revocation d'un lot.
 *
 * <p>Ce qui compte ici : des compteurs lus sur les coupons et non sur le lot, une revocation de lot
 * qui ne touche ni aux codes deja servis ni a ceux deja revoques, et un CSV qu'Excel ouvre sans
 * qu'une cellule puisse devenir une formule.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouponBatchServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private CouponBatchRepository batchRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private CouponBatchService service;

    private CouponBatch batch;

    @BeforeEach
    void setUp() {
        Promotion promotion = new Promotion();
        promotion.setCode("PROMO-RENTREE");
        promotion.setName("Rentree 2026");
        batch = CouponBatch.builder()
                .id(42L)
                .batchCode("CPB-0001")
                .promotion(promotion)
                .name("Flyers campus")
                .requestedQuantity(5)
                .generatedQuantity(5)
                .couponKind(CouponKind.SINGLE_USE)
                .codePrefix("RENTREE")
                .codeLength(8)
                .createdAt(Instant.parse("2026-09-01T08:00:00Z"))
                .build();
        when(batchRepository.findByBatchCode("CPB-0001")).thenReturn(Optional.of(batch));
    }

    @Test
    @DisplayName("La vue d'un lot additionne les compteurs par statut, et chaque statut y figure meme a zero")
    void viewSumsLiveCounts() {
        when(couponRepository.countByStatusForBatches(List.of(42L))).thenReturn(List.of(
                row(42L, CouponStatus.ACTIVE, 3, 0, 1),
                row(42L, CouponStatus.USED, 2, 2, 0)));

        CouponBatchView view = service.view("CPB-0001");

        assertThat(view.batchCode()).isEqualTo("CPB-0001");
        assertThat(view.promotionCode()).isEqualTo("PROMO-RENTREE");
        assertThat(view.promotionName()).isEqualTo("Rentree 2026");
        assertThat(view.totalCoupons()).isEqualTo(5);
        assertThat(view.totalRedemptions()).isEqualTo(2);
        assertThat(view.totalReserved()).isEqualTo(1);
        assertThat(view.countsByStatus())
                .containsEntry(CouponStatus.ACTIVE, 3L)
                .containsEntry(CouponStatus.USED, 2L)
                .containsEntry(CouponStatus.EXPIRED, 0L)
                .containsEntry(CouponStatus.REVOKED, 0L);
    }

    @Test
    @DisplayName("Un lot sans coupon s'affiche avec des compteurs a zero plutot que de tomber")
    void viewOfEmptyBatch() {
        when(couponRepository.countByStatusForBatches(any())).thenReturn(List.of());

        CouponBatchView view = service.view("CPB-0001");

        assertThat(view.totalCoupons()).isZero();
        assertThat(view.countsByStatus()).hasSize(CouponStatus.values().length).containsValue(0L);
    }

    @Test
    @DisplayName("La liste des lots compte une seule fois pour toute la page, et rattache chaque compteur a son lot")
    void listAttachesCountsPerBatch() {
        CouponBatch other = CouponBatch.builder().id(43L).batchCode("CPB-0002").promotion(batch.getPromotion())
                .name("Partenaire").requestedQuantity(1).generatedQuantity(1).couponKind(CouponKind.MULTI_USE).build();
        when(batchRepository.findRecent(any())).thenReturn(new PageImpl<>(List.of(other, batch), PageRequest.of(0, 20), 2));
        when(couponRepository.countByStatusForBatches(List.of(43L, 42L))).thenReturn(List.of(
                row(42L, CouponStatus.ACTIVE, 5, 0, 0),
                row(43L, CouponStatus.REVOKED, 1, 0, 0)));

        Page<CouponBatchView> page = service.list(null, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(CouponBatchView::batchCode).containsExactly("CPB-0002", "CPB-0001");
        assertThat(page.getContent().get(0).countsByStatus()).containsEntry(CouponStatus.REVOKED, 1L);
        assertThat(page.getContent().get(1).countsByStatus()).containsEntry(CouponStatus.ACTIVE, 5L);
        verify(batchRepository, never()).findRecentByPromotionCode(anyString(), any());
    }

    @Test
    @DisplayName("Le filtre par promotion passe par la requete dediee")
    void listFiltersByPromotion() {
        when(batchRepository.findRecentByPromotionCode(any(), any())).thenReturn(Page.empty());

        assertThat(service.list(" PROMO-RENTREE ", PageRequest.of(0, 20))).isEmpty();

        verify(batchRepository).findRecentByPromotionCode("PROMO-RENTREE", PageRequest.of(0, 20));
        verify(couponRepository, never()).countByStatusForBatches(any());
    }

    @Test
    @DisplayName("Revoquer un lot epargne les codes deja revoques, epuises ou expires, et compte chacun a part")
    @SuppressWarnings("unchecked")
    void revokeBatchSkipsUnusableCoupons() {
        Coupon active = coupon("A-ACTIVE", CouponKind.SINGLE_USE, CouponStatus.ACTIVE, 0, 1);
        Coupon multiActive = coupon("B-MULTI", CouponKind.MULTI_USE, CouponStatus.ACTIVE, 1, null);
        Coupon revoked = coupon("C-REVOKED", CouponKind.SINGLE_USE, CouponStatus.REVOKED, 0, 1);
        revoked.setRevokedReason("Motif d'origine");
        Coupon used = coupon("D-USED", CouponKind.SINGLE_USE, CouponStatus.USED, 1, 1);
        Coupon full = coupon("E-FULL", CouponKind.MULTI_USE, CouponStatus.ACTIVE, 3, 3);
        Coupon expired = coupon("F-EXPIRED", CouponKind.SINGLE_USE, CouponStatus.EXPIRED, 0, 1);
        when(couponRepository.findAllByBatchIdOrderByCode(42L))
                .thenReturn(List.of(active, multiActive, revoked, used, full, expired));

        CouponBatchRevocation result = service.revokeBatch("CPB-0001", "Partenariat arrete", "admin@test");

        assertThat(result).isEqualTo(new CouponBatchRevocation("CPB-0001", 6, 2, 1, 2, 1));
        assertThat(active.getStatus()).isEqualTo(CouponStatus.REVOKED);
        assertThat(active.getRevokedReason()).isEqualTo("Partenariat arrete");
        assertThat(active.getRevokedBy()).isEqualTo("admin@test");
        assertThat(active.getRevokedAt()).isNotNull();
        assertThat(multiActive.getStatus()).isEqualTo(CouponStatus.REVOKED);
        assertThat(revoked.getRevokedReason()).isEqualTo("Motif d'origine");
        assertThat(used.getStatus()).isEqualTo(CouponStatus.USED);
        assertThat(full.getStatus()).isEqualTo(CouponStatus.ACTIVE);
        assertThat(expired.getStatus()).isEqualTo(CouponStatus.EXPIRED);

        ArgumentCaptor<List<Coupon>> saved = ArgumentCaptor.forClass(List.class);
        verify(couponRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(active, multiActive);
    }

    @Test
    @DisplayName("Une retenue en cours n'epuise pas un code : un panier abandonne le rendrait")
    void reservationDoesNotExhaust() {
        Coupon held = coupon("A", CouponKind.SINGLE_USE, CouponStatus.ACTIVE, 0, 1);
        held.setReservedCount(1);
        assertThat(CouponBatchService.isExhausted(held)).isFalse();
    }

    @Test
    @DisplayName("Revoquer un lot exige un motif, et un lot inconnu est introuvable")
    void revokeBatchGuards() {
        assertThrows(BadRequestException.class, () -> service.revokeBatch("CPB-0001", " ", "admin"));
        when(batchRepository.findByBatchCode("CPB-9999")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.revokeBatch("CPB-9999", "motif", "admin"));
        assertThrows(ResourceNotFoundException.class, () -> service.view("CPB-9999"));
    }

    @Test
    @DisplayName("L'export CSV porte la marque d'ordre, le point-virgule, et une ligne par code dans l'ordre recu")
    void csvContent() {
        Coupon first = coupon("RENTREE-AAAA", CouponKind.SINGLE_USE, CouponStatus.ACTIVE, 0, 1);
        first.setValidUntil(Instant.parse("2026-12-31T23:59:59Z"));
        first.setIssuedAt(Instant.parse("2026-09-01T08:00:00Z"));
        Coupon second = coupon("RENTREE-BBBB", CouponKind.SINGLE_USE, CouponStatus.REVOKED, 0, 1);
        second.setAssignedToType("MEMBER");
        second.setAssignedToCode("MBR-001");
        second.setRevokedReason("Flyer perdu");
        when(couponRepository.findAllByBatchIdOrderByCode(42L)).thenReturn(List.of(first, second));

        String csv = service.exportCsv("CPB-0001");
        String[] lines = csv.split("\n");

        assertThat(csv).startsWith("﻿Code;Statut;Type;");
        assertThat(lines).hasSize(3);
        assertThat(lines[0].split(";", -1)).hasSize(13);
        assertThat(lines[1]).isEqualTo(
                "\"RENTREE-AAAA\";ACTIVE;SINGLE_USE;;;1;0;0;;2026-12-31T23:59:59Z;2026-09-01T08:00:00Z;;");
        assertThat(lines[2]).startsWith("\"RENTREE-BBBB\";REVOKED;SINGLE_USE;\"MEMBER\";\"MBR-001\";")
                .endsWith(";\"Flyer perdu\"");
    }

    @Test
    @DisplayName("Une cellule CSV double ses guillemets et neutralise ce qu'un tableur prendrait pour une formule")
    void csvEscaping() {
        assertThat(CouponBatchService.cell(null)).isEmpty();
        assertThat(CouponBatchService.cell("dit \"bonjour\"; puis part")).isEqualTo("\"dit \"\"bonjour\"\"; puis part\"");
        assertThat(CouponBatchService.cell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(CouponBatchService.cell("+33")).isEqualTo("\"'+33\"");
        assertThat(CouponBatchService.cell("-1")).isEqualTo("\"'-1\"");
        assertThat(CouponBatchService.cell("@SUM")).isEqualTo("\"'@SUM\"");
    }

    // -----------------------------------------------------------------------------------------

    private static Coupon coupon(String code, CouponKind kind, CouponStatus status, int redemptions, Integer max) {
        return Coupon.builder()
                .code(code)
                .couponKind(kind)
                .status(status)
                .redemptionCount(redemptions)
                .reservedCount(0)
                .maxRedemptions(max)
                .build();
    }

    private static CouponRepository.BatchStatusCount row(Long batchId, CouponStatus status, long count,
                                                         long redemptions, long reserved) {
        return new CouponRepository.BatchStatusCount() {
            @Override public Long getBatchId() { return batchId; }
            @Override public CouponStatus getStatus() { return status; }
            @Override public Number getCouponCount() { return count; }
            @Override public Number getRedemptions() { return redemptions; }
            @Override public Number getReserved() { return reserved; }
        };
    }
}

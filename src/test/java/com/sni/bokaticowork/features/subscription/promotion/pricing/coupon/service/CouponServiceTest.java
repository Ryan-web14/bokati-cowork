package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponReservationStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponReservation;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponReservationRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.CouponRedemptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Admission et retenue d'un code.
 *
 * <p>La règle qui commande tout : un coupon n'est consommé qu'au paiement, jamais à la saisie. Un
 * code à usage unique consommé dès la saisie serait perdu si le panier était abandonné, et le
 * client n'aurait plus rien à montrer.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouponServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Mock private CouponRepository couponRepository;
    @Mock private CouponReservationRepository reservationRepository;
    @Mock private CouponRedemptionRepository redemptionRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private CouponService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "reservationMinutes", 30);
        when(sequenceGenerator.next(anyString())).thenReturn("CPR-0001");
        when(couponRepository.save(any(Coupon.class))).thenAnswer(call -> call.getArgument(0));
        when(reservationRepository.save(any(CouponReservation.class))).thenAnswer(call -> call.getArgument(0));
        when(reservationRepository.findActiveFor(any(), anyString())).thenReturn(Optional.empty());
    }

    // -------------------------------------------------------------------------------------
    // Admission
    // -------------------------------------------------------------------------------------

    @Test
    void acceptsAValidCode() {
        assertNull(service.rejectionReason(coupon(CouponKind.MULTI_USE), "MEMBER", "MEM-1", NOW));
    }

    @Test
    void explainsEachRefusalInFrench() {
        Coupon revoked = coupon(CouponKind.MULTI_USE);
        revoked.setStatus(CouponStatus.REVOKED);
        assertEquals("Ce code a été annulé", service.rejectionReason(revoked, "MEMBER", "MEM-1", NOW));

        Coupon expired = coupon(CouponKind.MULTI_USE);
        expired.setValidUntil(NOW.minusSeconds(1));
        assertEquals("Ce code a expiré", service.rejectionReason(expired, "MEMBER", "MEM-1", NOW));

        Coupon early = coupon(CouponKind.MULTI_USE);
        early.setValidFrom(NOW.plusSeconds(3600));
        assertEquals("Ce code n'est pas encore utilisable", service.rejectionReason(early, "MEMBER", "MEM-1", NOW));
    }

    /**
     * Le message d'un code nominatif présenté par quelqu'un d'autre est volontairement le même que
     * celui d'un code inconnu : dire « ce code ne vous appartient pas » confirmerait son existence
     * à qui le teste.
     */
    @Test
    void hidesTheExistenceOfANamedCouponFromAnyoneElse() {
        Coupon named = coupon(CouponKind.MULTI_USE);
        named.setAssignedToType("MEMBER");
        named.setAssignedToCode("MEM-9");

        assertEquals("Ce code est introuvable", service.rejectionReason(named, "MEMBER", "MEM-1", NOW));
        assertNull(service.rejectionReason(named, "MEMBER", "MEM-9", NOW));
    }

    @Test
    void refusesASecondUseBySomeoneWhoAlreadyUsedIt() {
        Coupon perSubscriber = coupon(CouponKind.UNIQUE_PER_SUBSCRIBER);
        when(redemptionRepository.existsRedemption(anyString(), anyString(), anyString())).thenReturn(true);

        assertEquals("Vous avez déjà utilisé ce code",
                service.rejectionReason(perSubscriber, "MEMBER", "MEM-1", NOW));
    }

    @Test
    void countsReservationsAgainstTheRemainingUses() {
        Coupon single = coupon(CouponKind.SINGLE_USE);
        single.setReservedCount(1);

        // Un code a usage unique deja retenu par un autre panier n'est plus disponible, meme s'il
        // n'a pas encore ete consomme.
        assertEquals(0, single.remainingUses());
        assertEquals("Ce code a atteint sa limite d'utilisation",
                service.rejectionReason(single, "MEMBER", "MEM-1", NOW));
    }

    @Test
    void ignoresAMaxRedemptionValueThatContradictsASingleUseCoupon() {
        Coupon single = coupon(CouponKind.SINGLE_USE);
        single.setMaxRedemptions(10);

        // Le genre prime sur la valeur saisie, sans quoi l'appellation serait mensongere.
        assertEquals(1, single.effectiveMaxRedemptions());
    }

    // -------------------------------------------------------------------------------------
    // Retenue
    // -------------------------------------------------------------------------------------

    @Test
    void reservesWithoutConsuming() {
        Coupon single = coupon(CouponKind.SINGLE_USE);
        when(couponRepository.findByCodeForUpdate("PROMO1")).thenReturn(Optional.of(single));

        CouponReservation reservation = service.reserve("PROMO1", "CART-1", "MEMBER", "MEM-1",
                new BigDecimal("1000"), "XAF", "SYSTEM");

        assertEquals(CouponReservationStatus.RESERVED, reservation.getStatus());
        assertEquals(1, single.getReservedCount());
        // Rien n'est consomme : le code reste utilisable si le panier est abandonne.
        assertEquals(0, single.getRedemptionCount());
        assertEquals(CouponStatus.ACTIVE, single.getStatus());
    }

    @Test
    void refusesToReserveARefusedCodeWithItsReason() {
        Coupon revoked = coupon(CouponKind.MULTI_USE);
        revoked.setStatus(CouponStatus.REVOKED);
        when(couponRepository.findByCodeForUpdate("PROMO1")).thenReturn(Optional.of(revoked));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.reserve("PROMO1", "CART-1", "MEMBER", "MEM-1", null, "XAF", "SYSTEM"));

        assertEquals("Ce code a été annulé", ex.getMessage());
    }

    /** Un rafraîchissement de page ne doit pas empiler les retenues sur le même panier. */
    @Test
    void reusesTheReservationAlreadyHeldByTheSameCart() {
        Coupon coupon = coupon(CouponKind.MULTI_USE);
        CouponReservation existing = CouponReservation.builder()
                .reservationNumber("CPR-0001")
                .coupon(coupon)
                .cartReference("CART-1")
                .status(CouponReservationStatus.RESERVED)
                .expiresAt(NOW)
                .build();
        when(couponRepository.findByCodeForUpdate("PROMO1")).thenReturn(Optional.of(coupon));
        when(reservationRepository.findActiveFor(any(), anyString())).thenReturn(Optional.of(existing));

        service.reserve("PROMO1", "CART-1", "MEMBER", "MEM-1", new BigDecimal("2000"), "XAF", "SYSTEM");

        assertEquals(0, coupon.getReservedCount());
        assertEquals(new BigDecimal("2000"), existing.getDiscountAmount());
    }

    @Test
    void consumesOnlyOnCaptureAndClosesASingleUseCode() {
        Coupon single = coupon(CouponKind.SINGLE_USE);
        single.setReservedCount(1);
        CouponReservation reservation = reservation(single);
        when(reservationRepository.findActiveByCart("CART-1")).thenReturn(List.of(reservation));
        when(couponRepository.findByCodeForUpdate("PROMO1")).thenReturn(Optional.of(single));

        service.capture("CART-1");

        assertEquals(1, single.getRedemptionCount());
        assertEquals(0, single.getReservedCount());
        assertEquals(CouponStatus.USED, single.getStatus());
        assertEquals(CouponReservationStatus.CAPTURED, reservation.getStatus());
        assertNotNull(reservation.getCapturedAt());
    }

    @Test
    void givesTheCodeBackWhenTheCartIsAbandoned() {
        Coupon single = coupon(CouponKind.SINGLE_USE);
        single.setReservedCount(1);
        CouponReservation reservation = reservation(single);
        when(reservationRepository.findActiveByCart("CART-1")).thenReturn(List.of(reservation));
        when(couponRepository.findByCodeForUpdate("PROMO1")).thenReturn(Optional.of(single));

        service.release("CART-1", "Panier modifié");

        assertEquals(0, single.getReservedCount());
        assertEquals(0, single.getRedemptionCount());
        assertEquals(CouponStatus.ACTIVE, single.getStatus());
        assertEquals(CouponReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    void expiresStaleReservationsSoACodeIsNotHeldForever() {
        Coupon coupon = coupon(CouponKind.SINGLE_USE);
        coupon.setReservedCount(1);
        CouponReservation stale = reservation(coupon);
        when(reservationRepository.findExpired(any())).thenReturn(List.of(stale));
        when(couponRepository.findByCodeForUpdate("PROMO1")).thenReturn(Optional.of(coupon));

        assertEquals(1, service.releaseExpired());
        assertEquals(CouponReservationStatus.EXPIRED, stale.getStatus());
        assertEquals(0, coupon.getReservedCount());
        verify(couponRepository).save(coupon);
    }

    // -------------------------------------------------------------------------------------

    private Coupon coupon(CouponKind kind) {
        return Coupon.builder()
                .id(1L)
                .code("PROMO1")
                .couponKind(kind)
                .status(CouponStatus.ACTIVE)
                .redemptionCount(0)
                .reservedCount(0)
                .promotion(Promotion.builder()
                        .code("CAMPAGNE")
                        .name("Campagne test")
                        .status(PromotionStatus.ACTIVE)
                        .discountType(DiscountType.PERCENTAGE)
                        .discountValue(BigDecimal.TEN)
                        .startsAt(NOW.minusSeconds(3600))
                        .consumedBudgetAmount(BigDecimal.ZERO)
                        .build())
                .build();
    }

    private CouponReservation reservation(Coupon coupon) {
        return CouponReservation.builder()
                .reservationNumber("CPR-0001")
                .coupon(coupon)
                .cartReference("CART-1")
                .status(CouponReservationStatus.RESERVED)
                .expiresAt(NOW)
                .build();
    }
}

package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Admission, retenue et consommation d'un code.
 *
 * <p>La regle qui commande tout : <b>un coupon n'est consomme qu'au paiement, jamais a la saisie</b>.
 * Un code a usage unique consomme des la saisie serait perdu si le panier etait abandonne, et le
 * client n'aurait plus rien a montrer. D'ou la retenue, puis la capture ou la liberation, exactement
 * comme pour le stock et le portefeuille.</p>
 *
 * <p>Un refus dit toujours pourquoi, en francais. Un code refuse sans motif renvoie le client vers
 * le support, ou personne ne saura davantage lui repondre.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponReservationRepository reservationRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Value("${bokati.promotion.coupon.reservation-minutes:30}")
    private int reservationMinutes;

    // -----------------------------------------------------------------------------------------
    // Admission
    // -----------------------------------------------------------------------------------------

    /**
     * Verifie qu'un code est utilisable par ce souscripteur, sans rien retenir.
     *
     * @return le motif du refus, ou {@code null} si le code est admissible
     */
    @Transactional(readOnly = true)
    public String rejectionReason(Coupon coupon, String subscriberType, String subscriberCode, Instant now) {
        if (coupon.getStatus() == CouponStatus.REVOKED) {
            return "Ce code a été annulé";
        }
        if (coupon.getStatus() == CouponStatus.USED) {
            return "Ce code a déjà été utilisé";
        }
        if (coupon.getStatus() == CouponStatus.EXPIRED) {
            return "Ce code a expiré";
        }
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            return "Ce code n'est pas encore utilisable";
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            return "Ce code a expiré";
        }

        Promotion promotion = coupon.getPromotion();
        if (promotion == null || promotion.getStatus() != PromotionStatus.ACTIVE) {
            return "La promotion liée à ce code n'est pas active";
        }
        if (promotion.getStartsAt() != null && now.isBefore(promotion.getStartsAt())) {
            return "La promotion liée à ce code n'a pas encore commencé";
        }
        if (promotion.getEndsAt() != null && now.isAfter(promotion.getEndsAt())) {
            return "La promotion liée à ce code est terminée";
        }
        BigDecimal remainingBudget = promotion.remainingBudget();
        if (remainingBudget != null && remainingBudget.signum() <= 0) {
            return "Le budget de cette promotion est épuisé";
        }

        // Un coupon nominatif n'existe que pour son titulaire. Le message reste volontairement le
        // meme que pour un code inconnu : dire « ce code ne vous appartient pas » confirmerait a
        // qui le teste que le code existe.
        if (StringUtils.hasText(coupon.getAssignedToCode())
                && !coupon.getAssignedToCode().equalsIgnoreCase(subscriberCode)) {
            return "Ce code est introuvable";
        }

        Integer remaining = coupon.remainingUses();
        if (remaining != null && remaining <= 0) {
            return "Ce code a atteint sa limite d'utilisation";
        }

        if (coupon.getCouponKind() == CouponKind.UNIQUE_PER_SUBSCRIBER
                && StringUtils.hasText(subscriberCode)
                && redemptionRepository.existsRedemption(promotion.getCode(), subscriberType, subscriberCode)) {
            return "Vous avez déjà utilisé ce code";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Optional<Coupon> find(String code) {
        return StringUtils.hasText(code) ? couponRepository.findByCode(code.trim()) : Optional.empty();
    }

    /** Codes dont un abonné dispose aujourd'hui, pour les lui montrer avant qu'il ait à en saisir un. */
    @Transactional(readOnly = true)
    public List<Coupon> availableFor(String subscriberType, String subscriberCode) {
        return couponRepository.findAvailableFor(subscriberType, subscriberCode, Instant.now());
    }

    // -----------------------------------------------------------------------------------------
    // Retenue
    // -----------------------------------------------------------------------------------------

    /**
     * Retient un code pour un panier. Le verrou exclusif est pris avant toute verification : lire
     * puis verrouiller laisserait passer les deux paniers que le verrou est cense departager.
     */
    @Transactional
    public CouponReservation reserve(String code,
                                     String cartReference,
                                     String subscriberType,
                                     String subscriberCode,
                                     BigDecimal discountAmount,
                                     String currency,
                                     String reservedBy) {
        Coupon coupon = couponRepository.findByCodeForUpdate(requireText(code, "Code requis"))
                .orElseThrow(() -> new ResourceNotFoundException("Ce code est introuvable"));

        Optional<CouponReservation> existing = reservationRepository.findActiveFor(coupon.getId(), cartReference);
        if (existing.isPresent()) {
            // Un rafraichissement de page ne doit pas empiler les retenues : le meme panier garde
            // la sienne, avec le montant reactualise.
            CouponReservation reservation = existing.get();
            reservation.setDiscountAmount(discountAmount);
            reservation.setExpiresAt(Instant.now().plus(Duration.ofMinutes(reservationMinutes)));
            return reservationRepository.save(reservation);
        }

        String reason = rejectionReason(coupon, subscriberType, subscriberCode, Instant.now());
        if (reason != null) {
            throw new BadRequestException(reason);
        }

        coupon.setReservedCount(nonNull(coupon.getReservedCount()) + 1);
        couponRepository.save(coupon);

        return reservationRepository.save(CouponReservation.builder()
                .reservationNumber(sequenceGenerator.next("coupon_reservation"))
                .coupon(coupon)
                .cartReference(requireText(cartReference, "Référence de panier requise"))
                .subscriberType(subscriberType)
                .subscriberCode(subscriberCode)
                .discountAmount(discountAmount)
                .currency(currency)
                .status(CouponReservationStatus.RESERVED)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(reservationMinutes)))
                .reservedBy(reservedBy)
                .build());
    }

    /** Consomme les codes retenus par un panier. Appelé au paiement, jamais avant. */
    @Transactional
    public List<CouponReservation> capture(String cartReference) {
        List<CouponReservation> reservations = reservationRepository.findActiveByCart(cartReference);
        reservations.forEach(reservation -> {
            Coupon coupon = couponRepository.findByCodeForUpdate(reservation.getCoupon().getCode())
                    .orElse(reservation.getCoupon());
            coupon.setReservedCount(Math.max(0, nonNull(coupon.getReservedCount()) - 1));
            coupon.setRedemptionCount(nonNull(coupon.getRedemptionCount()) + 1);

            Integer ceiling = coupon.effectiveMaxRedemptions();
            if (ceiling != null && coupon.getRedemptionCount() >= ceiling) {
                coupon.setStatus(CouponStatus.USED);
            }
            couponRepository.save(coupon);

            reservation.setStatus(CouponReservationStatus.CAPTURED);
            reservation.setCapturedAt(Instant.now());
            reservationRepository.save(reservation);
        });
        return reservations;
    }

    /**
     * Consomme un code au moment ou la remise est appliquee a un document.
     *
     * <p>Si l'abonne avait retenu le code dans un panier, c'est cette retenue qui est capturee ·
     * sinon le code est retenu et capture dans le meme geste, sous la reference du document. Sans
     * cela, un code a usage unique applique a une souscription resterait utilisable.</p>
     */
    @Transactional
    public void consume(String code, String subscriberType, String subscriberCode, BigDecimal discountAmount,
                        String currency, String documentReference, String actor) {
        Coupon coupon = find(code).orElse(null);
        if (coupon == null) {
            return;
        }
        List<CouponReservation> held = reservationRepository.findActiveForSubscriber(coupon.getId(), subscriberType, subscriberCode);
        String cartReference = held.isEmpty()
                ? reserve(code, documentReference, subscriberType, subscriberCode, discountAmount, currency, actor).getCartReference()
                : held.getFirst().getCartReference();
        capture(cartReference);
    }

    /** Rend les codes retenus par un panier abandonné ou modifié. */
    @Transactional
    public int release(String cartReference, String reason) {
        List<CouponReservation> reservations = reservationRepository.findActiveByCart(cartReference);
        reservations.forEach(reservation -> releaseOne(reservation, reason, CouponReservationStatus.RELEASED));
        return reservations.size();
    }

    /** Rend les retenues arrivées à échéance. Appelé par un traitement de fond. */
    @Transactional
    public int releaseExpired() {
        List<CouponReservation> expired = reservationRepository.findExpired(Instant.now());
        expired.forEach(reservation ->
                releaseOne(reservation, "Panier abandonné", CouponReservationStatus.EXPIRED));
        return expired.size();
    }

    // -----------------------------------------------------------------------------------------

    private void releaseOne(CouponReservation reservation, String reason, CouponReservationStatus status) {
        Coupon coupon = couponRepository.findByCodeForUpdate(reservation.getCoupon().getCode())
                .orElse(reservation.getCoupon());
        coupon.setReservedCount(Math.max(0, nonNull(coupon.getReservedCount()) - 1));
        couponRepository.save(coupon);

        reservation.setStatus(status);
        reservation.setReleasedAt(Instant.now());
        reservation.setReleaseReason(reason);
        reservationRepository.save(reservation);
    }

    private int nonNull(Integer value) {
        return value == null ? 0 : value;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}

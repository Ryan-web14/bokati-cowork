package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponBatchRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generation de codes en lot.
 *
 * <p>Mille codes uniques pour un partenariat, imprimes sur des flyers, avec un suivi du taux
 * d'utilisation par lot. Le besoin est concret, et il impose deux choses que la generation naive
 * ne donne pas.</p>
 *
 * <p><b>Un alphabet sans ambiguite.</b> Un code lu sur un flyer est recopie a la main. Le zero et le
 * O, le un et le I ou le L se confondent a l'impression, et chaque confusion devient un appel au
 * support. Ils sont donc absents de l'alphabet.</p>
 *
 * <p><b>Une generation aleatoire et non sequentielle.</b> Des codes qui se suivent se devinent :
 * qui detient un flyer detient toute la campagne.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponBatchService {

    /** Ni O, ni I, ni L, ni 0, ni 1 · ce qui se confond a la lecture se confond a la saisie. */
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private static final int MAX_BATCH_SIZE = 50_000;
    private static final int MAX_ATTEMPTS_PER_CODE = 12;

    private final PromotionRepository promotionRepository;
    private final CouponBatchRepository batchRepository;
    private final CouponRepository couponRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public CouponBatch generate(String promotionCode,
                                String name,
                                int quantity,
                                CouponKind couponKind,
                                String codePrefix,
                                int codeLength,
                                Integer maxRedemptionsPerCoupon,
                                Instant validFrom,
                                Instant validUntil,
                                String channel,
                                String createdBy) {
        if (quantity <= 0 || quantity > MAX_BATCH_SIZE) {
            throw new BadRequestException("La quantité doit être comprise entre 1 et " + MAX_BATCH_SIZE);
        }
        if (codeLength < 6 || codeLength > 32) {
            throw new BadRequestException("La longueur d'un code doit être comprise entre 6 et 32");
        }
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new BadRequestException("La fin de validité précède son début");
        }
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(promotionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion " + promotionCode + " introuvable"));

        CouponBatch batch = batchRepository.save(CouponBatch.builder()
                .batchCode(sequenceGenerator.next("coupon_batch"))
                .promotion(promotion)
                .name(name)
                .requestedQuantity(quantity)
                .codePrefix(StringUtils.hasText(codePrefix) ? codePrefix.trim().toUpperCase() : null)
                .codeLength(codeLength)
                .couponKind(couponKind)
                .maxRedemptionsPerCoupon(maxRedemptionsPerCoupon)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .channel(channel)
                .createdBy(createdBy)
                .build());

        List<Coupon> coupons = new ArrayList<>(quantity);
        Set<String> produced = new HashSet<>(quantity * 2);
        for (int index = 0; index < quantity; index++) {
            String code = uniqueCode(batch, produced);
            coupons.add(Coupon.builder()
                    .code(code)
                    .promotion(promotion)
                    .batch(batch)
                    .couponKind(couponKind)
                    .maxRedemptions(couponKind == CouponKind.SINGLE_USE ? 1 : maxRedemptionsPerCoupon)
                    .validFrom(validFrom)
                    .validUntil(validUntil)
                    .status(CouponStatus.ACTIVE)
                    .issuedBy(createdBy)
                    .build());
        }
        couponRepository.saveAll(coupons);

        batch.setGeneratedQuantity(coupons.size());
        log.info("Lot {} · {} codes generes pour la promotion {}", batch.getBatchCode(), coupons.size(), promotionCode);
        return batchRepository.save(batch);
    }

    /**
     * Revoque un code. Il n'est pas supprime : un code revoque doit pouvoir etre reconnu comme tel
     * lorsque quelqu'un le presente, avec le motif qui a conduit a l'annuler.
     */
    @Transactional
    public Coupon revoke(String code, String reason, String revokedBy) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Code " + code + " introuvable"));
        coupon.setStatus(CouponStatus.REVOKED);
        coupon.setRevokedAt(Instant.now());
        coupon.setRevokedReason(reason);
        coupon.setRevokedBy(revokedBy);
        return couponRepository.save(coupon);
    }

    // -----------------------------------------------------------------------------------------

    private String uniqueCode(CouponBatch batch, Set<String> produced) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_CODE; attempt++) {
            String candidate = compose(batch);
            if (produced.add(candidate) && !couponRepository.existsByCode(candidate)) {
                return candidate;
            }
            produced.remove(candidate);
        }
        // Douze tirages infructueux signifient que l'espace est sature pour cette longueur. Allonger
        // le code en silence donnerait des codes de tailles differentes dans un meme lot.
        throw new BadRequestException(
                "Impossible de générer un code unique · augmentez la longueur ou changez le préfixe");
    }

    private String compose(CouponBatch batch) {
        StringBuilder code = new StringBuilder();
        if (StringUtils.hasText(batch.getCodePrefix())) {
            code.append(batch.getCodePrefix()).append('-');
        }
        for (int index = 0; index < batch.getCodeLength(); index++) {
            code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }
}

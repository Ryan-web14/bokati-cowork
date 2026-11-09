package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponBatchRevocation;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponBatchView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponLineView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponBatchRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        markRevoked(coupon, reason, revokedBy, Instant.now());
        return couponRepository.save(coupon);
    }

    /**
     * Revoque d'un geste les codes encore utilisables d'un lot, typiquement quand un partenariat
     * s'arrete ou qu'un paquet de flyers est perdu.
     *
     * <p>Les codes deja revoques gardent leur motif d'origine, et ceux qui ont servi jusqu'au bout
     * ou qui ont expire sont laisses tels quels : les revoquer reecrirait l'histoire de la campagne,
     * et un code consomme doit rester reconnu comme consomme.</p>
     */
    @Transactional
    public CouponBatchRevocation revokeBatch(String batchCode, String reason, String revokedBy) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Le motif de la révocation est obligatoire");
        }
        CouponBatch batch = batch(batchCode);
        List<Coupon> coupons = couponRepository.findAllByBatchIdOrderByCode(batch.getId());
        Instant now = Instant.now();
        int revoked = 0;
        int alreadyRevoked = 0;
        int exhausted = 0;
        int expired = 0;
        List<Coupon> changed = new ArrayList<>();
        for (Coupon coupon : coupons) {
            if (coupon.getStatus() == CouponStatus.REVOKED) {
                alreadyRevoked++;
            } else if (isExhausted(coupon)) {
                exhausted++;
            } else if (coupon.getStatus() == CouponStatus.EXPIRED) {
                expired++;
            } else {
                markRevoked(coupon, reason, revokedBy, now);
                changed.add(coupon);
                revoked++;
            }
        }
        couponRepository.saveAll(changed);
        log.info("Lot {} · {} codes revoques, {} deja revoques, {} epuises, {} expires",
                batch.getBatchCode(), revoked, alreadyRevoked, exhausted, expired);
        return new CouponBatchRevocation(batch.getBatchCode(), coupons.size(), revoked, alreadyRevoked, exhausted, expired);
    }

    // -----------------------------------------------------------------------------------------
    // Consultation
    // -----------------------------------------------------------------------------------------

    /** Les lots, du plus recent au plus ancien, chacun avec ses compteurs vivants. */
    @Transactional(readOnly = true)
    public Page<CouponBatchView> list(String promotionCode, Pageable pageable) {
        Page<CouponBatch> batches = StringUtils.hasText(promotionCode)
                ? batchRepository.findRecentByPromotionCode(promotionCode.trim(), pageable)
                : batchRepository.findRecent(pageable);
        Map<Long, CouponBatchView.Counts> counts = counts(batches.getContent().stream().map(CouponBatch::getId).toList());
        return batches.map(batch -> CouponBatchView.of(batch, counts.get(batch.getId())));
    }

    /** Un lot et ou en est sa campagne, lu dans la transaction qui charge sa promotion. */
    @Transactional(readOnly = true)
    public CouponBatchView view(String batchCode) {
        CouponBatch batch = batch(batchCode);
        return CouponBatchView.of(batch, counts(List.of(batch.getId())).get(batch.getId()));
    }

    /** Les codes d'un lot, page par page, eventuellement restreints a un statut. */
    @Transactional(readOnly = true)
    public Page<CouponLineView> coupons(String batchCode, CouponStatus status, Pageable pageable) {
        CouponBatch batch = batch(batchCode);
        Page<Coupon> page = status == null
                ? couponRepository.findPageByBatchId(batch.getId(), pageable)
                : couponRepository.findPageByBatchIdAndStatus(batch.getId(), status, pageable);
        return page.map(CouponLineView::of);
    }

    /**
     * Tous les codes d'un lot en CSV, pour les remettre a un partenaire ou a l'imprimeur.
     *
     * <p>Point-virgule et marque d'ordre en tete, comme les autres exports : c'est ce qu'Excel
     * attend en francais pour ouvrir le fichier sans assistant d'import.</p>
     */
    @Transactional(readOnly = true)
    public String exportCsv(String batchCode) {
        CouponBatch batch = batch(batchCode);
        return toCsv(couponRepository.findAllByBatchIdOrderByCode(batch.getId()));
    }

    static String toCsv(List<Coupon> coupons) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Code;Statut;Type;Titulaire type;Titulaire code;Utilisations max;Utilisations;Retenues;"
                + "Valide du;Valide au;Emis le;Revoque le;Motif de revocation\n");
        for (Coupon coupon : coupons) {
            csv.append(cell(coupon.getCode())).append(';')
                    .append(value(coupon.getStatus())).append(';')
                    .append(value(coupon.getCouponKind())).append(';')
                    .append(cell(coupon.getAssignedToType())).append(';')
                    .append(cell(coupon.getAssignedToCode())).append(';')
                    .append(value(coupon.effectiveMaxRedemptions())).append(';')
                    .append(value(coupon.getRedemptionCount())).append(';')
                    .append(value(coupon.getReservedCount())).append(';')
                    .append(value(coupon.getValidFrom())).append(';')
                    .append(value(coupon.getValidUntil())).append(';')
                    .append(value(coupon.getIssuedAt())).append(';')
                    .append(value(coupon.getRevokedAt())).append(';')
                    .append(cell(coupon.getRevokedReason())).append('\n');
        }
        return csv.toString();
    }

    /** Une cellule CSV · guillemets doubles, et pas de formule interpretable par un tableur. */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    // -----------------------------------------------------------------------------------------

    private CouponBatch batch(String batchCode) {
        if (!StringUtils.hasText(batchCode)) {
            throw new ResourceNotFoundException("Lot introuvable");
        }
        return batchRepository.findByBatchCode(batchCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Lot " + batchCode + " introuvable"));
    }

    /** La regle d'une revocation, la meme pour un code seul et pour un lot entier. */
    private static void markRevoked(Coupon coupon, String reason, String revokedBy, Instant now) {
        coupon.setStatus(CouponStatus.REVOKED);
        coupon.setRevokedAt(now);
        coupon.setRevokedReason(reason);
        coupon.setRevokedBy(revokedBy);
    }

    /**
     * Un code a servi jusqu'au bout. Les retenues en cours ne comptent pas : un panier abandonne
     * rendra son code, qui doit donc pouvoir etre revoque.
     */
    static boolean isExhausted(Coupon coupon) {
        if (coupon.getStatus() == CouponStatus.USED) {
            return true;
        }
        Integer ceiling = coupon.effectiveMaxRedemptions();
        return ceiling != null && coupon.getRedemptionCount() != null && coupon.getRedemptionCount() >= ceiling;
    }

    private Map<Long, CouponBatchView.Counts> counts(List<Long> batchIds) {
        Map<Long, CouponBatchView.Counts> result = new HashMap<>();
        if (batchIds.isEmpty()) {
            return result;
        }
        for (CouponRepository.BatchStatusCount row : couponRepository.countByStatusForBatches(batchIds)) {
            CouponBatchView.Counts current = result.getOrDefault(row.getBatchId(), CouponBatchView.Counts.empty());
            Map<CouponStatus, Long> byStatus = current.byStatus();
            byStatus.merge(row.getStatus(), longOf(row.getCouponCount()), Long::sum);
            result.put(row.getBatchId(), new CouponBatchView.Counts(byStatus,
                    current.redemptions() + longOf(row.getRedemptions()),
                    current.reserved() + longOf(row.getReserved())));
        }
        return result;
    }

    private static long longOf(Number number) {
        return number == null ? 0L : number.longValue();
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

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

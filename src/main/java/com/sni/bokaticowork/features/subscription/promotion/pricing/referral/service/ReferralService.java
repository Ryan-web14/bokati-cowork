package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service.PromotionBeneficiaryService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionReward;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralQualificationRule;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.Referral;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralProgram;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralLinkRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralProgramRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.repository.PromotionRewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

/**
 * Parrainage · enregistrement, qualification, recompense.
 *
 * <p>Une regle commande tout le reste : <b>un parrainage se qualifie, il ne se declare pas</b>.
 * Entre l'enregistrement et la qualification, rien n'est du. Recompenser des l'inscription revient a
 * payer pour des comptes crees et jamais utilises, lesquels sont precisement ce qu'attire un
 * programme qui paie trop tot.</p>
 *
 * <p>Les recompenses reutilisent les {@code PromotionReward} des deux promotions porteuses. Un
 * credit de portefeuille est verse immediatement ; toute autre recompense est posee comme un droit
 * nominatif sur sa promotion, que le moteur de tarification appliquera au prochain achat. La
 * distinction est ce qui evite d'ecrire un second moteur.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    /** Sans O, I, L, 0 ni 1 · un code de parrainage se dicte au telephone. */
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;
    private static final int MAX_CODE_ATTEMPTS = 12;

    private final ReferralProgramRepository programRepository;
    private final ReferralLinkRepository linkRepository;
    private final ReferralRepository referralRepository;
    private final PromotionRewardRepository rewardRepository;
    private final PromotionBeneficiaryService beneficiaryService;
    private final WalletService walletService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SecureRandom random = new SecureRandom();

    // -----------------------------------------------------------------------------------------
    // Lien de parrainage
    // -----------------------------------------------------------------------------------------

    /**
     * Rend le lien d'un parrain, en le creant au besoin. Un parrain n'en a qu'un par programme :
     * lui en donner plusieurs eclaterait ses statistiques et rendrait son plafond contournable.
     */
    @Transactional
    public ReferralLink linkFor(String programCode, String referrerType, String referrerCode) {
        ReferralProgram program = program(programCode);
        return linkRepository.findForReferrer(program.getId(), referrerType, referrerCode)
                .orElseGet(() -> linkRepository.save(ReferralLink.builder()
                        .program(program)
                        .referrerType(referrerType)
                        .referrerCode(referrerCode)
                        .code(uniqueCode())
                        .build()));
    }

    @Transactional
    public void recordClick(String linkCode) {
        linkRepository.findByCode(linkCode).ifPresent(link -> {
            link.setClickCount(link.getClickCount() + 1);
            linkRepository.save(link);
        });
    }

    // -----------------------------------------------------------------------------------------
    // Enregistrement
    // -----------------------------------------------------------------------------------------

    /**
     * Enregistre un parrainage a partir du code presente par le filleul. Rien n'est accorde ici.
     */
    @Transactional
    public Referral register(String linkCode, String refereeType, String refereeCode) {
        ReferralLink link = linkRepository.findByCode(requireText(linkCode, "Code de parrainage requis"))
                .orElseThrow(() -> new ResourceNotFoundException("Ce code de parrainage est introuvable"));
        if (!Boolean.TRUE.equals(link.getActive())) {
            throw new BadRequestException("Ce code de parrainage n'est plus actif");
        }

        ReferralProgram program = link.getProgram();
        if (!program.openAt(Instant.now())) {
            throw new BadRequestException("Ce programme de parrainage n'est pas ouvert");
        }
        if (link.getReferrerCode().equalsIgnoreCase(refereeCode)
                && link.getReferrerType().equalsIgnoreCase(refereeType)) {
            throw new BadRequestException("On ne peut pas se parrainer soi-même");
        }
        referralRepository.findByReferee(program.getId(), refereeType, refereeCode).ifPresent(existing -> {
            throw new BadRequestException("Ce bénéficiaire a déjà été parrainé sur ce programme");
        });

        Referral referral = referralRepository.save(Referral.builder()
                .referralNumber(sequenceGenerator.next("referral"))
                .program(program)
                .link(link)
                .referrerType(link.getReferrerType())
                .referrerCode(link.getReferrerCode())
                .refereeType(refereeType)
                .refereeCode(refereeCode)
                .status(ReferralStatus.PENDING)
                .currency(program.getCurrency())
                .build());

        link.setSignupCount(link.getSignupCount() + 1);
        linkRepository.save(link);

        // Un programme qui recompense des l'inscription qualifie immediatement · c'est son choix,
        // et il est assume au niveau du programme, pas decide ici.
        if (program.getQualificationRule() == ReferralQualificationRule.SIGNUP) {
            return qualify(referral);
        }
        return referral;
    }

    // -----------------------------------------------------------------------------------------
    // Qualification
    // -----------------------------------------------------------------------------------------

    /**
     * Qualifie les parrainages en attente d'un filleul qui vient de regler. Appele par le circuit de
     * paiement · c'est l'evenement qui prouve que le parrainage valait quelque chose.
     */
    @Transactional
    public int qualifyOnFirstPayment(String refereeType, String refereeCode) {
        List<Referral> pending = referralRepository.findPendingForReferee(refereeType, refereeCode);
        int qualified = 0;
        for (Referral referral : pending) {
            if (referral.getProgram().getQualificationRule() == ReferralQualificationRule.FIRST_PAYMENT) {
                qualify(referral);
                qualified++;
            }
        }
        return qualified;
    }

    /** Qualifie les parrainages dont l'anciennete exigee est atteinte. Appele par un traitement de fond. */
    @Transactional
    public int qualifyDueByTenure(int limit) {
        List<Referral> due = referralRepository.findDueByTenure(Instant.now(), limit);
        due.forEach(this::qualify);
        return due.size();
    }

    /**
     * Qualifie un parrainage et accorde les deux recompenses.
     *
     * <p>Le plafond du parrain ne fait pas echouer la qualification : le filleul a rempli sa part et
     * garde son avantage, seule la recompense du parrain est retenue. Refuser les deux punirait
     * quelqu'un pour la popularite d'un autre.</p>
     */
    @Transactional
    public Referral qualify(Referral referral) {
        if (referral.getStatus() != ReferralStatus.PENDING) {
            return referral;
        }
        ReferralProgram program = referral.getProgram();

        referral.setStatus(ReferralStatus.QUALIFIED);
        referral.setQualifiedAt(Instant.now());

        grant(referral, program.getRefereePromotion(), referral.getRefereeType(), referral.getRefereeCode(), false);

        if (referrerWithinCap(program, referral)) {
            grant(referral, program.getReferrerPromotion(), referral.getReferrerType(), referral.getReferrerCode(), true);
        } else {
            log.info("Parrainage {} · plafond atteint pour le parrain {}, sa recompense n'est pas accordee",
                    referral.getReferralNumber(), referral.getReferrerCode());
        }

        if (Boolean.TRUE.equals(referral.getRefereeRewardGranted())
                || Boolean.TRUE.equals(referral.getReferrerRewardGranted())) {
            referral.setStatus(ReferralStatus.REWARDED);
        }
        return referralRepository.save(referral);
    }

    @Transactional
    public Referral reject(String referralNumber, String reason) {
        Referral referral = referralRepository.findByNumber(referralNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Parrainage " + referralNumber + " introuvable"));
        if (referral.getStatus() == ReferralStatus.REWARDED) {
            throw new BadRequestException("Ce parrainage a déjà été récompensé, il ne peut plus être écarté");
        }
        referral.setStatus(ReferralStatus.REJECTED);
        referral.setRejectedAt(Instant.now());
        referral.setRejectionReason(reason);
        return referralRepository.save(referral);
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Accorde une recompense.
     *
     * <p>Deux natures, deux chemins. Un credit de portefeuille est verse tout de suite, c'est de
     * l'argent. Toute autre recompense devient un droit nominatif sur la promotion porteuse, que le
     * moteur appliquera au prochain achat · c'est ce qui evite d'ecrire un second moteur pour dire
     * la meme chose.</p>
     */
    private void grant(Referral referral, Promotion promotion, String ownerType, String ownerCode, boolean referrer) {
        if (promotion == null) {
            return;
        }
        List<PromotionReward> rewards = rewardRepository.findAllByPromotionId(promotion.getId());
        if (rewards.isEmpty()) {
            return;
        }

        BigDecimal walletCredit = rewards.stream()
                .filter(reward -> reward.getRewardType() == RewardType.WALLET_CREDIT)
                .map(reward -> reward.getValue() == null ? BigDecimal.ZERO : reward.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (walletCredit.signum() > 0) {
            creditWallet(referral, ownerType, ownerCode, walletCredit);
        }

        boolean hasOtherReward = rewards.stream()
                .anyMatch(reward -> reward.getRewardType() != RewardType.WALLET_CREDIT);
        if (hasOtherReward) {
            beneficiaryService.add(promotion.getCode(), ownerType, ownerCode,
                    "Parrainage " + referral.getReferralNumber(), "SYSTEM");
        }

        if (referrer) {
            referral.setReferrerRewardGranted(Boolean.TRUE);
            referral.setReferrerRewardAmount(walletCredit);
            referral.setReferrerRewardedAt(Instant.now());
        } else {
            referral.setRefereeRewardGranted(Boolean.TRUE);
            referral.setRefereeRewardAmount(walletCredit);
            referral.setRefereeRewardedAt(Instant.now());
        }
    }

    private void creditWallet(Referral referral, String ownerType, String ownerCode, BigDecimal amount) {
        String currency = StringUtils.hasText(referral.getCurrency()) ? referral.getCurrency() : "XAF";
        WalletResponse wallet = walletService.getOrCreate(ownerType, ownerCode, currency);
        WalletAccount account = walletService.serviceWallet(wallet.walletNumber());
        // Cle d'idempotence par beneficiaire : rejouer une qualification ne credite pas deux fois.
        walletService.credit(account, amount, WalletEntryType.PROMOTIONAL_CREDIT,
                "REFERRAL", referral.getReferralNumber(), referral.getReferralNumber(), "SYSTEM",
                "REFERRAL:" + referral.getReferralNumber() + ":" + ownerCode);
    }

    private boolean referrerWithinCap(ReferralProgram program, Referral referral) {
        if (program.getMaxReferralsPerReferrer() == null) {
            return true;
        }
        long counted = referralRepository.countAgainstCap(
                program.getId(), referral.getReferrerType(), referral.getReferrerCode());
        // Le parrainage courant est deja enregistre, donc deja compte.
        return counted <= program.getMaxReferralsPerReferrer();
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int index = 0; index < CODE_LENGTH; index++) {
                code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            String candidate = code.toString();
            if (!linkRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Impossible de générer un code de parrainage unique");
    }

    private ReferralProgram program(String programCode) {
        return programRepository.findByCode(programCode)
                .orElseThrow(() -> new ResourceNotFoundException("Programme " + programCode + " introuvable"));
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}

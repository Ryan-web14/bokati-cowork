package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model.PromotionBeneficiary;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository.PromotionBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tient la liste des beneficiaires nommes d'une promotion.
 *
 * <p>Trois exigences guident ce service, et chacune vient d'un probleme concret.</p>
 *
 * <p><b>Le retrait ne supprime pas.</b> Un geste accorde par erreur doit pouvoir etre annule, mais
 * la ligne reste : qui l'avait accorde, pourquoi, et qui l'a retire. Une ligne effacee ne repond a
 * aucune de ces questions le jour ou le client rappelle.</p>
 *
 * <p><b>Un import rend compte ligne a ligne.</b> Importer quarante invites et n'apprendre que
 * « trente-sept acceptes » oblige a chercher les trois autres a la main. Le rapport dit lesquels et
 * pourquoi, sur le modele de l'import du module inventaire.</p>
 *
 * <p><b>La notification est tracee.</b> Sans cela, un client reclamera une offre qu'il n'a jamais
 * recue, et personne ne pourra ni le confirmer ni le dementir.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionBeneficiaryService {

    private static final int MAX_IMPORT_SIZE = 10_000;

    private final PromotionRepository promotionRepository;
    private final PromotionBeneficiaryRepository beneficiaryRepository;

    /**
     * @param line    numero de ligne dans la source, pour que l'appelant retrouve la sienne
     * @param outcome ce qui a ete fait, ou la raison du refus
     */
    public record ImportLine(int line, String subscriberType, String subscriberCode, String outcome, boolean accepted) {
    }

    public record ImportReport(String promotionCode, int submitted, int accepted, int rejected, List<ImportLine> lines) {
    }

    // -----------------------------------------------------------------------------------------

    @Transactional
    public PromotionBeneficiary add(String promotionCode,
                                    String subscriberType,
                                    String subscriberCode,
                                    String reason,
                                    String addedBy) {
        Promotion promotion = promotion(promotionCode);
        return upsert(promotion, subscriberType, subscriberCode, reason, addedBy);
    }

    /**
     * Importe une liste. Chaque ligne est traitee pour elle-meme : une ligne refusee n'empeche pas
     * les autres d'entrer, sans quoi un fichier de quarante noms echouerait en entier a cause d'un
     * seul code mal saisi.
     */
    @Transactional
    public ImportReport importAll(String promotionCode, List<String[]> rows, String reason, String addedBy) {
        if (rows == null || rows.isEmpty()) {
            throw new BadRequestException("La liste à importer est vide");
        }
        if (rows.size() > MAX_IMPORT_SIZE) {
            throw new BadRequestException("Un import ne peut dépasser " + MAX_IMPORT_SIZE + " lignes");
        }
        Promotion promotion = promotion(promotionCode);

        List<ImportLine> report = new ArrayList<>(rows.size());
        Set<String> seen = new HashSet<>();
        int accepted = 0;

        for (int index = 0; index < rows.size(); index++) {
            int lineNumber = index + 1;
            String[] row = rows.get(index);
            if (row == null || row.length < 2
                    || !StringUtils.hasText(row[0]) || !StringUtils.hasText(row[1])) {
                report.add(new ImportLine(lineNumber, null, null,
                        "Type et code de souscripteur requis", false));
                continue;
            }
            String type = row[0].trim();
            String code = row[1].trim();

            // Un doublon dans le fichier lui-meme n'est pas une erreur de saisie a signaler comme
            // un rejet du systeme, mais il ne doit pas produire deux lignes.
            if (!seen.add((type + "|" + code).toLowerCase())) {
                report.add(new ImportLine(lineNumber, type, code, "Doublon dans le fichier", false));
                continue;
            }
            try {
                upsert(promotion, type, code, reason, addedBy);
                accepted++;
                report.add(new ImportLine(lineNumber, type, code, "Ajouté", true));
            } catch (Exception ex) {
                report.add(new ImportLine(lineNumber, type, code, ex.getMessage(), false));
            }
        }

        log.info("Import de beneficiaires sur {} · {} acceptees, {} refusees",
                promotionCode, accepted, rows.size() - accepted);
        return new ImportReport(promotionCode, rows.size(), accepted, rows.size() - accepted, List.copyOf(report));
    }

    /**
     * Retire un beneficiaire. Refuse apres utilisation : ce qui a ete consomme ne se reprend pas, et
     * le pretendre ferait diverger la liste de ce qui a reellement ete accorde.
     */
    @Transactional
    public PromotionBeneficiary revoke(String promotionCode, String subscriberType, String subscriberCode,
                                       String reason, String revokedBy) {
        PromotionBeneficiary beneficiary = require(promotionCode, subscriberType, subscriberCode);
        if (beneficiary.getRedeemedAt() != null) {
            throw new BadRequestException("Ce bénéficiaire a déjà utilisé la promotion, elle ne peut plus être retirée");
        }
        beneficiary.setRevokedAt(Instant.now());
        beneficiary.setRevokedBy(revokedBy);
        beneficiary.setRevokedReason(reason);
        return beneficiaryRepository.save(beneficiary);
    }

    @Transactional
    public PromotionBeneficiary markNotified(String promotionCode, String subscriberType, String subscriberCode,
                                             String channel) {
        PromotionBeneficiary beneficiary = require(promotionCode, subscriberType, subscriberCode);
        beneficiary.setNotifiedAt(Instant.now());
        beneficiary.setNotificationChannel(channel);
        return beneficiaryRepository.save(beneficiary);
    }

    @Transactional
    public void markRedeemed(String promotionCode, String subscriberType, String subscriberCode) {
        beneficiaryRepository.find(promotion(promotionCode).getId(), subscriberType, subscriberCode)
                .ifPresent(beneficiary -> {
                    if (beneficiary.getRedeemedAt() == null) {
                        beneficiary.setRedeemedAt(Instant.now());
                        beneficiaryRepository.save(beneficiary);
                    }
                });
    }

    /** Taux d'utilisation d'une campagne nominative · ce que le partenariat a reellement produit. */
    @Transactional(readOnly = true)
    public Usage usage(String promotionCode) {
        Long promotionId = promotion(promotionCode).getId();
        long active = beneficiaryRepository.countActive(promotionId);
        long redeemed = beneficiaryRepository.countRedeemed(promotionId);
        return new Usage(promotionCode, active, redeemed);
    }

    public record Usage(String promotionCode, long active, long redeemed) {
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Ajoute, ou reactive un beneficiaire precedemment retire. Reactiver plutot que creer une
     * seconde ligne conserve l'histoire du premier geste et respecte l'unicite en base.
     */
    private PromotionBeneficiary upsert(Promotion promotion, String subscriberType, String subscriberCode,
                                        String reason, String addedBy) {
        return beneficiaryRepository.find(promotion.getId(), subscriberType, subscriberCode)
                .map(existing -> {
                    existing.setRevokedAt(null);
                    existing.setRevokedBy(null);
                    existing.setRevokedReason(null);
                    existing.setAddedReason(reason);
                    existing.setAddedBy(addedBy);
                    return beneficiaryRepository.save(existing);
                })
                .orElseGet(() -> beneficiaryRepository.save(PromotionBeneficiary.builder()
                        .promotion(promotion)
                        .subscriberType(subscriberType)
                        .subscriberCode(subscriberCode)
                        .addedReason(reason)
                        .addedBy(addedBy)
                        .build()));
    }

    private PromotionBeneficiary require(String promotionCode, String subscriberType, String subscriberCode) {
        return beneficiaryRepository.find(promotion(promotionCode).getId(), subscriberType, subscriberCode)
                .orElseThrow(() -> new ResourceNotFoundException("Bénéficiaire introuvable sur cette promotion"));
    }

    private Promotion promotion(String promotionCode) {
        return promotionRepository.findByCodeIgnoreCase(promotionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion " + promotionCode + " introuvable"));
    }
}

package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassUsageStatus;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassUsageType;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassValidationChannel;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassUsage;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassUsageRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Consomme un pass, et sait revenir en arriere.
 *
 * <p>Separe de {@link PassValidationService} a dessein : valider est une question, consommer est un
 * acte. La separation permet d'annoncer « ce pass vous ouvre l'acces » avant que quiconque n'ait
 * franchi la porte, et de refuser sans avoir rien entame.</p>
 *
 * <p>Toute consommation repasse par la validation. Un appelant ne peut pas consommer un pass
 * expire en omettant de le verifier · l'oubli est rendu impossible plutot que deconseille.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassUsageService {

    private final PassRepository passRepository;
    private final PassUsageRepository usageRepository;
    private final PassBeneficiaryRepository beneficiaryRepository;
    private final PassValidationService validationService;
    private final PassEventWriter eventWriter;
    private final SequenceGeneratorFacade sequenceGenerator;

    /**
     * @param referenceType ce qui declenche l'usage · avec le code, il empeche qu'un double scan
     *                      consomme deux journees
     */
    public record UseRequest(
            String passNumber,
            String credentialValue,
            String bearerType,
            String bearerCode,
            PassUsageType usageType,
            String locationCode,
            String resourceCode,
            String resourceTypeCode,
            PassValidationChannel channel,
            String validatedBy,
            String referenceType,
            String referenceCode,
            Instant at
    ) {
    }

    /**
     * Enregistre un usage apres validation.
     *
     * @throws BadRequestException avec le motif de la validation, en francais, lorsqu'elle refuse
     */
    @Transactional
    public PassUsage use(UseRequest request) {
        PassValidationService.ValidationResult validation = validationService.validate(
                new PassValidationService.ValidationRequest(
                        request.passNumber(), request.credentialValue(),
                        request.bearerType(), request.bearerCode(),
                        request.locationCode(), request.resourceTypeCode(), request.at()));

        if (!validation.valid()) {
            throw new BadRequestException(validation.reason());
        }
        Pass pass = validation.pass();

        // Un double scan a la borne, ou un client qui reappuie parce que rien ne s'est affiche,
        // retrouve son usage au lieu d'en creer un second.
        if (StringUtils.hasText(request.referenceType()) && StringUtils.hasText(request.referenceCode())) {
            PassUsage replayed = usageRepository
                    .findByReference(pass.getId(), request.referenceType(), request.referenceCode())
                    .orElse(null);
            if (replayed != null) {
                return replayed;
            }
        }

        PassUsage usage = usageRepository.save(PassUsage.builder()
                .usageNumber(sequenceGenerator.next("pass_usage"))
                .pass(pass)
                .usedByType(request.bearerType())
                .usedByCode(StringUtils.hasText(request.bearerCode()) ? request.bearerCode() : pass.getOwnerCode())
                .usageType(request.usageType() == null ? PassUsageType.CHECK_IN : request.usageType())
                .locationCode(request.locationCode())
                .resourceCode(request.resourceCode())
                .quantity(BigDecimal.ONE)
                .status(PassUsageStatus.COMPLETED)
                .validatedBy(request.validatedBy())
                .validationChannel(request.channel())
                .referenceType(request.referenceType())
                .referenceCode(request.referenceCode())
                .startedAt(request.at())
                .build());

        countUsage(pass, validation.beneficiary(), 1);
        eventWriter.writeEvent(pass, PassEventType.PASS_USED, null);
        return usage;
    }

    /**
     * Contre-passe un usage.
     *
     * <p>La ligne n'est pas supprimee : un usage efface rendrait impossible d'expliquer pourquoi le
     * solde a bouge, et c'est precisement ce qu'on demande quand un client conteste.</p>
     */
    @Transactional
    public PassUsage reverse(String usageNumber, String reason, String reversedBy) {
        PassUsage usage = usageRepository.findByNumber(usageNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Usage " + usageNumber + " introuvable"));
        if (usage.getReversedAt() != null) {
            return usage;
        }

        usage.setReversedAt(Instant.now());
        usage.setReversalReason(reason);
        usage.setReversedBy(reversedBy);
        usageRepository.save(usage);

        Pass pass = usage.getPass();
        PassBeneficiary beneficiary = StringUtils.hasText(usage.getUsedByCode())
                ? beneficiaryRepository.find(pass.getId(), usage.getUsedByType(), usage.getUsedByCode()).orElse(null)
                : null;
        countUsage(pass, beneficiary, -1);
        eventWriter.writeEvent(pass, PassEventType.PASS_USAGE_REVERSED, null);
        return usage;
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Met a jour les compteurs et fait basculer l'etat du pass.
     *
     * <p>{@code PARTIALLY_USED} puis {@code CONSUMED} quand le plafond est atteint. Les deux statuts
     * existaient depuis le debut sans que rien ne les pose · un pass restait {@code ACTIVE} jusqu'a
     * son expiration, quel que soit ce qu'on en avait fait.</p>
     */
    private void countUsage(Pass pass, PassBeneficiary beneficiary, int delta) {
        int used = Math.max(0, (pass.getUsedCount() == null ? 0 : pass.getUsedCount()) + delta);
        pass.setUsedCount(used);

        if (pass.getMaxUses() != null && used >= pass.getMaxUses()) {
            pass.setStatus(PassStatus.CONSUMED);
        } else if (used > 0) {
            pass.setStatus(PassStatus.PARTIALLY_USED);
        } else {
            // Une contre-passation qui ramene a zero rend le pass a son etat d'origine, sans quoi
            // il resterait marque « partiellement utilise » sans aucun usage derriere.
            pass.setStatus(PassStatus.ACTIVE);
        }
        passRepository.save(pass);

        if (beneficiary != null) {
            beneficiary.setUsedCount(Math.max(0,
                    (beneficiary.getUsedCount() == null ? 0 : beneficiary.getUsedCount()) + delta));
            beneficiaryRepository.save(beneficiary);
        }
    }
}

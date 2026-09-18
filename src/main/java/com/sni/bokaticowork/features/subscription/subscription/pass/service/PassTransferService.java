package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassTransferStatus;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassCredential;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassTransfer;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassCredentialRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassTransferRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Changement definitif de titulaire.
 *
 * <p>{@code transferable} existait comme booleen sans rien derriere. Voici ce qu'il declenche.</p>
 *
 * <p>L'acceptation du destinataire n'est pas une politesse : recevoir un pass cree des obligations,
 * et un pass pousse a quelqu'un qui n'en veut pas encombre son espace sans qu'il puisse s'en
 * defaire. Le destinataire doit par ailleurs remplir les conditions du plan, niveau de verification
 * compris · un pass transfere ne doit pas servir a contourner ce qu'une vente directe exigerait.</p>
 *
 * <p>Deux consequences que le transfert entraine, et qu'il faut assumer explicitement.</p>
 *
 * <p><b>Les beneficiaires designes sont retires.</b> Ils avaient ete autorises par l'ancien
 * titulaire ; le nouveau ne les a jamais choisis, et les laisser reviendrait a lui imposer des
 * personnes qui consomment son pass.</p>
 *
 * <p><b>Les supports actifs sont revoques.</b> Un code QR reste sur le telephone de l'ancien
 * titulaire apres le transfert. Ne pas le revoquer lui laisserait la porte ouverte sur un pass qui
 * ne lui appartient plus.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassTransferService {

    private final PassRepository passRepository;
    private final PassTransferRepository transferRepository;
    private final PassBeneficiaryRepository beneficiaryRepository;
    private final PassCredentialRepository credentialRepository;
    private final SubscriptionOwnerResolver ownerResolver;
    private final SubscriberKycLevelGuard kycGuard;
    private final PassEventWriter eventWriter;
    private final SequenceGeneratorFacade sequenceGenerator;

    // -----------------------------------------------------------------------------------------

    /**
     * Demande un transfert. Rien ne change encore : le pass reste au titulaire jusqu'a
     * l'acceptation.
     */
    @Transactional
    public PassTransfer request(String passNumber,
                                SubscriberType toOwnerType,
                                String toOwnerCode,
                                String reason,
                                BigDecimal transferFee,
                                String requestedBy) {
        Pass pass = pass(passNumber);

        if (!Boolean.TRUE.equals(pass.getTransferable())) {
            throw new BadRequestException("Ce pass n'est pas transférable");
        }
        String statusRefusal = refusalForStatus(pass);
        if (statusRefusal != null) {
            throw new BadRequestException(statusRefusal);
        }
        if (pass.getValidUntil() != null && Instant.now().isAfter(pass.getValidUntil())) {
            throw new BadRequestException("Ce pass a expiré");
        }
        if (pass.getOwnerType() == toOwnerType && pass.getOwnerCode().equalsIgnoreCase(toOwnerCode)) {
            throw new BadRequestException("Ce pass appartient déjà à cette personne");
        }
        transferRepository.findPending(pass.getId()).ifPresent(pending -> {
            throw new BadRequestException("Un transfert est déjà en attente sur ce pass");
        });

        // Le destinataire est resolu des la demande, pas a l'acceptation : proposer un pass a
        // quelqu'un qui n'existe pas doit echouer tout de suite, pas apres une notification.
        ownerResolver.resolve(toOwnerType, requireText(toOwnerCode, "Destinataire requis"));

        PassTransfer transfer = transferRepository.save(PassTransfer.builder()
                .transferNumber(sequenceGenerator.next("pass_transfer"))
                .pass(pass)
                .fromOwnerType(pass.getOwnerType().name())
                .fromOwnerCode(pass.getOwnerCode())
                .toOwnerType(toOwnerType.name())
                .toOwnerCode(toOwnerCode.trim())
                .status(PassTransferStatus.PENDING_ACCEPTANCE)
                .reason(reason)
                .transferFee(transferFee)
                .currency(pass.getCurrency())
                .requestedBy(requestedBy)
                .build());

        log.info("Transfert {} demande sur le pass {} vers {}",
                transfer.getTransferNumber(), pass.getPassNumber(), toOwnerCode);
        return transfer;
    }

    /**
     * Le destinataire accepte. C'est ici, et seulement ici, que le pass change de main.
     */
    @Transactional
    public PassTransfer accept(String transferNumber, String acceptedBy) {
        PassTransfer transfer = pending(transferNumber);
        Pass pass = transfer.getPass();

        String statusRefusal = refusalForStatus(pass);
        if (statusRefusal != null) {
            // Le pass a pu etre consomme ou suspendu entre la demande et l'acceptation.
            throw new BadRequestException(statusRefusal);
        }

        SubscriberType toType = SubscriberType.valueOf(transfer.getToOwnerType());
        SubscriptionOwnerResolver.Owner recipient = ownerResolver.resolve(toType, transfer.getToOwnerCode());

        // Un pass transfere ne doit pas servir a contourner ce qu'une vente directe exigerait.
        kycGuard.require(pass.getPassVersion() == null ? null : pass.getPassVersion().getRequiredKycLevel(),
                recipient, "pass");

        pass.setOwnerType(toType);
        pass.setOwnerCode(recipient.code());
        passRepository.save(pass);

        revokeBeneficiaries(pass, transfer);
        revokeCredentials(pass, transfer);

        Instant now = Instant.now();
        transfer.setStatus(PassTransferStatus.COMPLETED);
        transfer.setAcceptedAt(now);
        transfer.setCompletedAt(now);
        transferRepository.save(transfer);

        eventWriter.writeEvent(pass, PassEventType.PASS_TRANSFERRED, null);
        log.info("Pass {} transfere de {} vers {}",
                pass.getPassNumber(), transfer.getFromOwnerCode(), transfer.getToOwnerCode());
        return transfer;
    }

    /** Le destinataire refuse. Le pass reste ou il est. */
    @Transactional
    public PassTransfer reject(String transferNumber, String reason) {
        PassTransfer transfer = pending(transferNumber);
        transfer.setStatus(PassTransferStatus.REJECTED);
        transfer.setRejectedAt(Instant.now());
        transfer.setRejectionReason(reason);
        return transferRepository.save(transfer);
    }

    /** L'emetteur se ravise avant acceptation. */
    @Transactional
    public PassTransfer cancel(String transferNumber, String reason) {
        PassTransfer transfer = pending(transferNumber);
        transfer.setStatus(PassTransferStatus.CANCELLED);
        transfer.setCancelledAt(Instant.now());
        transfer.setRejectionReason(reason);
        return transferRepository.save(transfer);
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Retire les personnes autorisees par l'ancien titulaire.
     *
     * <p>Le nouveau ne les a jamais choisies. Les laisser reviendrait a lui imposer des gens qui
     * consomment son pass, ce qu'il decouvrirait au moment ou son solde baisse sans qu'il soit
     * venu.</p>
     */
    private void revokeBeneficiaries(Pass pass, PassTransfer transfer) {
        List<PassBeneficiary> beneficiaries = beneficiaryRepository.findAllByPassId(pass.getId());
        for (PassBeneficiary beneficiary : beneficiaries) {
            if (beneficiary.getRevokedAt() != null) {
                continue;
            }
            beneficiary.setRevokedAt(Instant.now());
            beneficiary.setRevokedBy("SYSTEM");
            beneficiary.setRevokedReason("Transfert " + transfer.getTransferNumber());
            beneficiaryRepository.save(beneficiary);
        }
    }

    /**
     * Revoque les supports actifs.
     *
     * <p>Un code QR reste sur le telephone de l'ancien titulaire apres le transfert. Ne pas le
     * revoquer lui laisserait la porte ouverte sur un pass qui ne lui appartient plus · le nouveau
     * titulaire se verra remettre son propre support.</p>
     */
    private void revokeCredentials(Pass pass, PassTransfer transfer) {
        List<PassCredential> credentials = credentialRepository.findActiveByPassId(pass.getId());
        for (PassCredential credential : credentials) {
            credential.setRevokedAt(Instant.now());
            credential.setRevokedReason("Transfert " + transfer.getTransferNumber());
            credentialRepository.save(credential);
        }
    }

    private String refusalForStatus(Pass pass) {
        return switch (pass.getStatus()) {
            case ACTIVE, PARTIALLY_USED -> null;
            case CONSUMED -> "Ce pass est entièrement consommé";
            case EXPIRED -> "Ce pass a expiré";
            case SUSPENDED -> "Ce pass est suspendu";
            case CANCELLED -> "Ce pass a été annulé";
            case PENDING_ACTIVATION -> "Ce pass n'est pas encore activé";
            case PAST_DUE -> "Ce pass est en attente de règlement";
            case DRAFT -> "Ce pass n'est pas utilisable";
        };
    }

    private PassTransfer pending(String transferNumber) {
        PassTransfer transfer = transferRepository.findByNumber(transferNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transfert " + transferNumber + " introuvable"));
        if (transfer.getStatus() != PassTransferStatus.PENDING_ACCEPTANCE) {
            throw new BadRequestException("Ce transfert n'est plus en attente");
        }
        return transfer;
    }

    private Pass pass(String passNumber) {
        return passRepository.findByPassNumber(requireText(passNumber, "Numéro de pass requis"))
                .orElseThrow(() -> new ResourceNotFoundException("Pass " + passNumber + " introuvable"));
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}

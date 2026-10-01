package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationAddress;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationRegistration;
import com.sni.bokaticowork.features.domiciliation.model.ServiceDefinition;
import com.sni.bokaticowork.features.domiciliation.model.SubscriptionService;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationAddressRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationRegistrationRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La domiciliation · le service qui demande le plus de mecanique propre.
 *
 * <p>Trois verrous, chacun avec son message. Pas d'activation sans pieces d'identite verifiees du
 * representant legal. Pas d'activation sans enregistrement obtenu · le statut
 * {@code PENDING_REGISTRATION} est visible dans un tableau, c'est la que les dossiers s'enlisent.
 * Pas d'attestation fiscale sans engagement annuel · le refus explique la regle, il ne renvoie pas
 * une erreur technique.</p>
 *
 * <p>La qualite fiscale se deduit, ne se saisit jamais, et se recalcule a chaque changement
 * d'engagement. La perdre n'est pas un detail administratif : l'attestation en cours est revoquee,
 * le domicilie est prevenu, et l'administration l'est aussi si la configuration le demande.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomiciliationService {

    private static final List<DomiciliationContract.Status> ENDED =
            List.of(DomiciliationContract.Status.TERMINATED, DomiciliationContract.Status.EXPIRED);

    private final DomiciliationContractRepository contractRepository;
    private final DomiciliationRegistrationRepository registrationRepository;
    private final DomiciliationAddressRepository addressRepository;
    private final SubscribedServiceService subscribedServiceService;
    private final SubscriptionOwnerResolver ownerResolver;
    private final SubscriberKycLevelGuard kycGuard;
    private final DomiciliationDocuments documents;
    private final TransactionContextResolver contextResolver;
    private final OutboxService outboxService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Value("${bokati.domiciliation.certificate-validity-days:365}")
    private int certificateValidityDays;

    @Value("${bokati.domiciliation.kyc-level-required:2}")
    private int kycLevelRequired;

    @Value("${bokati.domiciliation.retention-years:10}")
    private int retentionYears;

    /** L'administration doit-elle etre informee de la fin d'un contrat qu'elle a enregistre · a confirmer avec le conseil. */
    @Value("${bokati.domiciliation.notify-administration-on-termination:false}")
    private boolean notifyAdministrationOnTermination;

    @Value("${bokati.domiciliation.administration-email:}")
    private String administrationEmail;

    // -----------------------------------------------------------------------------------------
    // Ouvrir
    // -----------------------------------------------------------------------------------------

    public record OpenOrder(
            String subscriptionNumber,
            String legalName, String legalForm, String registrationNumber, String taxNumber,
            String legalRepresentativeName,
            String addressCode, String suiteNumber,
            BillingCycle billingCycle, Integer commitmentMonths, Integer noticePeriodDays,
            LocalDate startDate,
            DomiciliationContract.MailForwardingMode mailForwardingMode
    ) {
    }

    /**
     * Ouvre un contrat en brouillon, sur le service de domiciliation de l'abonnement.
     *
     * <p>Le service est souscrit s'il ne l'est pas encore · il reste {@code PENDING} jusqu'a
     * l'enregistrement. L'engagement se lit dans le rythme de facturation et la duree ; la qualite
     * fiscale en decoule, et de rien d'autre.</p>
     */
    @Transactional
    public DomiciliationContract open(OpenOrder order, String actor) {
        if (!StringUtils.hasText(order.legalName())) {
            throw new BadRequestException("La dénomination du domicilié est requise");
        }
        BillingCycle cycle = order.billingCycle();
        if (cycle != BillingCycle.MONTHLY && cycle != BillingCycle.QUARTERLY && cycle != BillingCycle.YEARLY) {
            throw new BadRequestException("La domiciliation se facture au mois, au trimestre ou à l'année");
        }
        int commitment = order.commitmentMonths() == null ? defaultCommitment(cycle) : order.commitmentMonths();
        if (commitment < 1) {
            throw new BadRequestException("L'engagement est d'un mois au moins");
        }

        DomiciliationAddress address = addressRepository.findByCode(order.addressCode())
                .orElseThrow(() -> new ResourceNotFoundException("Adresse de domiciliation introuvable"));
        if (!Boolean.TRUE.equals(address.getActive())) {
            throw new BadRequestException("Cette adresse n'est plus proposée");
        }
        long occupants = contractRepository.countByAssignedAddress_IdAndStatusNotIn(address.getId(), ENDED);
        if (address.getMaxOccupants() != null && occupants >= address.getMaxOccupants()) {
            throw new ConflictException("domiciliation", "cette adresse a atteint son nombre maximal de domiciliés");
        }

        SubscriptionService service = subscribedServiceService.ofSubscription(order.subscriptionNumber()).stream()
                .filter(s -> s.getServiceDefinition().getServiceCategory() == ServiceDefinition.Category.DOMICILIATION
                        && s.getStatus() != SubscriptionService.Status.TERMINATED)
                .findFirst()
                .orElseGet(() -> subscribedServiceService.subscribe(order.subscriptionNumber(), "SVC-DOMICILIATION", 1, null, null, actor));
        Subscription subscription = service.getSubscription();
        contractRepository.findFirstBySubscription_IdAndStatusNotInOrderByCreatedAtDesc(subscription.getId(), ENDED)
                .ifPresent(existing -> {
                    throw new ConflictException("domiciliation", "un contrat est déjà en cours sur cet abonnement · " + existing.getContractNumber());
                });

        DomiciliationContract contract = DomiciliationContract.builder()
                .contractNumber(sequenceGenerator.next("domiciliation_contract"))
                .subscription(subscription)
                .subscriptionService(service)
                .businessEntity(subscription.getBusinessEntity())
                .legalName(order.legalName().trim())
                .legalForm(trim(order.legalForm()))
                .registrationNumber(trim(order.registrationNumber()))
                .taxNumber(trim(order.taxNumber()))
                .legalRepresentativeType(subscription.getSubscriberType().name())
                .legalRepresentativeCode(subscription.getSubscriberCode())
                .legalRepresentativeName(trim(order.legalRepresentativeName()))
                .assignedAddress(address)
                .suiteNumber(trim(order.suiteNumber()))
                .billingCycle(cycle)
                .commitmentMonths(commitment)
                .noticePeriodDays(order.noticePeriodDays() == null ? 30 : order.noticePeriodDays())
                .startDate(order.startDate())
                .endDate(order.startDate() == null ? null : order.startDate().plusMonths(commitment))
                .mailForwardingMode(order.mailForwardingMode() == null ? DomiciliationContract.MailForwardingMode.HOLD : order.mailForwardingMode())
                .status(DomiciliationContract.Status.DRAFT)
                .createdBy(actor)
                .build();
        applyFiscalRule(contract, "ouverture");
        return contractRepository.save(contract);
    }

    // -----------------------------------------------------------------------------------------
    // Le cycle de vie, verrou par verrou
    // -----------------------------------------------------------------------------------------

    /**
     * Verrou 1 · pieces du representant legal verifiees, puis contrat genere.
     *
     * <p>Sans pieces, le contrat passe {@code PENDING_DOCUMENTS} et le message dit lesquelles : un
     * dossier qui bute sur « KYC insuffisant » ne sait pas quoi faire, un dossier a qui l'on dit
     * « piece d'identite du representant » si.</p>
     */
    @Transactional
    public DomiciliationContract prepareForSignature(String contractNumber, String actor) {
        DomiciliationContract contract = get(contractNumber);
        requireStatus(contract, DomiciliationContract.Status.DRAFT, DomiciliationContract.Status.PENDING_DOCUMENTS);

        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(
                contract.getSubscription().getSubscriberType(), contract.getSubscription().getSubscriberCode());
        try {
            kycGuard.require(kycLevelRequired, owner, "domiciliation");
        } catch (BadRequestException ex) {
            contract.setStatus(DomiciliationContract.Status.PENDING_DOCUMENTS);
            contractRepository.save(contract);
            throw new ConflictException("domiciliation",
                    "les pièces d'identité du représentant légal doivent être vérifiées (niveau " + kycLevelRequired
                            + ") avant de générer le contrat · le dossier est en attente de pièces");
        }

        DocumentResponse generated = documents.contract(contract);
        contract.setContractDocumentCode(generated.getCode());
        contract.setStatus(DomiciliationContract.Status.PENDING_SIGNATURE);
        return contractRepository.save(contract);
    }

    /** Le contrat est signe · l'exemplaire signe est depose, l'enregistrement peut commencer. */
    @Transactional
    public DomiciliationContract markSigned(String contractNumber, String signedDocumentCode, String actor) {
        DomiciliationContract contract = get(contractNumber);
        requireStatus(contract, DomiciliationContract.Status.PENDING_SIGNATURE);
        if (!StringUtils.hasText(signedDocumentCode)) {
            throw new BadRequestException("Déposez l'exemplaire signé avant de le déclarer signé");
        }
        contract.setSignedDocumentCode(signedDocumentCode.trim());
        contract.setSignedAt(Instant.now());
        contract.setStatus(DomiciliationContract.Status.PENDING_REGISTRATION);
        DomiciliationContract saved = contractRepository.save(contract);

        // La demarche externe commence · une ligne a suivre, distincte du contrat.
        registrationRepository.save(DomiciliationRegistration.builder()
                .registrationNumber(sequenceGenerator.next("domiciliation_registration"))
                .contract(saved)
                .status(DomiciliationRegistration.Status.PENDING)
                .build());
        return saved;
    }

    public record RegistrationSubmission(String administrationOffice, java.math.BigDecimal stampDutyAmount,
                                         java.math.BigDecimal registrationFeeAmount, DomiciliationRegistration.PaidBy paidBy,
                                         Boolean rebilled) {
    }

    @Transactional
    public DomiciliationRegistration submitRegistration(String contractNumber, RegistrationSubmission submission, String actor) {
        DomiciliationContract contract = get(contractNumber);
        requireStatus(contract, DomiciliationContract.Status.PENDING_REGISTRATION);
        DomiciliationRegistration registration = openRegistration(contract);
        if (registration.getStatus() != DomiciliationRegistration.Status.PENDING) {
            throw new ConflictException("domiciliation", "cette démarche est déjà " + registration.getStatus());
        }
        registration.setStatus(DomiciliationRegistration.Status.SUBMITTED);
        registration.setSubmittedAt(Instant.now());
        registration.setSubmittedBy(actor);
        registration.setAdministrationOffice(trim(submission.administrationOffice()));
        registration.setStampDutyAmount(submission.stampDutyAmount());
        registration.setRegistrationFeeAmount(submission.registrationFeeAmount());
        registration.setTotalDutyAmount(nonNull(submission.stampDutyAmount()).add(nonNull(submission.registrationFeeAmount())));
        registration.setPaidBy(submission.paidBy());
        registration.setRebilled(Boolean.TRUE.equals(submission.rebilled()));
        return registrationRepository.save(registration);
    }

    public record RegistrationOutcome(String administrationReference, LocalDate registrationDate, LocalDate expiresAt,
                                      String receiptDocumentCode, String registeredDocumentCode) {
    }

    /**
     * Verrou 2 · l'enregistrement est obtenu, le contrat devient ACTIF et le service est rendu.
     *
     * <p>La reference de l'administration et l'exemplaire enregistre sont exiges : c'est ce que
     * l'attestation citera, et ce qu'un controle demandera.</p>
     */
    @Transactional
    public DomiciliationContract confirmRegistration(String contractNumber, RegistrationOutcome outcome, String actor) {
        DomiciliationContract contract = get(contractNumber);
        requireStatus(contract, DomiciliationContract.Status.PENDING_REGISTRATION);
        if (!StringUtils.hasText(outcome.administrationReference()) || !StringUtils.hasText(outcome.registeredDocumentCode())) {
            throw new BadRequestException("L'enregistrement se confirme avec la référence de l'administration et l'exemplaire enregistré et timbré");
        }
        DomiciliationRegistration registration = openRegistration(contract);
        registration.setStatus(DomiciliationRegistration.Status.REGISTERED);
        registration.setAdministrationReference(outcome.administrationReference().trim());
        registration.setRegistrationDate(outcome.registrationDate() == null ? LocalDate.now() : outcome.registrationDate());
        registration.setExpiresAt(outcome.expiresAt());
        registration.setReceiptDocumentCode(trim(outcome.receiptDocumentCode()));
        registration.setRegisteredDocumentCode(outcome.registeredDocumentCode().trim());
        registrationRepository.save(registration);

        contract.setStatus(DomiciliationContract.Status.ACTIVE);
        if (contract.getStartDate() == null) {
            contract.setStartDate(registration.getRegistrationDate());
            contract.setEndDate(contract.getStartDate().plusMonths(contract.getCommitmentMonths()));
        }
        if (Boolean.TRUE.equals(contract.getFiscalAddressEligible()) && contract.getFiscalAddressGrantedAt() == null) {
            contract.setFiscalAddressGrantedAt(Instant.now());
        }
        DomiciliationContract saved = contractRepository.save(contract);
        subscribedServiceService.activate(saved.getSubscriptionService());
        notify(saved, "DOMICILIATION_ACTIVATED", "Votre domiciliation est active",
                Map.of("administrationReference", registration.getAdministrationReference()));
        return saved;
    }

    @Transactional
    public DomiciliationRegistration rejectRegistration(String contractNumber, String reason, String actor) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Un rejet se motive · c'est ce qu'il faudra corriger");
        }
        DomiciliationContract contract = get(contractNumber);
        requireStatus(contract, DomiciliationContract.Status.PENDING_REGISTRATION);
        DomiciliationRegistration registration = openRegistration(contract);
        registration.setStatus(DomiciliationRegistration.Status.REJECTED);
        registration.setRejectionReason(reason.trim());
        DomiciliationRegistration rejected = registrationRepository.save(registration);
        // Une nouvelle demarche s'ouvre aussitot · le contrat reste en attente, pas en echec.
        registrationRepository.save(DomiciliationRegistration.builder()
                .registrationNumber(sequenceGenerator.next("domiciliation_registration"))
                .contract(contract)
                .status(DomiciliationRegistration.Status.PENDING)
                .build());
        notify(contract, "DOMICILIATION_REGISTRATION_REJECTED", "Enregistrement de votre domiciliation · à reprendre",
                Map.of("reason", reason.trim()));
        return rejected;
    }

    // -----------------------------------------------------------------------------------------
    // Attestation · verrou 3
    // -----------------------------------------------------------------------------------------

    /**
     * Delivre l'attestation, commerciale ou fiscale.
     *
     * <p>Fiscale seulement si l'engagement est annuel · et le refus explique la regle. Jamais avant
     * l'enregistrement, quelle que soit la portee : une attestation sur un contrat non enregistre
     * atteste d'un contrat qui n'existe pas encore pleinement.</p>
     */
    @Transactional
    public DomiciliationContract issueCertificate(String contractNumber, DomiciliationContract.CertificateScope scope, String actor) {
        DomiciliationContract contract = get(contractNumber);
        if (contract.getStatus() != DomiciliationContract.Status.ACTIVE) {
            throw new ConflictException("domiciliation", "aucune attestation avant l'enregistrement du contrat · le contrat est "
                    + contract.getStatus());
        }
        if (scope == DomiciliationContract.CertificateScope.FISCAL && !Boolean.TRUE.equals(contract.getFiscalAddressEligible())) {
            throw new ConflictException("domiciliation",
                    "l'adresse n'est fiscale que si l'abonnement est pris pour un an · cet engagement est de "
                            + contract.getCommitmentMonths() + " mois, seule une attestation commerciale peut être délivrée");
        }
        DomiciliationRegistration registration = registrationRepository
                .findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(contract.getId(), List.of(DomiciliationRegistration.Status.REGISTERED))
                .orElse(null);
        LocalDate validUntil = LocalDate.now().plusDays(Math.max(30, certificateValidityDays));
        if (contract.getEndDate() != null && contract.getEndDate().isBefore(validUntil)) {
            // Une attestation ne survit pas au contrat qu'elle atteste.
            validUntil = contract.getEndDate();
        }
        DocumentResponse generated = documents.certificate(contract, registration, scope, validUntil);
        contract.setCertificateDocumentCode(generated.getCode());
        contract.setCertificateScope(scope);
        contract.setCertificateIssuedAt(Instant.now());
        contract.setCertificateValidUntil(validUntil);
        contract.setCertificateRevokedAt(null);
        contract.setCertificateRevocationReason(null);
        DomiciliationContract saved = contractRepository.save(contract);
        notify(saved, "DOMICILIATION_CERTIFICATE_ISSUED", "Votre attestation de domiciliation est disponible",
                Map.of("scope", scope.name(), "validUntil", validUntil.toString(), "documentCode", generated.getCode()));
        return saved;
    }

    // -----------------------------------------------------------------------------------------
    // Changement d'engagement · la qualite fiscale se recalcule
    // -----------------------------------------------------------------------------------------

    @Transactional
    public DomiciliationContract changeCommitment(String contractNumber, BillingCycle cycle, Integer commitmentMonths, String actor) {
        DomiciliationContract contract = get(contractNumber);
        if (contract.getStatus().ended()) {
            throw new ConflictException("domiciliation", "ce contrat est terminé");
        }
        if (cycle != null) {
            if (cycle != BillingCycle.MONTHLY && cycle != BillingCycle.QUARTERLY && cycle != BillingCycle.YEARLY) {
                throw new BadRequestException("La domiciliation se facture au mois, au trimestre ou à l'année");
            }
            contract.setBillingCycle(cycle);
        }
        if (commitmentMonths != null) {
            if (commitmentMonths < 1) {
                throw new BadRequestException("L'engagement est d'un mois au moins");
            }
            contract.setCommitmentMonths(commitmentMonths);
            if (contract.getStartDate() != null) {
                contract.setEndDate(contract.getStartDate().plusMonths(commitmentMonths));
            }
        }
        applyFiscalRule(contract, "changement d'engagement par " + actor);
        return contractRepository.save(contract);
    }

    /**
     * La regle, en un seul endroit.
     *
     * <p>Eligible si douze mois et si l'adresse le permet. Perdre l'eligibilite revoque l'attestation
     * fiscale en cours et previent · le domicilie d'abord, l'administration si la configuration le
     * demande. Gagner l'eligibilite ne delivre rien : l'attestation se demande.</p>
     */
    private void applyFiscalRule(DomiciliationContract contract, String because) {
        boolean eligible = contract.getCommitmentMonths() >= DomiciliationContract.FISCAL_COMMITMENT_MONTHS
                && Boolean.TRUE.equals(contract.getAssignedAddress().getFiscalCapable());
        boolean was = Boolean.TRUE.equals(contract.getFiscalAddressEligible());
        contract.setFiscalAddressEligible(eligible);

        if (was && !eligible) {
            contract.setFiscalAddressRevokedAt(Instant.now());
            contract.setFiscalAddressRevocationReason("Engagement ramené à " + contract.getCommitmentMonths() + " mois · " + because);
            if (contract.getCertificateScope() == DomiciliationContract.CertificateScope.FISCAL && contract.getCertificateRevokedAt() == null) {
                contract.setCertificateRevokedAt(Instant.now());
                contract.setCertificateRevocationReason("Perte de la qualité fiscale · " + because);
            }
            if (contract.getStatus().live()) {
                notify(contract, "DOMICILIATION_FISCAL_LOST", "Votre adresse n'est plus fiscale",
                        Map.of("commitmentMonths", String.valueOf(contract.getCommitmentMonths())));
                notifyAdministration(contract, "DOMICILIATION_FISCAL_LOST_ADMIN",
                        "Perte de la qualité fiscale · " + contract.getLegalName());
            }
        } else if (!was && eligible) {
            contract.setFiscalAddressRevokedAt(null);
            contract.setFiscalAddressRevocationReason(null);
            if (contract.getStatus() == DomiciliationContract.Status.ACTIVE) {
                contract.setFiscalAddressGrantedAt(Instant.now());
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Fin
    // -----------------------------------------------------------------------------------------

    @Transactional
    public DomiciliationContract terminate(String contractNumber, String reason, LocalDate effectiveDate, String actor) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("La fin d'une domiciliation se motive");
        }
        DomiciliationContract contract = get(contractNumber);
        if (contract.getStatus().ended()) {
            throw new ConflictException("domiciliation", "ce contrat est déjà terminé");
        }
        LocalDate effective = effectiveDate == null ? LocalDate.now().plusDays(contract.getNoticePeriodDays()) : effectiveDate;
        contract.setEndDate(effective);
        contract.setTerminatedAt(Instant.now());
        contract.setTerminationReason(reason.trim());
        contract.setStatus(DomiciliationContract.Status.TERMINATED);
        if (contract.getCertificateDocumentCode() != null && contract.getCertificateRevokedAt() == null) {
            contract.setCertificateRevokedAt(Instant.now());
            contract.setCertificateRevocationReason("Fin du contrat · " + reason.trim());
        }
        // Conservation des pieces · posee a la fin, jamais raccourcie.
        contract.setRetainDocumentsUntil(effective.plusYears(Math.max(1, retentionYears)));
        DomiciliationContract saved = contractRepository.save(contract);
        subscribedServiceService.terminate(saved.getSubscriptionService());

        notify(saved, "DOMICILIATION_TERMINATED", "Fin de votre domiciliation",
                Map.of("effectiveDate", effective.toString(), "reason", reason.trim()));
        saved.setTerminationNotifiedAt(Instant.now());
        if (notifyAdministration(saved, "DOMICILIATION_TERMINATED_ADMIN", "Fin de domiciliation · " + saved.getLegalName())) {
            saved.setAdministrationNotifiedAt(Instant.now());
        }
        return contractRepository.save(saved);
    }

    // -----------------------------------------------------------------------------------------
    // Lire, registre
    // -----------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DomiciliationContract get(String contractNumber) {
        return contractRepository.findByContractNumber(contractNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat de domiciliation introuvable"));
    }

    @Transactional(readOnly = true)
    public Page<DomiciliationContract> list(List<DomiciliationContract.Status> statuses, Pageable pageable) {
        return statuses == null || statuses.isEmpty()
                ? contractRepository.findAllByOrderByCreatedAtDesc(pageable)
                : contractRepository.findByStatusInOrderByCreatedAtDesc(statuses, pageable);
    }

    @Transactional(readOnly = true)
    public List<DomiciliationRegistration> registrations(String contractNumber) {
        return registrationRepository.findByContract_IdOrderByCreatedAtDesc(get(contractNumber).getId());
    }

    @Transactional(readOnly = true)
    public Page<DomiciliationRegistration> registrationsInProgress(Pageable pageable) {
        return registrationRepository.findByStatusInOrderBySubmittedAtAsc(
                List.of(DomiciliationRegistration.Status.PENDING, DomiciliationRegistration.Status.SUBMITTED), pageable);
    }

    /** Le registre des domicilies · entrees et sorties datees, reference d'enregistrement. */
    @Transactional(readOnly = true)
    public byte[] registryCsv() {
        StringBuilder csv = new StringBuilder("contrat;denomination;forme;rccm;niu;adresse;complement;qualite;statut;entree;sortie;reference_enregistrement;date_enregistrement;attestation;attestation_valide_jusqu_au\n");
        for (DomiciliationContract c : contractRepository.registry()) {
            DomiciliationRegistration registration = registrationRepository
                    .findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(c.getId(), List.of(DomiciliationRegistration.Status.REGISTERED))
                    .orElse(null);
            csv.append(cell(c.getContractNumber())).append(';')
                    .append(cell(c.getLegalName())).append(';')
                    .append(cell(c.getLegalForm())).append(';')
                    .append(cell(c.getRegistrationNumber())).append(';')
                    .append(cell(c.getTaxNumber())).append(';')
                    .append(cell(c.getAssignedAddress().getLabel())).append(';')
                    .append(cell(c.getSuiteNumber())).append(';')
                    .append(Boolean.TRUE.equals(c.getFiscalAddressEligible()) ? "FISCALE" : "COMMERCIALE").append(';')
                    .append(c.getStatus()).append(';')
                    .append(c.getStartDate() == null ? "" : c.getStartDate()).append(';')
                    .append(c.getStatus().ended() && c.getEndDate() != null ? c.getEndDate() : "").append(';')
                    .append(registration == null ? "" : cell(registration.getAdministrationReference())).append(';')
                    .append(registration == null || registration.getRegistrationDate() == null ? "" : registration.getRegistrationDate()).append(';')
                    .append(c.certificateInForce() ? c.getCertificateScope() : "").append(';')
                    .append(c.certificateInForce() && c.getCertificateValidUntil() != null ? c.getCertificateValidUntil() : "").append('\n');
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // -----------------------------------------------------------------------------------------

    private DomiciliationRegistration openRegistration(DomiciliationContract contract) {
        return registrationRepository.findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(contract.getId(),
                        List.of(DomiciliationRegistration.Status.PENDING, DomiciliationRegistration.Status.SUBMITTED))
                .orElseThrow(() -> new ConflictException("domiciliation", "aucune démarche d'enregistrement ouverte sur ce contrat"));
    }

    private void requireStatus(DomiciliationContract contract, DomiciliationContract.Status... allowed) {
        for (DomiciliationContract.Status status : allowed) {
            if (contract.getStatus() == status) {
                return;
            }
        }
        throw new ConflictException("domiciliation", "cette étape n'est pas possible · le contrat est " + contract.getStatus());
    }

    private int defaultCommitment(BillingCycle cycle) {
        return switch (cycle) {
            case YEARLY -> 12;
            case QUARTERLY -> 3;
            default -> 1;
        };
    }

    private void notify(DomiciliationContract contract, String eventType, String subject, Map<String, String> details) {
        Subscription subscription = contract.getSubscription();
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(
                subscription.getSubscriberType().name(), subscription.getSubscriberCode());
        if (!StringUtils.hasText(party.email())) {
            log.info("Domiciliation {} · {} sans destinataire joignable", contract.getContractNumber(), eventType);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>(details);
        payload.put("recipientEmail", party.email());
        payload.put("recipientName", party.name());
        payload.put("recipientType", subscription.getSubscriberType().name());
        payload.put("recipientCode", subscription.getSubscriberCode());
        payload.put("subject", subject);
        payload.put("templateCode", eventType);
        payload.put("contractNumber", contract.getContractNumber());
        payload.put("legalName", contract.getLegalName());
        outboxService.publish(eventType, "DOMICILIATION", contract.getContractNumber(), payload);
    }

    private boolean notifyAdministration(DomiciliationContract contract, String eventType, String subject) {
        if (!notifyAdministrationOnTermination || !StringUtils.hasText(administrationEmail)) {
            return false;
        }
        Optional<DomiciliationRegistration> registration = registrationRepository
                .findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(contract.getId(), List.of(DomiciliationRegistration.Status.REGISTERED));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("adminEmail", administrationEmail);
        payload.put("subject", subject);
        payload.put("templateCode", eventType);
        payload.put("contractNumber", contract.getContractNumber());
        payload.put("legalName", contract.getLegalName());
        payload.put("administrationReference", registration.map(DomiciliationRegistration::getAdministrationReference).orElse(""));
        outboxService.publish(eventType, "DOMICILIATION", contract.getContractNumber(), payload);
        return true;
    }

    private java.math.BigDecimal nonNull(java.math.BigDecimal value) {
        return value == null ? java.math.BigDecimal.ZERO : value;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String cell(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.contains(";") || clean.contains("\"") ? "\"" + clean.replace("\"", "\"\"") + "\"" : clean;
    }

    /** Expose pour les tests. */
    void configure(int certificateValidityDays, int kycLevelRequired, boolean notifyAdministration, String administrationEmail) {
        this.certificateValidityDays = certificateValidityDays;
        this.retentionYears = 10;
        this.kycLevelRequired = kycLevelRequired;
        this.notifyAdministrationOnTermination = notifyAdministration;
        this.administrationEmail = administrationEmail;
    }
}

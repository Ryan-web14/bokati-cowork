package com.sni.bokaticowork.features.document.kyc.worker;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.config.KycAutomationProperties;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Taches KYC quotidiennes (lot KG3).
 *
 * <ul>
 *   <li><b>Echeances</b> (8 h chaque jour) : sur les pieces vivantes seulement (en attente ou
 *       verifiees, fichier ni remplace, ni archive, ni refuse, dossier non refuse), un rappel par
 *       seuil franchi (60, 30, 7 jours par defaut), memorise sur le document : un seuil manque un
 *       jour est rattrape le lendemain, aucun n'est envoye deux fois. Avant : le lundi seulement,
 *       a date exacte, y compris aux pieces refusees ou remplacees.</li>
 *   <li><b>Expiration</b> : une seule logique, apres le delai de grace ; dossier approuve en
 *       renouvellement (courriel), sinon a corriger. Les documents KYC ne sont plus expires par la
 *       tache horaire de la GED.</li>
 *   <li><b>Dossiers incomplets</b> (9 h) : rappels espaces, plafonnes.</li>
 * </ul>
 *
 * <p>Chaque piece est traitee dans sa propre transaction : une erreur n'annule plus tout le passage.
 * L'approbation automatique apres delai est faite par la tache du cycle de vie des documents (une
 * seule regle pour tous les documents).</p>
 */
@Slf4j
@Component
public class KycAutomationWorker {

    private static final Set<DocumentStatus> DEAD_FILES = Set.of(
            DocumentStatus.SUPERSEDED, DocumentStatus.ARCHIVED, DocumentStatus.REJECTED, DocumentStatus.EXPIRED);

    private final KycDocumentRepository kycDocumentRepository;
    private final KycCaseRepository kycCaseRepository;
    private final DocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;
    private final KycAutomationProperties properties;
    private final TransactionTemplate transactionTemplate;

    public KycAutomationWorker(KycDocumentRepository kycDocumentRepository,
                               KycCaseRepository kycCaseRepository,
                               DocumentRepository documentRepository,
                               MemberRepository memberRepository,
                               CustomerRepository customerRepository,
                               OutboxService outboxService,
                               KycAutomationProperties properties,
                               PlatformTransactionManager transactionManager) {
        this.kycDocumentRepository = kycDocumentRepository;
        this.kycCaseRepository = kycCaseRepository;
        this.documentRepository = documentRepository;
        this.memberRepository = memberRepository;
        this.customerRepository = customerRepository;
        this.outboxService = outboxService;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Ce que le passage a fait. */
    public record ExpiryRun(int reminders, int expired, int failures) {
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkExpiringKycDocuments() {
        ExpiryRun run = runExpiry(LocalDate.now());
        if (run.reminders() + run.expired() + run.failures() > 0) {
            log.info("KYC echeances : {} rappel(s), {} piece(s) expiree(s), {} echec(s)", run.reminders(), run.expired(), run.failures());
        }
    }

    /** Un passage complet, a la date donnee (public pour les tests). */
    public ExpiryRun runExpiry(LocalDate today) {
        List<Long> ids = kycDocumentRepository.findAllByStatusIn(
                        List.of(KycDocumentVerificationStatus.PENDING, KycDocumentVerificationStatus.VERIFIED)).stream()
                .map(KycDocument::getId).toList();
        int reminders = 0;
        int expired = 0;
        int failures = 0;
        for (Long id : ids) {
            try {
                String outcome = transactionTemplate.execute(status -> handle(id, today));
                if ("REMINDED".equals(outcome)) reminders++;
                if ("EXPIRED".equals(outcome)) expired++;
            } catch (RuntimeException ex) {
                failures++;
                log.warn("KYC echeance : piece {} non traitee : {}", id, ex.getMessage());
            }
        }
        return new ExpiryRun(reminders, expired, failures);
    }

    private String handle(Long pieceId, LocalDate today) {
        KycDocument piece = kycDocumentRepository.findById(pieceId).orElse(null);
        if (piece == null || !alive(piece)) {
            return "SKIPPED";
        }
        LocalDate expiry = expiryDate(piece);
        if (expiry == null) {
            return "SKIPPED";
        }
        int grace = properties.getExpiry().getGracePeriodDays() == null ? 0 : properties.getExpiry().getGracePeriodDays();
        if (today.isAfter(expiry.plusDays(grace))) {
            expire(piece);
            return "EXPIRED";
        }
        long daysLeft = ChronoUnit.DAYS.between(today, expiry);
        if (daysLeft < 0) {
            return "SKIPPED";
        }
        // Seuil franchi le plus proche (ex. 20 jours restants : seuil 30).
        Optional<Integer> crossed = properties.getExpiry().getReminderDays().stream()
                .filter(threshold -> daysLeft <= threshold)
                .min(Comparator.naturalOrder());
        if (crossed.isEmpty()) {
            return "SKIPPED";
        }
        Document document = piece.getDocument();
        Integer alreadySent = document.getExpiryReminderSentDays();
        if (alreadySent != null && alreadySent <= crossed.get()) {
            return "SKIPPED";
        }
        document.setExpiryReminderSentDays(crossed.get());
        documentRepository.save(document);
        publishDocumentEvent("KYC_DOCUMENT_EXPIRY_REMINDER", piece, Map.of("daysUntilExpiry", daysLeft));
        return "REMINDED";
    }

    /** Piece vivante : en attente ou verifiee, fichier courant, dossier non refuse. */
    static boolean alive(KycDocument piece) {
        if (piece.getStatus() != KycDocumentVerificationStatus.PENDING && piece.getStatus() != KycDocumentVerificationStatus.VERIFIED) {
            return false;
        }
        if (piece.getDocument() == null || DEAD_FILES.contains(piece.getDocument().getStatus())) {
            return false;
        }
        return piece.getKycCase() == null || piece.getKycCase().getStatus() != KycCaseStatus.REJECTED;
    }

    private static LocalDate expiryDate(KycDocument piece) {
        return piece.getExpiryDate() != null ? piece.getExpiryDate() : piece.getDocument().getExpiryDate();
    }

    private void expire(KycDocument piece) {
        piece.setStatus(KycDocumentVerificationStatus.EXPIRED);
        kycDocumentRepository.save(piece);
        Document document = piece.getDocument();
        if (document.getStatus() != DocumentStatus.EXPIRED) {
            document.setStatus(DocumentStatus.EXPIRED);
            documentRepository.save(document);
        }
        KycCase kycCase = piece.getKycCase();
        if (kycCase != null && kycCase.getStatus() != KycCaseStatus.REJECTED && kycCase.getStatus() != KycCaseStatus.EXPIRED) {
            boolean renewal = kycCase.getStatus() == KycCaseStatus.APPROVED || kycCase.getStatus() == KycCaseStatus.RENEWAL_REQUIRED;
            kycCase.setStatus(renewal ? KycCaseStatus.RENEWAL_REQUIRED : KycCaseStatus.PENDING_CORRECTION);
            kycCase.setDecisionComment("Pièce arrivée à échéance : " + typeName(piece));
            kycCaseRepository.save(kycCase);
            syncOwnerStatus(kycCase);
            HashMap<String, Object> extra = new HashMap<>();
            extra.put("documentTypeCode", piece.getDocumentType());
            extra.put("documentTypeName", typeName(piece));
            publishCaseEvent("KYC_RENEWAL_REQUIRED", kycCase, extra);
        }
        publishDocumentEvent("KYC_DOCUMENT_EXPIRED", piece, Map.of());
    }

    private static String typeName(KycDocument piece) {
        Document document = piece.getDocument();
        return document != null && document.getDocumentType() != null && document.getDocumentType().getName() != null
                ? document.getDocumentType().getName() : piece.getDocumentType();
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void remindIncompleteKycCases() {
        if (!Boolean.TRUE.equals(properties.getReminders().getEnabled())) {
            return;
        }
        Instant now = Instant.now();
        List<KycCaseStatus> statuses = List.of(KycCaseStatus.IN_PROGRESS, KycCaseStatus.PENDING_CORRECTION);
        List<Long> ids = kycCaseRepository.findAll().stream()
                .filter(item -> statuses.contains(item.getStatus()))
                .filter(item -> item.getReminderCount() == null || item.getReminderCount() < properties.getReminders().getMaxReminders())
                .filter(item -> shouldSendReminder(item, now))
                .map(KycCase::getId)
                .toList();
        for (Long id : ids) {
            try {
                transactionTemplate.executeWithoutResult(status -> kycCaseRepository.findById(id).ifPresent(item -> {
                    item.setLastReminderSentAt(now);
                    item.setReminderCount(item.getReminderCount() == null ? 1 : item.getReminderCount() + 1);
                    kycCaseRepository.save(item);
                    publishCaseEvent("KYC_INCOMPLETE_REMINDER", item, Map.of());
                }));
            } catch (RuntimeException ex) {
                log.warn("KYC rappel : dossier {} non traite : {}", id, ex.getMessage());
            }
        }
    }

    private boolean shouldSendReminder(KycCase kycCase, Instant now) {
        Instant reference = kycCase.getLastReminderSentAt() == null ? kycCase.getStartedAt() : kycCase.getLastReminderSentAt();
        if (reference == null) {
            return false;
        }
        long inactiveDays = ChronoUnit.DAYS.between(reference, now);
        return properties.getReminders().getReminderAfterDays().stream().anyMatch(days -> inactiveDays >= days);
    }

    private void syncOwnerStatus(KycCase kycCase) {
        if (kycCase.getOwnerType() == DocumentOwnerType.MEMBER) {
            memberRepository.findById(kycCase.getOwnerId()).ifPresent(member -> {
                member.setStatus(MemberStatus.PENDING_CORRECTION);
                memberRepository.save(member);
            });
        }
        if (kycCase.getOwnerType() == DocumentOwnerType.CUSTOMER) {
            customerRepository.findById(kycCase.getOwnerId()).ifPresent(customer -> {
                customer.setStatus(CustomerStatus.PENDING);
                customerRepository.save(customer);
            });
        }
    }

    private void publishCaseEvent(String eventType, KycCase kycCase, Map<String, Object> extra) {
        HashMap<String, Object> payload = new HashMap<>(extra);
        payload.put("kycCaseCode", kycCase.getCode());
        payload.put("ownerType", kycCase.getOwnerType());
        payload.put("ownerId", kycCase.getOwnerId());
        payload.put("status", kycCase.getStatus());
        outboxService.publish(eventType, "KYC_CASE", kycCase.getCode(), payload);
    }

    private void publishDocumentEvent(String eventType, KycDocument document, Map<String, Object> extra) {
        HashMap<String, Object> payload = new HashMap<>(extra);
        payload.put("documentCode", document.getDocument() == null ? null : document.getDocument().getCode());
        payload.put("documentType", typeName(document));
        payload.put("kycCaseCode", document.getKycCase() == null ? null : document.getKycCase().getCode());
        payload.put("expiryDate", expiryDate(document));
        payload.put("ownerType", document.getOwnerType());
        payload.put("ownerId", document.getOwnerId());
        outboxService.publish(eventType, "KYC_DOCUMENT", String.valueOf(document.getId()), payload);
    }
}

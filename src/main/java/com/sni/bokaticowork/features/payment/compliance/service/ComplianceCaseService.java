package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCaseNote;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceCaseNoteRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceCaseRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceRuleRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Les dossiers de conformite · ouvrir, confier, conclure.
 *
 * <p>Un dossier a un responsable, une echeance et une decision motivee. L'echeance depend de la
 * priorite, et elle est mesuree : un dossier hors delai remonte au tableau de bord, il ne
 * disparait pas dans la pile. Un sujet n'a qu'un dossier ouvert a la fois · un signalement de plus
 * sur le meme portefeuille s'ajoute au dossier existant plutot que d'en ouvrir un second que
 * personne ne rapprocherait du premier.</p>
 *
 * <p>La cloture retourne son verdict aux regles : un dossier ecarte compte comme bruit pour chaque
 * regle qui l'avait alimente, un dossier confirme comme succes. C'est ainsi que le taux de faux
 * positifs se mesure, regle par regle, sans que personne n'ait a le tenir a la main.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceCaseService {

    public static final String SUBJECT_WALLET = "WALLET";
    private static final List<ComplianceCase.Status> OPEN_STATUSES =
            List.of(ComplianceCase.Status.OPEN, ComplianceCase.Status.IN_REVIEW, ComplianceCase.Status.ESCALATED);

    private final ComplianceCaseRepository caseRepository;
    private final ComplianceCaseNoteRepository noteRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final WalletRiskFlagRepository flagRepository;
    private final WalletAccountRepository walletRepository;
    private final ComplianceAuditService auditService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Value("${bokati.compliance.case.due-hours.critical:24}")
    private int dueHoursCritical;

    @Value("${bokati.compliance.case.due-hours.high:72}")
    private int dueHoursHigh;

    @Value("${bokati.compliance.case.due-hours.medium:168}")
    private int dueHoursMedium;

    @Value("${bokati.compliance.case.due-hours.low:336}")
    private int dueHoursLow;

    // -----------------------------------------------------------------------------------------
    // Ouvrir
    // -----------------------------------------------------------------------------------------

    /**
     * Ouvre un dossier pour un portefeuille, ou rattache au dossier deja ouvert.
     *
     * <p>La priorite ne baisse jamais : un dossier MEDIUM qui recoit un signalement HIGH devient
     * HIGH, et son echeance se resserre en consequence.</p>
     */
    @Transactional
    public ComplianceCase openOrAttach(WalletAccount wallet, WalletRiskFlag flag, ComplianceCase.Priority priority,
                                       String title, String openedBy) {
        Optional<ComplianceCase> existing = caseRepository
                .findFirstBySubjectTypeAndSubjectCodeAndStatusInOrderByCreatedAtDesc(
                        SUBJECT_WALLET, wallet.getWalletNumber(), OPEN_STATUSES);

        ComplianceCase complianceCase;
        if (existing.isPresent()) {
            complianceCase = existing.get();
            complianceCase.setTriggeredByFlags(join(complianceCase.getTriggeredByFlags(), flag == null ? null : flag.getFlagNumber()));
            if (priority != null && priority.ordinal() > complianceCase.getPriority().ordinal()) {
                complianceCase.setPriority(priority);
                complianceCase.setDueAt(earliest(complianceCase.getDueAt(), dueFor(priority, complianceCase.getCreatedAt())));
            }
            complianceCase = caseRepository.save(complianceCase);
        } else {
            Instant now = Instant.now();
            ComplianceCase.Priority effective = priority == null ? ComplianceCase.Priority.MEDIUM : priority;
            complianceCase = caseRepository.save(ComplianceCase.builder()
                    .caseNumber(sequenceGenerator.next("compliance_case"))
                    .subjectType(SUBJECT_WALLET)
                    .subjectCode(wallet.getWalletNumber())
                    .wallet(wallet)
                    .title(title)
                    .triggeredByFlags(flag == null ? null : flag.getFlagNumber())
                    .priority(effective)
                    .dueAt(dueFor(effective, now))
                    .openedBy(openedBy == null ? "SYSTEM" : openedBy)
                    .build());
            auditService.record(openedBy, "CASE_OPENED", SUBJECT_WALLET, wallet.getWalletNumber(),
                    complianceCase.getCaseNumber() + " · " + title);
        }

        if (flag != null) {
            flag.setCaseNumber(complianceCase.getCaseNumber());
            if (flag.getStatus() == WalletRiskFlag.Status.OPEN) {
                flag.setStatus(WalletRiskFlag.Status.UNDER_REVIEW);
            }
            flagRepository.save(flag);
        }
        return complianceCase;
    }

    // -----------------------------------------------------------------------------------------
    // Confier, annoter, escalader
    // -----------------------------------------------------------------------------------------

    @Transactional
    public ComplianceCase assign(String caseNumber, String assignee, String actor) {
        if (!StringUtils.hasText(assignee)) {
            throw new BadRequestException("Indiquez à qui confier le dossier");
        }
        ComplianceCase complianceCase = open(caseNumber);
        String before = complianceCase.getAssignedTo();
        complianceCase.setAssignedTo(assignee.trim());
        complianceCase.setAssignedAt(Instant.now());
        if (complianceCase.getStatus() == ComplianceCase.Status.OPEN) {
            complianceCase.setStatus(ComplianceCase.Status.IN_REVIEW);
        }
        ComplianceCase saved = caseRepository.save(complianceCase);
        auditService.record(actor, null, "CASE_ASSIGNED", SUBJECT_WALLET, saved.getSubjectCode(),
                before, assignee.trim(), saved.getCaseNumber());
        return saved;
    }

    @Transactional
    public ComplianceCaseNote addNote(String caseNumber, String author, String note) {
        if (!StringUtils.hasText(note)) {
            throw new BadRequestException("Une note vide n'est pas une note");
        }
        ComplianceCase complianceCase = open(caseNumber);
        ComplianceCaseNote saved = noteRepository.save(ComplianceCaseNote.builder()
                .complianceCase(complianceCase)
                .author(author == null ? "SYSTEM" : author)
                .note(note.trim())
                .build());
        auditService.record(author, "CASE_NOTE", SUBJECT_WALLET, complianceCase.getSubjectCode(),
                complianceCase.getCaseNumber() + " · " + note.trim());
        return saved;
    }

    @Transactional
    public ComplianceCase escalate(String caseNumber, String escalatedTo, String reason, String actor) {
        if (!StringUtils.hasText(escalatedTo) || !StringUtils.hasText(reason)) {
            throw new BadRequestException("Une escalade dit à qui, et pourquoi");
        }
        ComplianceCase complianceCase = open(caseNumber);
        complianceCase.setStatus(ComplianceCase.Status.ESCALATED);
        complianceCase.setEscalatedTo(escalatedTo.trim());
        complianceCase.setEscalatedAt(Instant.now());
        if (complianceCase.getPriority().ordinal() < ComplianceCase.Priority.HIGH.ordinal()) {
            complianceCase.setPriority(ComplianceCase.Priority.HIGH);
            complianceCase.setDueAt(earliest(complianceCase.getDueAt(), dueFor(ComplianceCase.Priority.HIGH, Instant.now())));
        }
        ComplianceCase saved = caseRepository.save(complianceCase);
        auditService.record(actor, "CASE_ESCALATED", SUBJECT_WALLET, saved.getSubjectCode(),
                saved.getCaseNumber() + " · vers " + escalatedTo.trim() + " · " + reason.trim());
        return saved;
    }

    // -----------------------------------------------------------------------------------------
    // Conclure
    // -----------------------------------------------------------------------------------------

    /**
     * Ferme le dossier avec une decision motivee.
     *
     * <p>Le verdict retourne aux regles et aux signalements : ecarte, les signalements du dossier
     * sont ecartes et chaque regle compte un faux positif ; confirme, ils sont confirmes et chaque
     * regle compte un succes. Un portefeuille place en examen pour ce dossier est libere si le
     * dossier est ecarte · ce qui suit une confirmation est une action administrative, tracee
     * comme telle, pas un effet de bord de la cloture.</p>
     */
    @Transactional
    public ComplianceCase close(String caseNumber, ComplianceCase.Decision decision, String rationale,
                                String findings, String actor) {
        if (decision == null) {
            throw new BadRequestException("Une clôture porte une décision");
        }
        if (!StringUtils.hasText(rationale) || rationale.trim().length() < 10) {
            throw new BadRequestException("Une décision sans motif écrit n'est pas une décision · dix caractères au moins");
        }
        ComplianceCase complianceCase = open(caseNumber);
        Instant now = Instant.now();
        String before = complianceCase.getStatus().name();

        complianceCase.setStatus(switch (decision) {
            case CLEARED -> ComplianceCase.Status.CLOSED_CLEARED;
            case CONFIRMED -> ComplianceCase.Status.CLOSED_CONFIRMED;
            case REPORTED -> ComplianceCase.Status.CLOSED_REPORTED;
        });
        complianceCase.setDecision(decision);
        complianceCase.setDecisionRationale(rationale.trim());
        if (StringUtils.hasText(findings)) {
            complianceCase.setFindings(findings.trim());
        }
        complianceCase.setDecidedBy(actor == null ? "SYSTEM" : actor);
        complianceCase.setDecidedAt(now);
        ComplianceCase saved = caseRepository.save(complianceCase);

        boolean cleared = decision == ComplianceCase.Decision.CLEARED;
        for (String flagNumber : flagNumbers(saved.getTriggeredByFlags())) {
            flagRepository.findByFlagNumber(flagNumber).ifPresent(flag -> {
                if (flag.open()) {
                    flag.setStatus(cleared ? WalletRiskFlag.Status.DISMISSED : WalletRiskFlag.Status.CONFIRMED);
                    flag.setReviewedBy(saved.getDecidedBy());
                    flag.setReviewedAt(now);
                    flag.setResolution("Dossier " + saved.getCaseNumber() + " · " + rationale.trim());
                    flagRepository.save(flag);
                }
                if (StringUtils.hasText(flag.getRuleCode())) {
                    ruleRepository.findByRuleCode(flag.getRuleCode()).ifPresent(rule -> {
                        if (cleared) {
                            rule.setDismissedCount(rule.getDismissedCount() + 1);
                        } else {
                            rule.setConfirmedCount(rule.getConfirmedCount() + 1);
                        }
                        ruleRepository.save(rule);
                    });
                }
            });
        }

        if (cleared && saved.getWallet() != null) {
            releaseIfUnderReviewForThisOnly(saved.getWallet().getId());
        }

        auditService.record(actor, null, "CASE_CLOSED", SUBJECT_WALLET, saved.getSubjectCode(),
                before, saved.getStatus().name(), saved.getCaseNumber() + " · " + rationale.trim());
        return saved;
    }

    private void releaseIfUnderReviewForThisOnly(Long walletId) {
        WalletAccount wallet = walletRepository.findByIdForUpdate(walletId).orElse(null);
        if (wallet == null || wallet.getStatus() != WalletStatus.UNDER_REVIEW) {
            return;
        }
        boolean otherOpen = caseRepository
                .findFirstBySubjectTypeAndSubjectCodeAndStatusInOrderByCreatedAtDesc(SUBJECT_WALLET, wallet.getWalletNumber(), OPEN_STATUSES)
                .isPresent();
        if (!otherOpen) {
            wallet.setStatus(WalletStatus.ACTIVE);
            walletRepository.save(wallet);
        }
    }

    // -----------------------------------------------------------------------------------------
    // Lire
    // -----------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ComplianceCase get(String caseNumber) {
        return caseRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
    }

    @Transactional(readOnly = true)
    public List<ComplianceCaseNote> notes(String caseNumber) {
        return noteRepository.findByComplianceCase_IdOrderByCreatedAtAsc(get(caseNumber).getId());
    }

    @Transactional(readOnly = true)
    public Page<ComplianceCase> open(Pageable pageable) {
        return caseRepository.findByStatusInOrderByPriorityDescDueAtAsc(OPEN_STATUSES, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ComplianceCase> all(Pageable pageable) {
        return caseRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ComplianceCase> mine(String assignee, Pageable pageable) {
        return caseRepository.findByAssignedToAndStatusInOrderByDueAtAsc(assignee, OPEN_STATUSES, pageable);
    }

    @Transactional(readOnly = true)
    public List<ComplianceCase> forSubject(String subjectCode) {
        return caseRepository.findBySubjectTypeAndSubjectCodeOrderByCreatedAtDesc(SUBJECT_WALLET, subjectCode);
    }

    @Transactional(readOnly = true)
    public List<ComplianceCase> overdue() {
        return caseRepository.findOverdue(OPEN_STATUSES, Instant.now());
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return caseRepository.countByStatusIn(OPEN_STATUSES);
    }

    @Transactional(readOnly = true)
    public long countOverdue() {
        return caseRepository.countOverdue(OPEN_STATUSES, Instant.now());
    }

    public static ComplianceCase.Priority priorityFor(ComplianceRule.Severity severity) {
        if (severity == null) {
            return ComplianceCase.Priority.MEDIUM;
        }
        return switch (severity) {
            case CRITICAL -> ComplianceCase.Priority.CRITICAL;
            case HIGH -> ComplianceCase.Priority.HIGH;
            case MEDIUM -> ComplianceCase.Priority.MEDIUM;
            case LOW, INFO -> ComplianceCase.Priority.LOW;
        };
    }

    // -----------------------------------------------------------------------------------------

    private ComplianceCase open(String caseNumber) {
        ComplianceCase complianceCase = get(caseNumber);
        if (!complianceCase.getStatus().open()) {
            throw new BadRequestException("Ce dossier est clos · un dossier clos ne se rouvre pas, on en ouvre un autre");
        }
        return complianceCase;
    }

    private Instant dueFor(ComplianceCase.Priority priority, Instant from) {
        int hours = switch (priority) {
            case CRITICAL -> dueHoursCritical;
            case HIGH -> dueHoursHigh;
            case MEDIUM -> dueHoursMedium;
            case LOW -> dueHoursLow;
        };
        return from.plus(Duration.ofHours(Math.max(1, hours)));
    }

    private Instant earliest(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        return b == null || a.isBefore(b) ? a : b;
    }

    private String join(String existing, String flagNumber) {
        if (!StringUtils.hasText(flagNumber)) {
            return existing;
        }
        Set<String> all = new LinkedHashSet<>(flagNumbers(existing));
        all.add(flagNumber);
        return String.join(",", all);
    }

    static List<String> flagNumbers(String joined) {
        if (!StringUtils.hasText(joined)) {
            return List.of();
        }
        return Arrays.stream(joined.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }
}

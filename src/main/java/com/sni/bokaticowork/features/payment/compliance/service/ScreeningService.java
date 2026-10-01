package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningListEntry;
import com.sni.bokaticowork.features.payment.compliance.repository.ScreeningCheckRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ScreeningListEntryRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Le controle des listes de personnes exposees et sanctionnees · et la preuve qu'il a eu lieu.
 *
 * <p>Le point technique qui compte n'est pas le controle mais sa preuve : quelle liste, quelle
 * version, quelle date, quel resultat. Chaque controle laisse une ligne que la base interdit de
 * reecrire. Un controle dont on ne peut pas montrer qu'il a eu lieu n'a, en pratique, pas eu
 * lieu.</p>
 *
 * <p>Le rapprochement de noms est volontairement simple et volontairement large : noms normalises,
 * tous les mots de l'un dans l'autre, dans n'importe quel ordre. Il produit des rapprochements
 * possibles qu'un humain tranche · c'est la revue qui decide, jamais l'algorithme. Un rapprochement
 * manque est plus grave qu'un rapprochement a ecarter.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningService {

    public static final String SUBJECT_MEMBER = "MEMBER";

    private final ScreeningListEntryRepository entryRepository;
    private final ScreeningCheckRepository checkRepository;
    private final WalletAccountRepository walletRepository;
    private final WalletRiskFlagService flagService;
    private final ComplianceCaseService caseService;
    private final ComplianceAuditService auditService;
    private final SequenceGeneratorFacade sequenceGenerator;

    // -----------------------------------------------------------------------------------------
    // Controler
    // -----------------------------------------------------------------------------------------

    /**
     * Controle un sujet et garde la preuve.
     *
     * <p>Dans sa propre transaction : le controle a l'entree en relation ne doit ni bloquer ni
     * annuler l'inscription. Un rapprochement ouvre un signalement et un dossier ; l'inscription,
     * elle, aboutit, et c'est le dossier qui decidera de la suite.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScreeningCheck screen(String subjectType, String subjectCode, String fullName, Integer birthYear,
                                 ScreeningCheck.Trigger trigger, String checkedBy) {
        String normalized = normalize(fullName);
        List<ScreeningListEntry> entries = entryRepository.findByActiveTrue();
        List<ScreeningListEntry> matches = new ArrayList<>();
        List<String> details = new ArrayList<>();
        boolean exact = false;

        if (StringUtils.hasText(normalized)) {
            for (ScreeningListEntry entry : entries) {
                Match match = matchOf(normalized, birthYear, entry);
                if (match != Match.NONE) {
                    matches.add(entry);
                    exact |= match == Match.EXACT;
                    details.add(entry.getListCode() + "/" + entry.getListVersion() + " · " + entry.getFullName()
                            + (entry.getReference() == null ? "" : " (" + entry.getReference() + ")") + " · " + match);
                }
            }
        }

        ScreeningCheck.Result result = matches.isEmpty() ? ScreeningCheck.Result.CLEAR
                : exact ? ScreeningCheck.Result.MATCH : ScreeningCheck.Result.POSSIBLE_MATCH;

        ScreeningCheck check = checkRepository.save(ScreeningCheck.builder()
                .checkNumber(sequenceGenerator.next("screening_check"))
                .subjectType(subjectType)
                .subjectCode(subjectCode)
                .subjectName(fullName == null ? "" : fullName.trim())
                .triggerReason(trigger)
                .listsChecked(listsChecked())
                .checkedAt(Instant.now())
                .result(result)
                .matchedEntryIds(matches.isEmpty() ? null
                        : String.join(",", matches.stream().map(e -> String.valueOf(e.getId())).toList()))
                .matchDetails(details.isEmpty() ? null : String.join("\n", details))
                .checkedBy(checkedBy == null ? "SYSTEM" : checkedBy)
                .build());

        auditService.record(checkedBy, "SCREENING_CHECK", subjectType, subjectCode,
                check.getCheckNumber() + " · " + result + " · " + check.getListsChecked());

        if (result != ScreeningCheck.Result.CLEAR) {
            openCaseFor(check);
        }
        return check;
    }

    /** Un rapprochement ouvre un dossier sur le portefeuille du sujet, s'il en a un · sinon il attend la revue seul. */
    private void openCaseFor(ScreeningCheck check) {
        Optional<WalletAccount> wallet = walletRepository.list(check.getSubjectType(), check.getSubjectCode(),
                org.springframework.data.domain.PageRequest.of(0, 1)).stream().findFirst();
        if (wallet.isEmpty()) {
            log.warn("Controle {} · rapprochement {} pour {} {} sans portefeuille · en attente de revue",
                    check.getCheckNumber(), check.getResult(), check.getSubjectType(), check.getSubjectCode());
            return;
        }
        WalletRiskFlag.Severity severity = check.getResult() == ScreeningCheck.Result.MATCH
                ? WalletRiskFlag.Severity.CRITICAL : WalletRiskFlag.Severity.HIGH;
        flagService.raiseForRule(wallet.get(), WalletRiskFlag.Type.LIST_MATCH, severity,
                        "Contrôle de liste " + check.getCheckNumber() + " · " + check.getResult() + "\n" + check.getMatchDetails(),
                        check.getCheckNumber(), "SCREENING")
                .ifPresent(raised -> {
                    ComplianceCase opened = caseService.openOrAttach(wallet.get(), raised.flag(),
                            check.getResult() == ScreeningCheck.Result.MATCH ? ComplianceCase.Priority.CRITICAL : ComplianceCase.Priority.HIGH,
                            "Rapprochement de liste · " + check.getResult(), "SCREENING");
                    check.setCaseNumber(opened.getCaseNumber());
                    checkRepository.save(check);
                });
    }

    // -----------------------------------------------------------------------------------------
    // Revoir
    // -----------------------------------------------------------------------------------------

    /** La revue humaine · vrai rapprochement ou homonymie. Le resultat du controle, lui, ne bouge pas. */
    @Transactional
    public ScreeningCheck review(String checkNumber, boolean trueMatch, String rationale, String reviewer) {
        if (!StringUtils.hasText(rationale) || rationale.trim().length() < 10) {
            throw new BadRequestException("La revue d'un rapprochement se motive · dix caractères au moins");
        }
        ScreeningCheck check = checkRepository.findByCheckNumber(checkNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Contrôle introuvable"));
        if (check.getResult() == ScreeningCheck.Result.CLEAR) {
            throw new BadRequestException("Un contrôle sans rapprochement n'a rien à revoir");
        }
        check.setReviewedBy(reviewer);
        check.setReviewedAt(Instant.now());
        check.setReviewOutcome(trueMatch ? "TRUE_MATCH" : "FALSE_MATCH");
        ScreeningCheck saved = checkRepository.save(check);
        auditService.record(reviewer, "SCREENING_REVIEWED", saved.getSubjectType(), saved.getSubjectCode(),
                saved.getCheckNumber() + " · " + saved.getReviewOutcome() + " · " + rationale.trim());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ScreeningCheck> pendingReview(Pageable pageable) {
        return checkRepository.findByResultInOrderByCheckedAtDesc(
                List.of(ScreeningCheck.Result.POSSIBLE_MATCH, ScreeningCheck.Result.MATCH), pageable);
    }

    @Transactional(readOnly = true)
    public Page<ScreeningCheck> all(Pageable pageable) {
        return checkRepository.findAllByOrderByCheckedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<ScreeningCheck> history(String subjectType, String subjectCode) {
        return checkRepository.findBySubjectTypeAndSubjectCodeOrderByCheckedAtDesc(subjectType, subjectCode);
    }

    @Transactional(readOnly = true)
    public long countUnreviewedMatches() {
        return checkRepository.countByResultInAndReviewedAtIsNull(
                List.of(ScreeningCheck.Result.POSSIBLE_MATCH, ScreeningCheck.Result.MATCH));
    }

    // -----------------------------------------------------------------------------------------
    // Alimenter les listes
    // -----------------------------------------------------------------------------------------

    public record ListEntryInput(String fullName, String aliases, Integer birthYear, String nationality, String reference) {
    }

    /**
     * Charge une version d'une liste · l'ancienne version de la meme liste est desactivee.
     *
     * <p>Desactivee, pas supprimee : les controles passes la citent, et leur preuve doit rester
     * verifiable contre ce qu'elle nomme.</p>
     */
    @Transactional
    public int loadList(String listCode, String listVersion, ScreeningListEntry.Type type,
                        List<ListEntryInput> inputs, String loadedBy) {
        if (!StringUtils.hasText(listCode) || !StringUtils.hasText(listVersion) || type == null) {
            throw new BadRequestException("Une liste a un code, une version et un type");
        }
        List<ScreeningListEntry> previous = entryRepository.findActiveByList(listCode.trim());
        previous.forEach(entry -> entry.setActive(false));
        entryRepository.saveAll(previous);

        int loaded = 0;
        for (ListEntryInput input : inputs) {
            if (input == null || !StringUtils.hasText(input.fullName())) {
                continue;
            }
            String aliases = input.aliases() == null ? null : Arrays.stream(input.aliases().split(";"))
                    .map(this::normalize).filter(StringUtils::hasText).reduce((a, b) -> a + ";" + b).orElse(null);
            entryRepository.save(ScreeningListEntry.builder()
                    .listCode(listCode.trim())
                    .listVersion(listVersion.trim())
                    .entryType(type)
                    .fullName(input.fullName().trim())
                    .normalizedName(normalize(input.fullName()))
                    .aliases(aliases)
                    .birthYear(input.birthYear())
                    .nationality(input.nationality())
                    .reference(input.reference())
                    .loadedBy(loadedBy)
                    .build());
            loaded++;
        }
        auditService.record(loadedBy, "SCREENING_LIST_LOADED", "SCREENING_LIST", listCode.trim(),
                "version " + listVersion.trim() + " · " + loaded + " entrées · " + previous.size() + " désactivées");
        return loaded;
    }

    @Transactional(readOnly = true)
    public String listsChecked() {
        List<Object[]> lists = entryRepository.activeLists();
        if (lists.isEmpty()) {
            return "AUCUNE_LISTE";
        }
        return String.join(";", lists.stream().map(row -> row[0] + "/" + row[1] + "(" + row[2] + ")").toList());
    }

    // -----------------------------------------------------------------------------------------

    enum Match {
        NONE, EXACT, PARTIAL
    }

    /**
     * Rapprochement large.
     *
     * <p>EXACT si les noms normalises sont egaux (ou un alias l'est) et que l'annee de naissance, si
     * les deux la connaissent, concorde. PARTIAL si tous les mots de l'un figurent dans l'autre ·
     * « Jean Pierre Mabiala » contre « Mabiala Jean ». Une annee de naissance qui contredit ecarte
     * le rapprochement : c'est le seul cas ou l'algorithme se permet de dire non.</p>
     */
    private Match matchOf(String normalizedSubject, Integer birthYear, ScreeningListEntry entry) {
        if (birthYear != null && entry.getBirthYear() != null && !birthYear.equals(entry.getBirthYear())) {
            return Match.NONE;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(entry.getNormalizedName());
        if (StringUtils.hasText(entry.getAliases())) {
            candidates.addAll(Arrays.asList(entry.getAliases().split(";")));
        }
        Set<String> subjectWords = new HashSet<>(Arrays.asList(normalizedSubject.split(" ")));
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            if (candidate.equals(normalizedSubject)) {
                return Match.EXACT;
            }
            Set<String> candidateWords = new HashSet<>(Arrays.asList(candidate.split(" ")));
            if (subjectWords.size() >= 2 && candidateWords.size() >= 2
                    && (subjectWords.containsAll(candidateWords) || candidateWords.containsAll(subjectWords))) {
                return Match.PARTIAL;
            }
        }
        return Match.NONE;
    }

    /** Sans accents, sans casse, sans ponctuation, un seul espace entre les mots. */
    String normalize(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        String stripped = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}

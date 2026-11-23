package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.*;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pieces comptables en partie double (plan, sections 4.4, 4.6 et 4.8).
 *
 * <p>Une piece est d'abord un brouillard au numero provisoire. A la validation, ses controles
 * passent ; une piece desequilibree ou a la contrepartie non conforme suit la politique en
 * vigueur : attente d'equilibrage (defaut), ligne d'equilibrage sur le compte d'attente dedie, ou
 * acceptation tracee. Une piece validee prend son numero definitif dans la sequence continue du
 * journal, dans la meme transaction, et devient immuable : elle se contre-passe, elle ne se
 * modifie ni ne se supprime.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingEntryService {

    private static final String BALANCE = "ACCOUNTING_ENTRY:BALANCE";
    private static final EnumSet<JournalType> CLASS_NINE_JOURNALS = EnumSet.of(JournalType.OFF_BALANCE, JournalType.ANALYTIC);
    private static final EnumSet<JournalType> TREASURY_JOURNALS = EnumSet.of(JournalType.CASH, JournalType.BANK, JournalType.MOBILE_MONEY);
    /** Lot K1 (S11) : pieces saisies par une personne, soumises a la seconde validation au-dela du seuil. */
    private static final EnumSet<EntryOrigin> HUMAN_ORIGINS = EnumSet.of(EntryOrigin.MANUAL, EntryOrigin.CORRECTION, EntryOrigin.REVERSAL,
            EntryOrigin.REGULARIZATION);
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules();
    private static final java.time.format.DateTimeFormatter DAY = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccEntryRepository entryRepository;
    private final AccEntryLineRepository lineRepository;
    private final AccAccountRepository accountRepository;
    private final AccPendingEntryLogRepository pendingRepository;
    private final AccLetteringLineRepository letteringLineRepository;
    private final AccLetteringRepository letteringRepository;
    private final AccountingChartService chart;
    private final AccountingPeriodService periods;
    private final EntryNumbering numbering;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashActorResolver actors;
    @Lazy
    private final AccountingLetteringService lettering;
    @Lazy
    private final AnalyticAllocator analyticAllocator;
    /** Garde des comptes tenus par le fichier des immobilisations (lot F18), absente d'un contexte reduit. */
    private final org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.features.accounting.service.support.FixedAssetIntake> fixedAssets;
    /** Lot K0 (M1) : previent le moteur quand une piece d'evenement est validee. */
    private final org.springframework.context.ApplicationEventPublisher publisher;

    /** Probleme qui empeche la validation directe ; il suit la politique des pieces desequilibrees. */
    record Issue(PendingReason reason, String detail) {
    }

    /** Lot F18 : une saisie manuelle sur un compte suivi par les fiches d'immobilisation exige le droit special et un motif. */
    private void guardFixedAssets(AccountingRequests.Entry request) {
        com.sni.bokaticowork.features.accounting.service.support.FixedAssetIntake guard = fixedAssets == null ? null : fixedAssets.getIfAvailable();
        if (guard != null && request.lines() != null) {
            guard.checkManualEntry(request.lines(), request.label());
        }
    }

    // ------------------------------------------------------------- saisie

    public AccountingResponses.Entry create(AccountingRequests.Entry request) {
        String actor = actors.resolve("SYSTEM");
        guardFixedAssets(request);
        AccJournal journal = periods.journal(request.journalCode());
        if (!Boolean.TRUE.equals(journal.getActive())) {
            throw new BadRequestException("Le journal " + journal.getCode() + " est desactive");
        }
        AccPeriod period = periods.periodFor(request.entryDate(), true);
        AccEntry entry = entryRepository.save(AccEntry.builder()
                .provisionalNumber(sequenceGenerator.next("acc_provisional_entry"))
                .journalCode(journal.getCode())
                .fiscalYearCode(periods.fiscalYearOf(period).getCode())
                .periodCode(period.getCode())
                .entryDate(request.entryDate())
                .documentDate(request.documentDate())
                .label(request.label().trim())
                .origin(EntryOrigin.MANUAL)
                .sourceType(trim(request.sourceType()))
                .sourceCode(trim(request.sourceCode()))
                .status(EntryStatus.DRAFT)
                .createdBy(actor)
                .build());
        writeLines(entry, request.lines());
        if (Boolean.TRUE.equals(request.validate())) {
            return validate(entry.getProvisionalNumber());
        }
        return toResponse(entry, List.of());
    }

    /**
     * Piece produite par le logiciel (moteur d'imputation, lettrage, cloture) : validee aussitot,
     * ou mise en attente selon la politique.
     */
    public AccEntry post(String journalCode, LocalDate date, String label, EntryOrigin origin, String sourceType, String sourceCode,
                         List<AccountingRequests.EntryLine> lines, String actor) {
        return post(new AutoPost(journalCode, date, null, label, origin, sourceType, sourceCode, lines, actor, true, null));
    }

    /**
     * Piece automatique (moteur d'imputation, lot F2).
     *
     * @param originalDate date du fait quand la piece a ete reportee dans une periode ouverte suivante
     * @param validate     faux : la piece reste en brouillard (journal sans validation automatique, reprise)
     */
    public record AutoPost(String journalCode, LocalDate date, LocalDate originalDate, String label, EntryOrigin origin,
                           String sourceType, String sourceCode, List<AccountingRequests.EntryLine> lines, String actor,
                           boolean validate, Long postingEventId) {
    }

    public AccEntry post(AutoPost request) {
        AccJournal journal = periods.journal(request.journalCode());
        if (!Boolean.TRUE.equals(journal.getActive())) {
            throw new BadRequestException("Le journal " + journal.getCode() + " est desactive");
        }
        AccPeriod period = periods.periodFor(request.date(), request.origin() == EntryOrigin.MANUAL);
        String label = request.label() == null ? "" : request.label().length() > 500 ? request.label().substring(0, 500) : request.label();
        AccEntry entry = entryRepository.save(AccEntry.builder()
                .provisionalNumber(sequenceGenerator.next("acc_provisional_entry"))
                .journalCode(journal.getCode())
                .fiscalYearCode(periods.fiscalYearOf(period).getCode())
                .periodCode(period.getCode())
                .entryDate(request.date())
                .originalDate(request.originalDate())
                .label(label)
                .origin(request.origin())
                .sourceType(request.sourceType())
                .sourceCode(request.sourceCode())
                .status(EntryStatus.DRAFT)
                .createdBy(request.actor())
                .postingEventId(request.postingEventId())
                .build());
        writeLines(entry, request.lines());
        if (request.validate()) {
            runValidation(entry, journal, request.actor(), false);
        }
        return entry;
    }

    /** Lignes inversees d'une piece (brouillard de contre-passation d'une piece de reprise). */
    public List<AccountingRequests.EntryLine> swappedLines(AccEntry entry) {
        return lineRepository.findByEntryIdOrderByLineNoAsc(entry.getId()).stream()
                .map(line -> new AccountingRequests.EntryLine(line.getAccountNumber(), line.getThirdPartyCode(), line.getLabel(),
                        line.getCredit().signum() > 0 ? line.getCredit() : null, line.getDebit().signum() > 0 ? line.getDebit() : null,
                        line.getCurrency(), line.getForeignAmount(), line.getRate(), line.getTaxCode(), line.getTaxBase(), line.getDueDate(),
                        line.getReference()))
                .toList();
    }

    /** Derniere date validee d'un journal sur un exercice (ordre chronologique strict). */
    public java.util.Optional<LocalDate> lastValidatedDate(String journalCode, String fiscalYearCode) {
        return entryRepository.lastValidatedDate(journalCode, fiscalYearCode, EntryNumbering.NUMBERED);
    }

    /**
     * Lot K1 (decision D3, M5) : derniere date validee d'un journal dans une periode. La chronologie
     * stricte ne s'impose que dans le mois : un fait arrive en retard garde sa date si sa periode est
     * ouverte, meme si le journal a deja des pieces du mois suivant.
     */
    public java.util.Optional<LocalDate> lastValidatedDate(String journalCode, String fiscalYearCode, String periodCode) {
        return entryRepository.lastValidatedDateInPeriod(journalCode, fiscalYearCode, periodCode, EntryNumbering.NUMBERED);
    }

    /** Date du jour a Brazzaville (lot K1, S5 : le serveur tourne en UTC). */
    public static LocalDate today() {
        return LocalDate.now(PostingEngine.AccountingClock.ZONE);
    }

    /** Refus code CHRONOLOGIE_STRICTE, avec la date a retenir (lot K1 : limite a la periode). */
    private void checkChronology(AccEntry entry, String journalCode, String what) {
        if (!settings.strictChronology()) {
            return;
        }
        lastValidatedDate(journalCode, entry.getFiscalYearCode(), entry.getPeriodCode())
                .filter(last -> entry.getEntryDate().isBefore(last))
                .ifPresent(last -> {
                    // Plusieurs pieces peuvent porter la meme date ; seule une date anterieure est refusee.
                    throw BadRequestException.coded("CHRONOLOGIE_STRICTE", what + " est datée du " + entry.getEntryDate().format(DAY)
                            + ", avant la dernière pièce validée du journal " + journalCode + " dans la période " + entry.getPeriodCode()
                            + " (" + last.format(DAY) + "). Plusieurs pièces peuvent avoir la même date, mais pas une date antérieure dans le même mois : "
                            + "datez-la au " + last.format(DAY) + " ou après, ou passez le paramètre accounting.numbering.chronology à PERIOD");
                });
    }

    /**
     * Lot K1 : date proposee pour une correction. La date de la piece d'origine si sa periode est
     * ouverte (les etats du mois deviennent justes), au plus tot la derniere piece du journal dans ce
     * mois ; sinon aujourd'hui.
     */
    public LocalDate correctionDate(AccEntry original, String journalCode) {
        LocalDate date = original.getEntryDate();
        AccPeriod period;
        try {
            period = periods.periodFor(date, true);
        } catch (RuntimeException closed) {
            return today().isBefore(date) ? date : today();
        }
        if (settings.strictChronology()) {
            LocalDate last = lastValidatedDate(journalCode, periods.fiscalYearOf(period).getCode(), period.getCode()).orElse(null);
            if (last != null && date.isBefore(last)) {
                date = last;
            }
        }
        return date;
    }

    /** Lot K1 (S11) : au-dela du seuil, une piece saisie par une personne est validee par une autre. */
    private boolean aboveSecondValidation(AccEntry entry) {
        BigDecimal threshold = settings.secondValidationThreshold();
        return threshold != null && entry.getTotalDebit().max(entry.getTotalCredit()).compareTo(threshold) >= 0;
    }

    private String secondValidationMessage(String what) {
        return what + " enregistrée en brouillard : au-delà de " + plain(settings.secondValidationThreshold())
                + " XAF, elle est validée par un autre comptable";
    }

    /** Une piece en attente ou en brouillard remplacee par une piece regeneree (lot F2). */
    public void discardForRegeneration(AccEntry entry, String actor) {
        if (entry.getStatus() != EntryStatus.PENDING_BALANCE && entry.getStatus() != EntryStatus.DRAFT) {
            throw new BadRequestException("La piece " + entry.getProvisionalNumber() + " est " + entry.getStatus()
                    + " : seule une piece en attente ou en brouillard se regenere ; une piece validee se contre-passe");
        }
        entry.setStatus(EntryStatus.REJECTED);
        entry.setRejectedBy(actor);
        entry.setRejectedAt(Instant.now());
        entry.setRejectionReason("Regeneree depuis l'evenement comptable");
        entryRepository.save(entry);
        pendingRepository.findByEntryIdOrderByCreatedAtDesc(entry.getId()).stream().filter(log -> log.getResolvedAt() == null)
                .forEach(log -> {
                    log.setResolvedAt(Instant.now());
                    log.setResolvedBy(actor);
                    log.setResolution("REGENERATED");
                    pendingRepository.save(log);
                });
    }

    public AccountingResponses.Entry update(String reference, AccountingRequests.Entry request) {
        AccEntry entry = entry(reference);
        if (entry.getStatus() != EntryStatus.DRAFT && entry.getStatus() != EntryStatus.PENDING_BALANCE) {
            throw new BadRequestException("Seul un brouillard ou une piece en attente se modifie ; une piece validee se contre-passe");
        }
        if (entry.getStatus() == EntryStatus.PENDING_BALANCE && !actors.hasAuthority(BALANCE)) {
            throw new ForbiddenException("Completer une piece en attente d'equilibrage demande le droit ACCOUNTING_ENTRY:BALANCE");
        }
        if (entry.getOrigin() == EntryOrigin.MANUAL) {
            guardFixedAssets(request);
        }
        AccJournal journal = periods.journal(request.journalCode());
        if (entry.getPostingEventId() != null) {
            // Lot K1 (S4) : une piece automatique garde son journal, et la trace de ce que le schema
            // avait produit est conservee a la premiere modification.
            if (!journal.getCode().equalsIgnoreCase(entry.getJournalCode())) {
                throw new BadRequestException("Le journal d'une pièce automatique vient de son schéma (" + entry.getJournalCode()
                        + ") : corrigez le schéma puis régénérez l'événement, ou rejetez la pièce");
            }
            if (entry.getGeneratedLinesJson() == null) {
                entry.setGeneratedLinesJson(snapshot(lineRepository.findByEntryIdOrderByLineNoAsc(entry.getId())));
            }
            entry.setManuallyChangedBy(actors.resolve("SYSTEM"));
            entry.setManuallyChangedAt(Instant.now());
            if (StringUtils.hasText(request.changeReason())) {
                String reason = request.changeReason().trim();
                entry.setChangeReason(reason.length() > 500 ? reason.substring(0, 500) : reason);
            }
        }
        AccPeriod period = periods.periodFor(request.entryDate(), true);
        entry.setJournalCode(journal.getCode());
        entry.setFiscalYearCode(periods.fiscalYearOf(period).getCode());
        entry.setPeriodCode(period.getCode());
        entry.setEntryDate(request.entryDate());
        entry.setDocumentDate(request.documentDate());
        entry.setLabel(request.label().trim());
        entryRepository.save(entry);
        lineRepository.deleteByEntryId(entry.getId());
        lineRepository.flush();
        writeLines(entry, request.lines());
        if (Boolean.TRUE.equals(request.validate()) || entry.getStatus() == EntryStatus.PENDING_BALANCE) {
            return validate(entry.getProvisionalNumber());
        }
        return toResponse(entry, List.of());
    }

    public void deleteDraft(String reference) {
        AccEntry entry = entry(reference);
        if (entry.getStatus() != EntryStatus.DRAFT) {
            throw new BadRequestException("Seul un brouillard se supprime ; une piece en attente se rejette, une piece validee se contre-passe");
        }
        if (entry.getPostingEventId() != null) {
            // Lot K1 (S3) : supprimer ce brouillard laissait l'evenement pointer vers une piece disparue.
            throw new BadRequestException("Le brouillard " + entry.getProvisionalNumber() + " vient d'un événement comptable : rejetez-le avec un motif. "
                    + "L'événement repasse en échec, puis se régénère ou s'écarte");
        }
        lineRepository.deleteByEntryId(entry.getId());
        entryRepository.delete(entry);
    }

    private void writeLines(AccEntry entry, List<AccountingRequests.EntryLine> requests) {
        int lineNo = 1;
        List<AccEntryLine> lines = new ArrayList<>();
        for (AccountingRequests.EntryLine request : requests) {
            BigDecimal debit = money(request.debit());
            BigDecimal credit = money(request.credit());
            if (debit.signum() < 0 || credit.signum() < 0) {
                throw new BadRequestException("Ligne " + lineNo + " : un montant ne peut pas etre negatif");
            }
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new BadRequestException("Ligne " + lineNo + " : une ligne porte un debit ou un credit, pas les deux");
            }
            if (debit.signum() == 0 && credit.signum() == 0) {
                throw new BadRequestException("Ligne " + lineNo + " : une ligne a zero n'est pas admise");
            }
            AccAccount account = chart.find(request.accountNumber()).orElse(null);
            lines.add(AccEntryLine.builder()
                    .entryId(entry.getId())
                    .lineNo(lineNo++)
                    .accountNumber(request.accountNumber().trim())
                    .thirdPartyType(account == null ? null : account.getThirdPartyType())
                    .thirdPartyCode(trim(request.thirdPartyCode()))
                    .label(StringUtils.hasText(request.label()) ? request.label().trim() : entry.getLabel())
                    .debit(debit)
                    .credit(credit)
                    .currency(trim(request.currency()))
                    .foreignAmount(request.foreignAmount())
                    .rate(request.rate())
                    .taxCode(trim(request.taxCode()))
                    .taxBase(request.taxBase())
                    .dueDate(request.dueDate())
                    .reference(trim(request.reference()))
                    .analyticJson(AnalyticAllocator.write(request.analytic()))
                    .build());
        }
        lineRepository.saveAll(lines);
        entry.setTotalDebit(lines.stream().map(AccEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add));
        entry.setTotalCredit(lines.stream().map(AccEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add));
        entryRepository.save(entry);
    }

    // ------------------------------------------------------------ validation

    public AccountingResponses.Entry validate(String reference) {
        AccEntry entry = entry(reference);
        if (entry.getStatus() != EntryStatus.DRAFT && entry.getStatus() != EntryStatus.PENDING_BALANCE) {
            throw new BadRequestException("La piece " + reference + " n'est ni un brouillard ni une piece en attente");
        }
        String actor = actors.resolve("SYSTEM");
        AccJournal journal = periods.journal(entry.getJournalCode());
        // Lot K1 (S11) : la seconde validation couvre aussi les contre-passations, les regularisations
        // et les corrections (avant : la validation des saisies manuelles seulement).
        if (HUMAN_ORIGINS.contains(entry.getOrigin()) && !PostingEngine.SYSTEM_ACTOR.equals(entry.getCreatedBy())
                && aboveSecondValidation(entry) && CashActorResolver.samePerson(actor, entry.getCreatedBy())) {
            throw new ForbiddenException("Au-delà de " + plain(settings.secondValidationThreshold())
                    + " XAF, une pièce saisie, contre-passée, régularisée ou corrigée par une personne est validée par un autre comptable");
        }
        if (entry.getStatus() == EntryStatus.DRAFT && entry.getOrigin() == EntryOrigin.REVERSAL && StringUtils.hasText(entry.getReversalOf())) {
            AccEntry original = entry(entry.getReversalOf());
            finalizeReversal(entry, original, journal, actor, true);
            return toResponse(entry, List.of("Contre-passation validée sous le numéro " + entry.getEntryNumber()));
        }
        if (entry.getStatus() == EntryStatus.DRAFT && entry.getOrigin() == EntryOrigin.REGULARIZATION && StringUtils.hasText(entry.getRegularizationOf())) {
            AccEntry original = entry(entry.getRegularizationOf());
            finalizeRegularization(entry, original, journal, actor);
            return toResponse(entry, List.of("Pièce " + original.getEntryNumber() + " régularisée par " + entry.getEntryNumber()));
        }
        boolean manual = entry.getOrigin() == EntryOrigin.MANUAL || entry.getOrigin() == EntryOrigin.CORRECTION;
        List<String> messages = runValidation(entry, journal, actor, manual);
        return toResponse(entry, messages);
    }

    /** Controles, politique des pieces desequilibrees, puis numerotation. */
    private List<String> runValidation(AccEntry entry, AccJournal journal, String actor, boolean manual) {
        AccPeriod period = periods.periodFor(entry.getEntryDate(), manual);
        entry.setPeriodCode(period.getCode());
        entry.setFiscalYearCode(periods.fiscalYearOf(period).getCode());
        List<AccEntryLine> lines = lineRepository.findByEntryIdOrderByLineNoAsc(entry.getId());
        if (lines.size() < 2) {
            throw new BadRequestException("Une piece comptable a au moins deux lignes, au debit et au credit");
        }
        checkChronology(entry, journal.getCode(), "La pièce " + entry.getProvisionalNumber());
        List<Issue> issues = checks(entry, journal, lines, manual);
        // Lot F9 : ventilation analytique obligatoire (analytique active).
        List<String> analytic = analyticAllocator.problems(entry, lines);
        if (!analytic.isEmpty()) {
            String detail = String.join(" ; ", analytic);
            if (manual) {
                throw new BadRequestException("Ventilation analytique incomplete : " + detail);
            }
            issues.add(new Issue(PendingReason.ANALYTIC_INCOMPLETE, detail));
        }
        BigDecimal difference = entry.getTotalDebit().subtract(entry.getTotalCredit());
        if (difference.signum() != 0) {
            issues.add(new Issue(PendingReason.UNBALANCED, "Debit " + plain(entry.getTotalDebit()) + ", credit "
                    + plain(entry.getTotalCredit()) + ", difference " + plain(difference)));
        }
        List<String> messages = new ArrayList<>();
        if (!issues.isEmpty()) {
            boolean onlyUnbalanced = issues.stream().allMatch(issue -> issue.reason() == PendingReason.UNBALANCED);
            UnbalancedPolicy policy = onlyUnbalanced ? settings.unbalancedPolicy() : UnbalancedPolicy.HOLD;
            if (policy == UnbalancedPolicy.SUSPENSE_ACCOUNT) {
                BigDecimal cap = settings.suspenseMaxDifference();
                if (cap != null && difference.abs().compareTo(cap) > 0) {
                    policy = UnbalancedPolicy.HOLD;
                    messages.add("Difference au-dela du plafond du compte d'attente : piece mise en attente");
                }
            }
            switch (policy) {
                case HOLD -> {
                    hold(entry, issues, difference, actor);
                    messages.add("Piece mise en attente d'equilibrage : " + issues.get(0).detail());
                    return messages;
                }
                case SUSPENSE_ACCOUNT -> {
                    AccAccount suspense = chart.account(settings.suspenseAccount());
                    AccEntryLine balancing = lineRepository.save(AccEntryLine.builder()
                            .entryId(entry.getId())
                            .lineNo(lines.size() + 1)
                            .accountNumber(suspense.getNumber())
                            .label("Equilibrage a regulariser : " + entry.getProvisionalNumber())
                            .debit(difference.signum() < 0 ? difference.negate() : BigDecimal.ZERO.setScale(4))
                            .credit(difference.signum() > 0 ? difference : BigDecimal.ZERO.setScale(4))
                            .reference(entry.getProvisionalNumber())
                            .build());
                    lines.add(balancing);
                    entry.setTotalDebit(entry.getTotalDebit().add(balancing.getDebit()));
                    entry.setTotalCredit(entry.getTotalCredit().add(balancing.getCredit()));
                    log(entry, PendingReason.UNBALANCED, difference, "Equilibree sur " + suspense.getNumber(), "SUSPENSE", actor);
                    notifier.notifyAccountants("Piece equilibree sur le compte d'attente · " + entry.getProvisionalNumber(),
                            "La piece " + entry.getProvisionalNumber() + " du journal " + entry.getJournalCode() + " (" + entry.getLabel()
                                    + ") etait desequilibree de " + plain(difference) + " XAF : la difference est passee sur " + suspense.getNumber()
                                    + ". Regularisez ce compte avant la cloture.", entry.getProvisionalNumber(), "/accounting/entries", entry.getCreatedBy());
                    messages.add("Difference de " + plain(difference) + " XAF passee sur le compte d'attente " + suspense.getNumber());
                }
                case ACCEPT -> {
                    entry.setAcceptedUnbalanced(true);
                    entry.setUnbalancedAmount(difference);
                    log(entry, PendingReason.UNBALANCED, difference, "Acceptee desequilibree par " + actor, "ACCEPTED", actor);
                    notifier.notifyAccountants("Piece acceptee desequilibree · " + entry.getProvisionalNumber(),
                            "La piece " + entry.getProvisionalNumber() + " du journal " + entry.getJournalCode() + " a ete validee avec un ecart de "
                                    + plain(difference) + " XAF (mode ACCEPT). La cloture est bloquee jusqu'a sa regularisation.",
                            entry.getProvisionalNumber(), "/accounting/entries", entry.getCreatedBy());
                    messages.add("Piece validee avec un ecart de " + plain(difference) + " XAF, a regulariser avant la cloture");
                }
            }
        }
        // Une piece qui sort de l'attente : son suivi est resolu.
        pendingRepository.findByEntryIdOrderByCreatedAtDesc(entry.getId()).stream()
                .filter(log -> log.getResolvedAt() == null)
                .forEach(log -> {
                    log.setResolvedAt(Instant.now());
                    log.setResolvedBy(actor);
                    log.setResolution("COMPLETED");
                    pendingRepository.save(log);
                });
        numbering.assign(entry, journal, lines);
        entry.setStatus(EntryStatus.VALIDATED);
        entry.setPendingReason(null);
        entry.setPendingDetail(null);
        entry.setValidatedBy(actor);
        entry.setValidatedAt(Instant.now());
        entryRepository.save(entry);
        analyticAllocator.persist(entry, lines);
        Map<String, AccAccount> accounts = accounts(lines);
        lines.stream().filter(line -> StringUtils.hasText(line.getThirdPartyCode()))
                .forEach(line -> chart.registerThirdParty(line.getAccountNumber(),
                        accounts.containsKey(line.getAccountNumber()) ? accounts.get(line.getAccountNumber()).getThirdPartyType() : null,
                        line.getThirdPartyCode(), null));
        if (entry.getPostingEventId() != null) {
            publisher.publishEvent(new com.sni.bokaticowork.features.accounting.service.support.EntryValidatedEvent(
                    entry.getPostingEventId(), entry.getProvisionalNumber(), entry.getEntryNumber()));
        }
        if (StringUtils.hasText(entry.getCorrectionOf())) {
            linkCorrection(entry, lines, actor);
        }
        messages.add("Piece validee sous le numero " + entry.getEntryNumber());
        return messages;
    }

    /**
     * Lot K1 : la piece d'origine pointe vers la piece qui la corrige. Un reclassement de toute une
     * ligne sur un compte lettrable est lettre avec la ligne d'origine (les deux se soldent).
     */
    private void linkCorrection(AccEntry correction, List<AccEntryLine> lines, String actor) {
        entryRepository.findByEntryNumber(correction.getCorrectionOf()).ifPresent(original -> {
            original.setCorrectedBy(correction.getEntryNumber());
            entryRepository.save(original);
            if (!"RECLASS".equals(correction.getCorrectionKind())) {
                return;
            }
            List<AccEntryLine> available = new ArrayList<>(lines);
            for (AccEntryLine line : lineRepository.findByEntryIdOrderByLineNoAsc(original.getId())) {
                BigDecimal amount = line.getDebit().max(line.getCredit());
                boolean debit = line.getDebit().signum() > 0;
                available.stream()
                        .filter(out -> out.getAccountNumber().equals(line.getAccountNumber())
                                && Objects.equals(out.getThirdPartyCode(), line.getThirdPartyCode())
                                && (debit ? out.getCredit() : out.getDebit()).compareTo(amount) == 0)
                        .findFirst()
                        .ifPresent(out -> {
                            available.remove(out);
                            lettering.pairCorrection(line, out, actor);
                        });
            }
        });
    }

    /** Controles de compte et de contrepartie (plan, section 4.6). */
    private List<Issue> checks(AccEntry entry, AccJournal journal, List<AccEntryLine> lines, boolean manual) {
        List<Issue> issues = new ArrayList<>();
        Map<String, AccAccount> accounts = accounts(lines);
        boolean classNineJournal = CLASS_NINE_JOURNALS.contains(journal.getType());
        List<String> allowed = AccountingPeriodService.split(journal.getAllowedAccounts());
        for (AccEntryLine line : lines) {
            AccAccount account = accounts.get(line.getAccountNumber());
            if (account == null) {
                if (manual) {
                    throw new BadRequestException("Ligne " + line.getLineNo() + " : le compte " + line.getAccountNumber() + " n'existe pas");
                }
                issues.add(new Issue(PendingReason.MISSING_ACCOUNT, "Compte " + line.getAccountNumber() + " absent du plan"));
                continue;
            }
            if (account.getStatus() != AccountStatus.ACTIVE) {
                if (manual) {
                    throw new BadRequestException("Ligne " + line.getLineNo() + " : le compte " + account.getNumber() + " n'est pas actif");
                }
                issues.add(new Issue(PendingReason.INACTIVE_ACCOUNT, "Compte " + account.getNumber() + " inactif"));
            }
            boolean classNineAccount = AccountingChartService.isAnalyticOrOffBalance(account);
            if (classNineJournal != classNineAccount) {
                throw new BadRequestException(classNineJournal
                        ? "Le journal " + journal.getCode() + " ne recoit que des comptes de classe 9"
                        : "Le compte " + account.getNumber() + " (classe 9) se tient dans les journaux hors bilan ou analytique, pas dans " + journal.getCode());
            }
            if (account.getThirdPartyType() != null && !StringUtils.hasText(line.getThirdPartyCode())) {
                issues.add(new Issue(PendingReason.COUNTERPART_NOT_ALLOWED, "Le compte de tiers " + account.getNumber()
                        + " porte toujours le tiers concerne (ligne " + line.getLineNo() + ")"));
            }
            if (!allowed.isEmpty() && allowed.stream().noneMatch(prefix -> account.getNumber().startsWith(prefix))) {
                issues.add(new Issue(PendingReason.COUNTERPART_NOT_ALLOWED, "Le compte " + account.getNumber()
                        + " n'est pas autorise dans le journal " + journal.getCode()));
            }
        }
        if (TREASURY_JOURNALS.contains(journal.getType()) && journal.getTreasuryAccountNumber() != null
                && lines.stream().noneMatch(line -> line.getAccountNumber().startsWith(journal.getTreasuryAccountNumber()))) {
            issues.add(new Issue(PendingReason.COUNTERPART_NOT_ALLOWED, "Un journal de tresorerie contient une ligne sur son compte "
                    + journal.getTreasuryAccountNumber()));
        }
        return issues;
    }

    private void hold(AccEntry entry, List<Issue> issues, BigDecimal difference, String actor) {
        Issue first = issues.get(0);
        entry.setStatus(EntryStatus.PENDING_BALANCE);
        entry.setPendingReason(first.reason());
        entry.setPendingDetail(issues.stream().map(Issue::detail).distinct().collect(Collectors.joining(" ; ")));
        entry.setUnbalancedAmount(difference.signum() == 0 ? null : difference);
        entryRepository.save(entry);
        boolean alreadyLogged = pendingRepository.findByEntryIdOrderByCreatedAtDesc(entry.getId()).stream()
                .anyMatch(log -> log.getResolvedAt() == null);
        if (!alreadyLogged) {
            log(entry, first.reason(), difference, entry.getPendingDetail(), null, actor);
            notifier.notifyAccountants("Piece en attente d'equilibrage · " + entry.getProvisionalNumber(),
                    "La piece " + entry.getProvisionalNumber() + " du journal " + entry.getJournalCode() + " datee du " + entry.getEntryDate()
                            + " (" + entry.getLabel() + ") est en attente : " + entry.getPendingDetail()
                            + ". Elle n'entre pas dans la comptabilite tant qu'elle n'est pas corrigee.",
                    entry.getProvisionalNumber(), "/accounting/entries/pending", entry.getCreatedBy());
        }
    }

    private void log(AccEntry entry, PendingReason reason, BigDecimal difference, String detail, String resolution, String actor) {
        pendingRepository.save(AccPendingEntryLog.builder()
                .entryId(entry.getId())
                .reason(reason)
                .difference(difference)
                .detail(detail == null ? null : detail.length() > 1000 ? detail.substring(0, 1000) : detail)
                .notifiedAt(Instant.now())
                .resolution(resolution)
                .resolvedAt(resolution == null ? null : Instant.now())
                .resolvedBy(resolution == null ? null : actor)
                .build());
    }

    // --------------------------------------------------- corrections et suites

    public AccountingResponses.Entry reject(String reference, String reason) {
        AccEntry entry = entry(reference);
        if (entry.getStatus() != EntryStatus.PENDING_BALANCE && entry.getStatus() != EntryStatus.DRAFT) {
            throw new BadRequestException("Seule une piece en attente ou un brouillard se rejette");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez le motif du rejet");
        }
        String actor = actors.resolve("SYSTEM");
        entry.setStatus(EntryStatus.REJECTED);
        entry.setRejectedBy(actor);
        entry.setRejectedAt(Instant.now());
        entry.setRejectionReason(reason.trim());
        entryRepository.save(entry);
        pendingRepository.findByEntryIdOrderByCreatedAtDesc(entry.getId()).stream().filter(log -> log.getResolvedAt() == null)
                .forEach(log -> {
                    log.setResolvedAt(Instant.now());
                    log.setResolvedBy(actor);
                    log.setResolution("REJECTED");
                    pendingRepository.save(log);
                });
        if (entry.getPostingEventId() != null) {
            // Lot K1 (S3) : l'evenement repasse en echec avec le motif, il se regenere ou s'ecarte.
            publisher.publishEvent(new com.sni.bokaticowork.features.accounting.service.support.EntryRejectedEvent(
                    entry.getPostingEventId(), entry.getProvisionalNumber(), reason.trim()));
        }
        return toResponse(entry, List.of("Piece rejetee : elle reste consultable"));
    }

    /** Contre-passation demandee par une personne : piece inverse datee du jour (ou de la date donnee), liee a l'originale. */
    public AccountingResponses.Entry reverse(String reference, AccountingRequests.Reverse request) {
        return reverse(reference, request, false);
    }

    /**
     * Contre-passation.
     *
     * @param fromEngine annulation d'un fait metier par le moteur : ni seconde validation, ni retour
     *                   vers l'evenement d'origine (l'evenement d'annulation porte la contre-passation)
     */
    public AccountingResponses.Entry reverse(String reference, AccountingRequests.Reverse request, boolean fromEngine) {
        AccEntry original = entry(reference);
        if (original.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException(original.getStatus() == EntryStatus.REVERSED
                    ? "La piece " + original.getEntryNumber() + " est deja contre-passee par " + original.getReversedByEntry()
                    : "Seule une piece validee se contre-passe");
        }
        if (!fromEngine && !entryRepository.findByReversalOfAndStatus(original.getEntryNumber(), EntryStatus.DRAFT).isEmpty()) {
            throw new BadRequestException("Une contre-passation de " + original.getEntryNumber()
                    + " attend déjà la validation d'un autre comptable : validez-la ou rejetez-la");
        }
        String actor = actors.resolve("SYSTEM");
        // Lot K1 (S5) : la date du jour est celle de Brazzaville, pas celle du serveur.
        LocalDate date = request.date() == null ? today() : request.date();
        if (date.isBefore(original.getEntryDate())) {
            throw new BadRequestException("La contre-passation ne peut pas être datée avant la pièce " + original.getEntryNumber()
                    + " (" + original.getEntryDate().format(DAY) + ")");
        }
        AccJournal journal = periods.journal(original.getJournalCode());
        AccPeriod period = periods.periodFor(date, true);
        AccEntry reversal = entryRepository.save(AccEntry.builder()
                .provisionalNumber(sequenceGenerator.next("acc_provisional_entry"))
                .journalCode(journal.getCode())
                .fiscalYearCode(periods.fiscalYearOf(period).getCode())
                .periodCode(period.getCode())
                .entryDate(date)
                .label("Contre-passation de " + original.getEntryNumber() + " : " + request.reason().trim())
                .origin(EntryOrigin.REVERSAL)
                .sourceType(original.getSourceType())
                .sourceCode(original.getSourceCode())
                .status(EntryStatus.DRAFT)
                .reversalOf(original.getEntryNumber())
                .reversalReason(request.reason().trim())
                .acceptedUnbalanced(Boolean.TRUE.equals(original.getAcceptedUnbalanced()))
                .unbalancedAmount(original.getUnbalancedAmount() == null ? null : original.getUnbalancedAmount().negate())
                .createdBy(actor)
                .build());
        List<AccEntryLine> originalLines = lineRepository.findByEntryIdOrderByLineNoAsc(original.getId());
        List<AccEntryLine> swapped = new ArrayList<>();
        for (AccEntryLine line : originalLines) {
            swapped.add(AccEntryLine.builder().entryId(reversal.getId()).lineNo(line.getLineNo()).accountNumber(line.getAccountNumber())
                    .thirdPartyType(line.getThirdPartyType()).thirdPartyCode(line.getThirdPartyCode())
                    .label("Contre-passation : " + (line.getLabel() == null ? "" : line.getLabel()))
                    .debit(line.getCredit()).credit(line.getDebit()).currency(line.getCurrency()).foreignAmount(line.getForeignAmount())
                    .rate(line.getRate()).taxCode(line.getTaxCode()).taxBase(line.getTaxBase()).reference(original.getEntryNumber()).build());
        }
        lineRepository.saveAll(swapped);
        reversal.setTotalDebit(original.getTotalCredit());
        reversal.setTotalCredit(original.getTotalDebit());
        entryRepository.save(reversal);
        if (!fromEngine && aboveSecondValidation(reversal)) {
            // Lot K1 (S11) : une seule personne n'annule plus une piece au-dela du seuil.
            checkChronology(reversal, journal.getCode(), "La contre-passation");
            return toResponse(reversal, List.of(secondValidationMessage("Contre-passation")));
        }
        finalizeReversal(reversal, original, journal, actor, !fromEngine);
        return toResponse(reversal, List.of("Contre-passation validee sous le numero " + reversal.getEntryNumber()));
    }

    /**
     * Numerote la contre-passation et marque la piece d'origine. {@code notify} : une piece
     * d'evenement contre-passee hors du moteur fait passer l'evenement en {@code REVERSED} (lot K1, S1).
     */
    private void finalizeReversal(AccEntry reversal, AccEntry original, AccJournal journal, String actor, boolean notify) {
        if (original.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("La pièce " + original.getEntryNumber() + " n'est plus validée ("
                    + (original.getStatus() == EntryStatus.REVERSED ? "déjà contre-passée par " + original.getReversedByEntry() : original.getStatus())
                    + ") : rejetez ce brouillard de contre-passation");
        }
        AccPeriod period = periods.periodFor(reversal.getEntryDate(), true);
        reversal.setPeriodCode(period.getCode());
        reversal.setFiscalYearCode(periods.fiscalYearOf(period).getCode());
        checkChronology(reversal, journal.getCode(), "La contre-passation");
        List<AccEntryLine> swapped = lineRepository.findByEntryIdOrderByLineNoAsc(reversal.getId());
        // Equilibree par construction : elle reprend chaque ligne en echangeant debit et credit.
        numbering.assign(reversal, journal, swapped);
        reversal.setStatus(EntryStatus.VALIDATED);
        reversal.setValidatedBy(actor);
        reversal.setValidatedAt(Instant.now());
        entryRepository.save(reversal);
        original.setStatus(EntryStatus.REVERSED);
        original.setReversedByEntry(reversal.getEntryNumber());
        original.setReversalReason(reversal.getReversalReason());
        entryRepository.save(original);
        lettering.pairReversal(lineRepository.findByEntryIdOrderByLineNoAsc(original.getId()), swapped, actor);
        if (notify && original.getPostingEventId() != null) {
            publisher.publishEvent(new com.sni.bokaticowork.features.accounting.service.support.EntryReversedEvent(
                    original.getPostingEventId(), original.getProvisionalNumber(), reversal.getEntryNumber(), reversal.getReversalReason()));
        }
    }

    /**
     * Regularisation d'une piece acceptee desequilibree (mode ACCEPT) : une ecriture complementaire
     * apporte exactement la contrepartie manquante ; la piece d'origine perd alors son marqueur.
     * Lot K1 (S6) : datee du jour choisi, dans une periode ouverte.
     */
    public AccountingResponses.Entry regularize(String reference, AccountingRequests.Regularize request) {
        AccEntry original = entry(reference);
        if (!Boolean.TRUE.equals(original.getAcceptedUnbalanced()) || original.getStatus() == EntryStatus.DRAFT) {
            throw new BadRequestException("La piece " + reference + " n'est pas une piece acceptee desequilibree");
        }
        if (!entryRepository.findByRegularizationOfAndStatus(original.getEntryNumber(), EntryStatus.DRAFT).isEmpty()) {
            throw new BadRequestException("Une régularisation de " + original.getEntryNumber()
                    + " attend déjà la validation d'un autre comptable : validez-la ou rejetez-la");
        }
        String actor = actors.resolve("SYSTEM");
        AccJournal journal = periods.journal(original.getJournalCode());
        LocalDate date = request.date() == null ? today() : request.date();
        AccPeriod period = periods.periodFor(date, true);
        AccEntry complement = entryRepository.save(AccEntry.builder()
                .provisionalNumber(sequenceGenerator.next("acc_provisional_entry"))
                .journalCode(journal.getCode())
                .fiscalYearCode(periods.fiscalYearOf(period).getCode())
                .periodCode(period.getCode())
                .entryDate(date)
                .label(request.label().trim())
                .origin(EntryOrigin.REGULARIZATION)
                .status(EntryStatus.DRAFT)
                .regularizationOf(original.getEntryNumber())
                .createdBy(actor)
                .build());
        writeLines(complement, request.lines());
        regularizationChecks(complement, original, journal);
        if (aboveSecondValidation(complement)) {
            checkChronology(complement, journal.getCode(), "La régularisation");
            return toResponse(complement, List.of(secondValidationMessage("Régularisation")));
        }
        finalizeRegularization(complement, original, journal, actor);
        return toResponse(complement, List.of("Piece " + reference + " regularisee par " + complement.getEntryNumber()));
    }

    private void regularizationChecks(AccEntry complement, AccEntry original, AccJournal journal) {
        BigDecimal complementDifference = complement.getTotalDebit().subtract(complement.getTotalCredit());
        BigDecimal originalDifference = original.getUnbalancedAmount() == null ? BigDecimal.ZERO : original.getUnbalancedAmount();
        if (complementDifference.add(originalDifference).signum() != 0) {
            throw new BadRequestException("L'ecriture complementaire doit apporter exactement " + plain(originalDifference.negate())
                    + " XAF de difference pour solder l'ecart de la piece " + original.getEntryNumber());
        }
        List<AccEntryLine> lines = lineRepository.findByEntryIdOrderByLineNoAsc(complement.getId());
        checks(complement, journal, lines, true).stream().filter(issue -> issue.reason() != PendingReason.UNBALANCED).findFirst()
                .ifPresent(issue -> {
                    throw new BadRequestException(issue.detail());
                });
    }

    private void finalizeRegularization(AccEntry complement, AccEntry original, AccJournal journal, String actor) {
        if (!Boolean.TRUE.equals(original.getAcceptedUnbalanced())) {
            throw new BadRequestException("La pièce " + original.getEntryNumber() + " est déjà régularisée par " + original.getRegularizedBy()
                    + " : rejetez ce brouillard de régularisation");
        }
        AccPeriod period = periods.periodFor(complement.getEntryDate(), true);
        complement.setPeriodCode(period.getCode());
        complement.setFiscalYearCode(periods.fiscalYearOf(period).getCode());
        checkChronology(complement, journal.getCode(), "La régularisation");
        regularizationChecks(complement, original, journal);
        List<AccEntryLine> lines = lineRepository.findByEntryIdOrderByLineNoAsc(complement.getId());
        complement.setAcceptedUnbalanced(true);
        complement.setUnbalancedAmount(complement.getTotalDebit().subtract(complement.getTotalCredit()));
        numbering.assign(complement, journal, lines);
        complement.setStatus(EntryStatus.VALIDATED);
        complement.setValidatedBy(actor);
        complement.setValidatedAt(Instant.now());
        // Ecrite tout de suite : le controle en base admet la piece grace a son marqueur.
        entryRepository.saveAndFlush(complement);
        // Ensemble, les deux pieces s'equilibrent : les marqueurs tombent.
        complement.setAcceptedUnbalanced(false);
        entryRepository.save(complement);
        original.setAcceptedUnbalanced(false);
        original.setRegularizedBy(complement.getEntryNumber());
        entryRepository.save(original);
    }

    // ------------------------------------------------------ corrections (lot K1)

    /**
     * Lot K1 (S2, decision D2) : reclassement. Une piece OD liee a la piece d'origine sort chaque
     * montant du compte (ou du tiers) errone et le porte sur le bon ; la ventilation analytique suit
     * (celle d'origine, ou celle donnee). Une ligne lettree se delettre d'abord ; une ligne reclassee
     * en entier sur un compte lettrable est lettree avec sa sortie.
     */
    public AccountingResponses.Entry reclassify(String reference, AccountingRequests.Reclassify request) {
        AccEntry original = entry(reference);
        if (original.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException(original.getStatus() == EntryStatus.REVERSED
                    ? "La pièce " + original.getEntryNumber() + " est contre-passée par " + original.getReversedByEntry() + " : elle ne se reclasse plus"
                    : "Seule une pièce validée se reclasse ; un brouillard ou une pièce en attente se modifie");
        }
        String actor = actors.resolve("SYSTEM");
        AccJournal journal = periods.journal(settings.correctionJournal());
        LocalDate date = request.date() == null ? correctionDate(original, journal.getCode()) : request.date();
        if (date.isBefore(original.getEntryDate())) {
            throw new BadRequestException("Le reclassement ne peut pas être daté avant la pièce " + original.getEntryNumber()
                    + " (" + original.getEntryDate().format(DAY) + ")");
        }
        AccPeriod period = periods.periodFor(date, true);
        Map<Integer, AccEntryLine> byNumber = lineRepository.findByEntryIdOrderByLineNoAsc(original.getId()).stream()
                .collect(Collectors.toMap(AccEntryLine::getLineNo, Function.identity()));
        Set<Integer> seen = new HashSet<>();
        List<AccEntryLine> moves = new ArrayList<>();
        for (AccountingRequests.ReclassLine move : request.lines()) {
            AccEntryLine line = byNumber.get(move.lineNo());
            if (line == null) {
                throw new BadRequestException("La pièce " + original.getEntryNumber() + " n'a pas de ligne " + move.lineNo());
            }
            if (!seen.add(move.lineNo())) {
                throw new BadRequestException("La ligne " + move.lineNo() + " est indiquée deux fois");
            }
            String number = StringUtils.hasText(move.accountNumber()) ? move.accountNumber().trim() : line.getAccountNumber();
            AccAccount target = chart.find(number).orElseThrow(() -> new BadRequestException("Ligne " + move.lineNo()
                    + " : le compte " + number + " n'existe pas"));
            String thirdParty = move.thirdPartyCode() != null ? trim(move.thirdPartyCode())
                    : target.getThirdPartyType() != null ? line.getThirdPartyCode() : null;
            String analytic = move.analytic() != null ? AnalyticAllocator.write(move.analytic()) : line.getAnalyticJson();
            if (target.getNumber().equals(line.getAccountNumber()) && Objects.equals(thirdParty, line.getThirdPartyCode())) {
                throw new BadRequestException("Ligne " + move.lineNo() + " : indiquez un autre compte ou un autre tiers"
                        + " (une ventilation analytique seule se corrige par le reclassement analytique)");
            }
            BigDecimal lineAmount = line.getDebit().max(line.getCredit());
            BigDecimal amount = move.amount() == null ? lineAmount : money(move.amount());
            if (amount.signum() <= 0 || amount.compareTo(lineAmount) > 0) {
                throw new BadRequestException("Ligne " + move.lineNo() + " : le montant à reclasser est compris entre 0 et " + plain(lineAmount) + " XAF");
            }
            if (letteringLineRepository.matchedByLine(List.of(line.getId())).stream().anyMatch(row -> row[1] != null
                    && ((BigDecimal) row[1]).signum() > 0)) {
                throw new BadRequestException("Ligne " + move.lineNo() + " lettrée" + lettering.activeCode(line.getId()).map(code -> " (" + code + ")").orElse("")
                        + " : délettrez-la avant de la reclasser");
            }
            boolean debit = line.getDebit().signum() > 0;
            BigDecimal foreign = prorate(line.getForeignAmount(), amount, lineAmount);
            BigDecimal taxBase = prorate(line.getTaxBase(), amount, lineAmount);
            BigDecimal zero = BigDecimal.ZERO.setScale(4);
            String lineReference = StringUtils.hasText(line.getReference()) ? line.getReference() : original.getEntryNumber();
            moves.add(AccEntryLine.builder().accountNumber(line.getAccountNumber()).thirdPartyType(line.getThirdPartyType())
                    .thirdPartyCode(line.getThirdPartyCode()).label("Reclassement : sortie du " + line.getAccountNumber() + " (ligne " + line.getLineNo() + ")")
                    .debit(debit ? zero : amount).credit(debit ? amount : zero).currency(line.getCurrency()).foreignAmount(foreign)
                    .rate(line.getRate()).taxCode(line.getTaxCode()).taxBase(taxBase).dueDate(line.getDueDate()).reference(lineReference)
                    .analyticJson(line.getAnalyticJson()).build());
            moves.add(AccEntryLine.builder().accountNumber(target.getNumber()).thirdPartyType(target.getThirdPartyType())
                    .thirdPartyCode(thirdParty).label("Reclassement : vers le " + target.getNumber() + " (ligne " + line.getLineNo() + ")")
                    .debit(debit ? amount : zero).credit(debit ? zero : amount).currency(line.getCurrency()).foreignAmount(foreign)
                    .rate(line.getRate()).taxCode(line.getTaxCode()).taxBase(taxBase).dueDate(line.getDueDate()).reference(lineReference)
                    .analyticJson(analytic).build());
        }
        AccEntry entry = entryRepository.save(AccEntry.builder()
                .provisionalNumber(sequenceGenerator.next("acc_provisional_entry"))
                .journalCode(journal.getCode())
                .fiscalYearCode(periods.fiscalYearOf(period).getCode())
                .periodCode(period.getCode())
                .entryDate(date)
                .label(truncate("Reclassement de " + original.getEntryNumber() + " : " + request.reason().trim(), 500))
                .origin(EntryOrigin.CORRECTION)
                .sourceType(original.getSourceType())
                .sourceCode(original.getSourceCode())
                .status(EntryStatus.DRAFT)
                .correctionOf(original.getEntryNumber())
                .correctionKind("RECLASS")
                .changeReason(truncate(request.reason().trim(), 500))
                .createdBy(actor)
                .build());
        int lineNo = 1;
        for (AccEntryLine move : moves) {
            move.setEntryId(entry.getId());
            move.setLineNo(lineNo++);
        }
        lineRepository.saveAll(moves);
        entry.setTotalDebit(moves.stream().map(AccEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add));
        entry.setTotalCredit(moves.stream().map(AccEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add));
        entryRepository.save(entry);
        return submitCorrection(entry, journal, actor, "Reclassement");
    }

    /**
     * Lot K1 (S2) : piece juste d'une correction de piece manuelle, liee a la piece corrigee (la
     * contre-passation est faite par l'appelant, dans la meme transaction).
     */
    public AccountingResponses.Entry createCorrection(AccEntry original, AccountingRequests.Correct request, LocalDate date) {
        if (request.lines() == null || request.lines().size() < 2) {
            throw new BadRequestException("Indiquez les lignes de la pièce juste (au moins deux)");
        }
        String actor = actors.resolve("SYSTEM");
        AccJournal journal = periods.journal(StringUtils.hasText(request.journalCode()) ? request.journalCode() : original.getJournalCode());
        if (!Boolean.TRUE.equals(journal.getActive())) {
            throw new BadRequestException("Le journal " + journal.getCode() + " est desactive");
        }
        AccPeriod period = periods.periodFor(date, true);
        String label = StringUtils.hasText(request.label()) ? request.label().trim() : original.getLabel();
        AccEntry entry = entryRepository.save(AccEntry.builder()
                .provisionalNumber(sequenceGenerator.next("acc_provisional_entry"))
                .journalCode(journal.getCode())
                .fiscalYearCode(periods.fiscalYearOf(period).getCode())
                .periodCode(period.getCode())
                .entryDate(date)
                .documentDate(original.getDocumentDate())
                .label(truncate(label, 500))
                .origin(EntryOrigin.CORRECTION)
                .sourceType(original.getSourceType())
                .sourceCode(original.getSourceCode())
                .status(EntryStatus.DRAFT)
                .correctionOf(original.getEntryNumber())
                .correctionKind("CORRECTION")
                .changeReason(truncate(request.reason().trim(), 500))
                .createdBy(actor)
                .build());
        writeLines(entry, request.lines());
        return submitCorrection(entry, journal, actor, "Pièce juste");
    }

    private AccountingResponses.Entry submitCorrection(AccEntry entry, AccJournal journal, String actor, String what) {
        if (aboveSecondValidation(entry)) {
            checkChronology(entry, journal.getCode(), what);
            return toResponse(entry, List.of(secondValidationMessage(what)));
        }
        return toResponse(entry, runValidation(entry, journal, actor, true));
    }

    private static BigDecimal prorate(BigDecimal value, BigDecimal part, BigDecimal whole) {
        if (value == null || whole.signum() == 0 || part.compareTo(whole) == 0) {
            return value;
        }
        return value.multiply(part).divide(whole, 4, RoundingMode.HALF_UP);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    /** Lot K1 (S4) : lignes produites par le schema, gardees avant une modification a la main. */
    private static String snapshot(List<AccEntryLine> lines) {
        List<AccountingResponses.GeneratedLine> generated = lines.stream().map(line -> new AccountingResponses.GeneratedLine(line.getAccountNumber(),
                line.getThirdPartyCode(), line.getLabel(), line.getDebit(), line.getCredit(), line.getTaxCode(), line.getReference())).toList();
        try {
            return JSON.writeValueAsString(generated);
        } catch (Exception exception) {
            return null;
        }
    }

    private static List<AccountingResponses.GeneratedLine> generatedLines(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return List.of(JSON.readValue(json, AccountingResponses.GeneratedLine[].class));
        } catch (Exception exception) {
            return null;
        }
    }

    // ------------------------------------------------------------- lecture

    @Transactional(readOnly = true)
    public AccountingResponses.Entry get(String reference) {
        return toResponse(entry(reference), List.of());
    }

    @Transactional(readOnly = true)
    public List<AccountingResponses.Entry> search(String journal, EntryStatus status, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? today().withDayOfMonth(1) : from;
        LocalDate end = to == null ? today() : to;
        return toResponses(entryRepository.search(StringUtils.hasText(journal) ? journal.trim().toUpperCase(Locale.ROOT) : null, status, start, end));
    }

    @Transactional(readOnly = true)
    public List<AccountingResponses.Entry> pending() {
        return toResponses(entryRepository.findByStatusOrderByCreatedAtAsc(EntryStatus.PENDING_BALANCE));
    }

    public AccEntry entry(String reference) {
        if (!StringUtils.hasText(reference)) {
            throw new BadRequestException("Reference de piece manquante");
        }
        String value = reference.trim();
        return entryRepository.findByEntryNumber(value).or(() -> entryRepository.findByProvisionalNumber(value))
                .orElseThrow(() -> new ResourceNotFoundException("Piece comptable introuvable : " + reference));
    }

    List<AccountingResponses.Entry> toResponses(List<AccEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AccEntryLine>> lines = lineRepository.findByEntryIdInOrderByEntryIdAscLineNoAsc(
                        entries.stream().map(AccEntry::getId).toList()).stream()
                .collect(Collectors.groupingBy(AccEntryLine::getEntryId, LinkedHashMap::new, Collectors.toList()));
        return entries.stream().map(entry -> toResponse(entry, lines.getOrDefault(entry.getId(), List.of()), List.of())).toList();
    }

    AccountingResponses.Entry toResponse(AccEntry entry, List<String> messages) {
        return toResponse(entry, lineRepository.findByEntryIdOrderByLineNoAsc(entry.getId()), messages);
    }

    private AccountingResponses.Entry toResponse(AccEntry entry, List<AccEntryLine> lines, List<String> messages) {
        Map<String, AccAccount> accounts = accounts(lines);
        Map<Long, String> codes = new HashMap<>();
        Map<Long, BigDecimal> matched = new HashMap<>();
        if (!lines.isEmpty() && EntryNumbering.NUMBERED.contains(entry.getStatus())) {
            List<Long> ids = lines.stream().map(AccEntryLine::getId).toList();
            letteringLineRepository.matchedByLine(ids).forEach(row -> matched.put((Long) row[0], (BigDecimal) row[1]));
            if (!matched.isEmpty()) {
                for (AccEntryLine line : lines) {
                    if (matched.containsKey(line.getId())) {
                        lettering.activeCode(line.getId()).ifPresent(code -> codes.put(line.getId(), code));
                    }
                }
            }
        }
        List<AccountingResponses.EntryLine> lineResponses = lines.stream().map(line -> new AccountingResponses.EntryLine(line.getId(),
                line.getLineNo(), line.getAccountNumber(), accounts.containsKey(line.getAccountNumber()) ? accounts.get(line.getAccountNumber()).getLabel() : null,
                line.getThirdPartyType(), line.getThirdPartyCode(), line.getLabel(), line.getDebit(), line.getCredit(), line.getCurrency(),
                line.getForeignAmount(), line.getRate(), line.getTaxCode(), line.getTaxBase(), line.getDueDate(), line.getReference(),
                codes.get(line.getId()), matched.get(line.getId()))).toList();
        return new AccountingResponses.Entry(entry.getEntryNumber(), entry.getProvisionalNumber(), entry.getJournalCode(), entry.getFiscalYearCode(),
                entry.getPeriodCode(), entry.getEntryDate(), entry.getOriginalDate(), entry.getDocumentDate(), entry.getLabel(), entry.getOrigin(),
                entry.getSourceType(), entry.getSourceCode(), entry.getStatus(), entry.getAcceptedUnbalanced(), entry.getUnbalancedAmount(),
                entry.getPendingReason(), entry.getPendingDetail(), entry.getTotalDebit(), entry.getTotalCredit(),
                entry.getTotalDebit().subtract(entry.getTotalCredit()), entry.getCreatedBy(), entry.getCreatedAt(), entry.getValidatedBy(),
                entry.getValidatedAt(), entry.getReversalOf(), entry.getReversedByEntry(), entry.getReversalReason(), entry.getRegularizationOf(),
                entry.getRegularizedBy(), entry.getRejectedBy(), entry.getRejectionReason(), entry.getContentHash(), lineResponses, messages,
                entry.getCorrectionOf(), entry.getCorrectionKind(), entry.getCorrectedBy(), entry.getManuallyChangedBy(), entry.getManuallyChangedAt(),
                entry.getChangeReason(), generatedLines(entry.getGeneratedLinesJson()),
                entry.getStatus() == EntryStatus.DRAFT && HUMAN_ORIGINS.contains(entry.getOrigin()) && entry.getOrigin() != EntryOrigin.MANUAL
                        && aboveSecondValidation(entry));
    }

    private Map<String, AccAccount> accounts(List<AccEntryLine> lines) {
        Set<String> numbers = lines.stream().map(AccEntryLine::getAccountNumber).collect(Collectors.toSet());
        Map<String, AccAccount> accounts = new HashMap<>();
        for (String number : numbers) {
            accountRepository.findByNumber(number).ifPresent(account -> accounts.put(number, account));
        }
        return accounts;
    }

    static BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(4, RoundingMode.HALF_UP);
    }

    static String plain(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

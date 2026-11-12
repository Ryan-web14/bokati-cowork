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

    /** Probleme qui empeche la validation directe ; il suit la politique des pieces desequilibrees. */
    record Issue(PendingReason reason, String detail) {
    }

    // ------------------------------------------------------------- saisie

    public AccountingResponses.Entry create(AccountingRequests.Entry request) {
        String actor = actors.resolve(null);
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

    /** Derniere date validee d'un journal sur un exercice (ordre chronologique strict). */
    public java.util.Optional<LocalDate> lastValidatedDate(String journalCode, String fiscalYearCode) {
        return entryRepository.lastValidatedDate(journalCode, fiscalYearCode, EntryNumbering.NUMBERED);
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
        AccJournal journal = periods.journal(request.journalCode());
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
        String actor = actors.resolve(null);
        AccJournal journal = periods.journal(entry.getJournalCode());
        if (entry.getOrigin() == EntryOrigin.MANUAL) {
            BigDecimal threshold = settings.secondValidationThreshold();
            if (threshold != null && entry.getTotalDebit().max(entry.getTotalCredit()).compareTo(threshold) >= 0
                    && CashActorResolver.samePerson(actor, entry.getCreatedBy())) {
                throw new ForbiddenException("Au-dela de " + threshold.stripTrailingZeros().toPlainString()
                        + " XAF, une piece saisie a la main est validee par un autre comptable");
            }
        }
        List<String> messages = runValidation(entry, journal, actor, entry.getOrigin() == EntryOrigin.MANUAL);
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
        if (settings.strictChronology()) {
            entryRepository.lastValidatedDate(journal.getCode(), entry.getFiscalYearCode(), EntryNumbering.NUMBERED)
                    .filter(last -> entry.getEntryDate().isBefore(last))
                    .ifPresent(last -> {
                        throw new BadRequestException("Le journal " + journal.getCode() + " a deja une piece validee au " + last
                                + " : une piece ne peut pas etre datee avant (ordre chronologique strict)");
                    });
        }
        List<Issue> issues = checks(entry, journal, lines, manual);
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
        Map<String, AccAccount> accounts = accounts(lines);
        lines.stream().filter(line -> StringUtils.hasText(line.getThirdPartyCode()))
                .forEach(line -> chart.registerThirdParty(line.getAccountNumber(),
                        accounts.containsKey(line.getAccountNumber()) ? accounts.get(line.getAccountNumber()).getThirdPartyType() : null,
                        line.getThirdPartyCode(), null));
        messages.add("Piece validee sous le numero " + entry.getEntryNumber());
        return messages;
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
        String actor = actors.resolve(null);
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
        return toResponse(entry, List.of("Piece rejetee : elle reste consultable"));
    }

    /** Contre-passation : piece inverse datee du jour (ou de la date donnee), liee a l'originale. */
    public AccountingResponses.Entry reverse(String reference, AccountingRequests.Reverse request) {
        AccEntry original = entry(reference);
        if (original.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException(original.getStatus() == EntryStatus.REVERSED
                    ? "La piece " + original.getEntryNumber() + " est deja contre-passee par " + original.getReversedByEntry()
                    : "Seule une piece validee se contre-passe");
        }
        String actor = actors.resolve(null);
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
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
        if (settings.strictChronology()) {
            entryRepository.lastValidatedDate(journal.getCode(), reversal.getFiscalYearCode(), EntryNumbering.NUMBERED)
                    .filter(last -> date.isBefore(last))
                    .ifPresent(last -> {
                        throw new BadRequestException("Le journal " + journal.getCode() + " a deja une piece au " + last
                                + " : datez la contre-passation de ce jour ou apres");
                    });
        }
        // Equilibree par construction : elle reprend chaque ligne en echangeant debit et credit.
        numbering.assign(reversal, journal, swapped);
        reversal.setStatus(EntryStatus.VALIDATED);
        reversal.setValidatedBy(actor);
        reversal.setValidatedAt(Instant.now());
        entryRepository.save(reversal);
        original.setStatus(EntryStatus.REVERSED);
        original.setReversedByEntry(reversal.getEntryNumber());
        original.setReversalReason(request.reason().trim());
        entryRepository.save(original);
        lettering.pairReversal(originalLines, swapped, actor);
        return toResponse(reversal, List.of("Contre-passation validee sous le numero " + reversal.getEntryNumber()));
    }

    /**
     * Regularisation d'une piece acceptee desequilibree (mode ACCEPT) : une ecriture complementaire
     * apporte exactement la contrepartie manquante ; la piece d'origine perd alors son marqueur.
     */
    public AccountingResponses.Entry regularize(String reference, AccountingRequests.Regularize request) {
        AccEntry original = entry(reference);
        if (!Boolean.TRUE.equals(original.getAcceptedUnbalanced()) || original.getStatus() == EntryStatus.DRAFT) {
            throw new BadRequestException("La piece " + reference + " n'est pas une piece acceptee desequilibree");
        }
        String actor = actors.resolve(null);
        AccJournal journal = periods.journal(original.getJournalCode());
        AccPeriod period = periods.periodFor(LocalDate.now(), true);
        AccEntry complement = entryRepository.save(AccEntry.builder()
                .provisionalNumber(sequenceGenerator.next("acc_provisional_entry"))
                .journalCode(journal.getCode())
                .fiscalYearCode(periods.fiscalYearOf(period).getCode())
                .periodCode(period.getCode())
                .entryDate(LocalDate.now())
                .label(request.label().trim())
                .origin(EntryOrigin.REGULARIZATION)
                .status(EntryStatus.DRAFT)
                .regularizationOf(original.getEntryNumber())
                .createdBy(actor)
                .build());
        writeLines(complement, request.lines());
        BigDecimal complementDifference = complement.getTotalDebit().subtract(complement.getTotalCredit());
        BigDecimal originalDifference = original.getUnbalancedAmount() == null ? BigDecimal.ZERO : original.getUnbalancedAmount();
        if (complementDifference.add(originalDifference).signum() != 0) {
            throw new BadRequestException("L'ecriture complementaire doit apporter exactement " + plain(originalDifference.negate())
                    + " XAF de difference pour solder l'ecart de la piece " + reference);
        }
        List<AccEntryLine> lines = lineRepository.findByEntryIdOrderByLineNoAsc(complement.getId());
        checks(complement, journal, lines, true).stream().filter(issue -> issue.reason() != PendingReason.UNBALANCED).findFirst()
                .ifPresent(issue -> {
                    throw new BadRequestException(issue.detail());
                });
        complement.setAcceptedUnbalanced(true);
        complement.setUnbalancedAmount(complementDifference);
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
        return toResponse(complement, List.of("Piece " + reference + " regularisee par " + complement.getEntryNumber()));
    }

    // ------------------------------------------------------------- lecture

    @Transactional(readOnly = true)
    public AccountingResponses.Entry get(String reference) {
        return toResponse(entry(reference), List.of());
    }

    @Transactional(readOnly = true)
    public List<AccountingResponses.Entry> search(String journal, EntryStatus status, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
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
                entry.getRegularizedBy(), entry.getRejectedBy(), entry.getRejectionReason(), entry.getContentHash(), lineResponses, messages);
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

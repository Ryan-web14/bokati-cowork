package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.FiscalYearStatus;
import com.sni.bokaticowork.features.accounting.enums.JournalType;
import com.sni.bokaticowork.features.accounting.enums.NumberingReset;
import com.sni.bokaticowork.features.accounting.enums.PeriodStatus;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.model.AccJournal;
import com.sni.bokaticowork.features.accounting.model.AccPeriod;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccFiscalYearRepository;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPeriodRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Exercices, periodes et journaux (plan, sections 4.2 et 4.3).
 *
 * <p>Une periode verrouillee n'accepte plus que la saisie comptable ; une periode close n'accepte
 * plus rien. Une periode ne se verrouille ni ne se clot tant qu'il y reste une piece en attente
 * d'equilibrage, et ne se clot pas tant qu'il y reste un brouillard.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingPeriodService {

    private final AccFiscalYearRepository fiscalYearRepository;
    private final AccPeriodRepository periodRepository;
    private final AccJournalRepository journalRepository;
    private final AccEntryRepository entryRepository;
    private final AccountingChartService chart;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final org.springframework.beans.factory.ObjectProvider<BankService> bankService;

    // ------------------------------------------------------------- exercices

    @Transactional(readOnly = true)
    public List<AccountingResponses.FiscalYear> fiscalYears() {
        return fiscalYearRepository.findAllByOrderByStartDateAsc().stream().map(this::toResponse).toList();
    }

    /** Cree un exercice et ses periodes mensuelles. */
    public AccountingResponses.FiscalYear createFiscalYear(AccountingRequests.CreateFiscalYear request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new BadRequestException("La fin de l'exercice suit son debut");
        }
        if (fiscalYearRepository.findByCode(request.code().trim()).isPresent()) {
            throw new BadRequestException("L'exercice " + request.code() + " existe deja");
        }
        for (AccFiscalYear existing : fiscalYearRepository.findAllByOrderByStartDateAsc()) {
            if (!request.startDate().isAfter(existing.getEndDate()) && !request.endDate().isBefore(existing.getStartDate())) {
                throw new BadRequestException("L'exercice chevauche l'exercice " + existing.getCode());
            }
        }
        AccFiscalYear year = fiscalYearRepository.save(AccFiscalYear.builder().code(request.code().trim())
                .startDate(request.startDate()).endDate(request.endDate()).status(FiscalYearStatus.OPEN).build());
        LocalDate cursor = request.startDate();
        while (!cursor.isAfter(request.endDate())) {
            LocalDate end = cursor.withDayOfMonth(cursor.lengthOfMonth());
            if (end.isAfter(request.endDate())) {
                end = request.endDate();
            }
            String code = cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue());
            if (periodRepository.findByCode(code).isPresent()) {
                throw new BadRequestException("La periode " + code + " existe deja dans un autre exercice");
            }
            periodRepository.save(AccPeriod.builder().fiscalYearId(year.getId()).code(code).startDate(cursor).endDate(end)
                    .status(PeriodStatus.OPEN).build());
            cursor = end.plusDays(1);
        }
        return toResponse(year);
    }

    // -------------------------------------------------------------- periodes

    public AccountingResponses.Period lock(String code) {
        AccPeriod period = period(code);
        // Lot F10 : chaque compte bancaire suivi a son rapprochement valide a la fin de la periode.
        if (!"false".equalsIgnoreCase(settings.text("accounting.bank.reconciliation-before-lock", "true"))) {
            BankService banks = bankService.getIfAvailable();
            List<String> missing = banks == null ? List.of() : banks.missingReconciliations(period.getEndDate());
            if (!missing.isEmpty()) {
                throw new BadRequestException("Rapprochement bancaire non valide au " + period.getEndDate() + " pour : " + String.join(", ", missing));
            }
        }
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BadRequestException("Seule une periode ouverte se verrouille");
        }
        assertNoPendingIn(period, "verrouiller");
        period.setStatus(PeriodStatus.LOCKED);
        period.setLockedAt(Instant.now());
        period.setLockedBy(actors.resolve("SYSTEM"));
        return toResponse(periodRepository.save(period));
    }

    public AccountingResponses.Period unlock(String code) {
        AccPeriod period = period(code);
        if (period.getStatus() != PeriodStatus.LOCKED) {
            throw new BadRequestException("Seule une periode verrouillee se deverrouille ; une periode close ne se rouvre pas");
        }
        period.setStatus(PeriodStatus.OPEN);
        period.setLockedAt(null);
        period.setLockedBy(null);
        return toResponse(periodRepository.save(period));
    }

    public AccountingResponses.Period close(String code) {
        AccPeriod period = period(code);
        if (period.getStatus() == PeriodStatus.CLOSED) {
            return toResponse(period);
        }
        assertNoPendingIn(period, "clore");
        long drafts = entryRepository.countByPeriodAndStatuses(period.getCode(), EnumSet.of(EntryStatus.DRAFT));
        if (drafts > 0) {
            throw new BadRequestException("La periode " + period.getCode() + " a encore " + drafts
                    + " piece(s) en brouillard : validez-les ou supprimez-les avant de la clore");
        }
        // Les periodes se closent dans l'ordre : pas de trou dans les periodes closes.
        for (AccPeriod earlier : periodRepository.findAllByOrderByStartDateAsc()) {
            if (earlier.getStartDate().isBefore(period.getStartDate()) && earlier.getStatus() != PeriodStatus.CLOSED) {
                throw new BadRequestException("Closez d'abord la periode " + earlier.getCode());
            }
        }
        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedAt(Instant.now());
        period.setClosedBy(actors.resolve("SYSTEM"));
        return toResponse(periodRepository.save(period));
    }

    private void assertNoPendingIn(AccPeriod period, String verb) {
        long pending = entryRepository.countByPeriodAndStatuses(period.getCode(), EnumSet.of(EntryStatus.PENDING_BALANCE));
        if (pending > 0) {
            throw new BadRequestException("Impossible de " + verb + " la periode " + period.getCode() + " : " + pending
                    + " piece(s) attendent leur equilibrage");
        }
        if (settings.unbalancedBlocksPeriodClose()) {
            long accepted = entryRepository.findAcceptedUnbalanced(EnumSet.of(EntryStatus.VALIDATED, EntryStatus.REVERSED)).stream()
                    .filter(entry -> period.getCode().equals(entry.getPeriodCode())).count();
            if (accepted > 0) {
                throw new BadRequestException("Impossible de " + verb + " la periode " + period.getCode() + " : " + accepted
                        + " piece(s) acceptee(s) desequilibree(s) ne sont pas regularisees");
            }
        }
    }

    /**
     * Periode ou s'inscrit une piece datee de ce jour.
     *
     * @param manual une saisie comptable est admise dans une periode verrouillee
     */
    public AccPeriod periodFor(LocalDate date, boolean manual) {
        AccPeriod period = periodRepository.findCovering(date)
                .orElseThrow(() -> new BadRequestException("Aucun exercice ouvert ne couvre le " + date + " : creez l'exercice"));
        if (period.getStatus() == PeriodStatus.CLOSED) {
            throw new BadRequestException("La periode " + period.getCode() + " est close : elle n'accepte plus d'ecriture");
        }
        if (period.getStatus() == PeriodStatus.LOCKED && !manual) {
            throw new BadRequestException("La periode " + period.getCode() + " est verrouillee : seule la saisie comptable y est admise");
        }
        AccFiscalYear year = fiscalYearRepository.findById(period.getFiscalYearId()).orElseThrow();
        if (year.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BadRequestException("L'exercice " + year.getCode() + " est clos");
        }
        return period;
    }

    /**
     * Premiere periode ouverte qui couvre ce jour ou le suit : une ecriture automatique datee d'une
     * periode verrouillee ou close passe dans la premiere periode ouverte suivante (lot F2).
     */
    public AccPeriod firstOpenOnOrAfter(LocalDate date) {
        for (AccPeriod period : periodRepository.findAllByOrderByStartDateAsc()) {
            if (period.getEndDate().isBefore(date) || period.getStatus() != PeriodStatus.OPEN) {
                continue;
            }
            AccFiscalYear year = fiscalYearRepository.findById(period.getFiscalYearId()).orElseThrow();
            if (year.getStatus() != FiscalYearStatus.CLOSED) {
                return period;
            }
        }
        throw new BadRequestException("Aucune periode ouverte au " + date + " ou apres : creez l'exercice suivant");
    }

    public AccFiscalYear fiscalYearOf(AccPeriod period) {
        return fiscalYearRepository.findById(period.getFiscalYearId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<AccountingResponses.Period> periods() {
        return periodRepository.findAllByOrderByStartDateAsc().stream().map(this::toResponse).toList();
    }

    public AccPeriod period(String code) {
        return periodRepository.findByCode(code == null ? null : code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Periode comptable introuvable : " + code));
    }

    // -------------------------------------------------------------- journaux

    @Transactional(readOnly = true)
    public List<AccountingResponses.Journal> journals() {
        return journalRepository.findAllByOrderByCodeAsc().stream().map(AccountingPeriodService::toResponse).toList();
    }

    public AccountingResponses.Journal createJournal(AccountingRequests.CreateJournal request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9-]{1,30}")) {
            throw new BadRequestException("Un code de journal est fait de lettres, de chiffres et de tirets");
        }
        if (journalRepository.findByCode(code).isPresent()) {
            throw new BadRequestException("Le journal " + code + " existe deja");
        }
        boolean treasury = request.type() == JournalType.CASH || request.type() == JournalType.BANK || request.type() == JournalType.MOBILE_MONEY;
        if (treasury) {
            if (!StringUtils.hasText(request.treasuryAccountNumber())) {
                throw new BadRequestException("Un journal de tresorerie est lie a son compte de tresorerie");
            }
            var account = chart.account(request.treasuryAccountNumber());
            if (account.getTreasuryType() == null) {
                throw new BadRequestException("Le compte " + account.getNumber() + " n'est pas un compte de tresorerie");
            }
        }
        AccJournal journal = journalRepository.save(AccJournal.builder().code(code).label(request.label().trim()).type(request.type())
                .treasuryAccountNumber(treasury ? request.treasuryAccountNumber().trim() : null)
                .numberingReset(request.numberingReset() == null ? NumberingReset.YEAR : request.numberingReset())
                .autoValidate(request.autoValidate() == null || request.autoValidate())
                .allowedAccounts(join(request.allowedAccounts())).cashRegisterCode(request.cashRegisterCode())
                .createdBy(actors.resolve("SYSTEM")).build());
        return toResponse(journal);
    }

    public AccountingResponses.Journal updateJournal(String code, AccountingRequests.UpdateJournal request) {
        AccJournal journal = journal(code);
        if (StringUtils.hasText(request.label())) {
            journal.setLabel(request.label().trim());
        }
        if (request.autoValidate() != null) {
            journal.setAutoValidate(request.autoValidate());
        }
        if (request.allowedAccounts() != null) {
            journal.setAllowedAccounts(join(request.allowedAccounts()));
        }
        if (request.active() != null) {
            journal.setActive(request.active());
        }
        return toResponse(journalRepository.save(journal));
    }

    public AccJournal journal(String code) {
        return journalRepository.findByCode(code == null ? null : code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Journal introuvable : " + code));
    }

    // ------------------------------------------------------------------ outils

    private AccountingResponses.FiscalYear toResponse(AccFiscalYear year) {
        return new AccountingResponses.FiscalYear(year.getCode(), year.getStartDate(), year.getEndDate(), year.getStatus(),
                periodRepository.findByFiscalYearIdOrderByStartDateAsc(year.getId()).stream().map(this::toResponse).toList());
    }

    private AccountingResponses.Period toResponse(AccPeriod period) {
        return new AccountingResponses.Period(period.getCode(), period.getStartDate(), period.getEndDate(), period.getStatus(),
                period.getLockedAt(), period.getLockedBy(), period.getClosedAt(), period.getClosedBy(),
                entryRepository.countByPeriodAndStatuses(period.getCode(), EnumSet.of(EntryStatus.PENDING_BALANCE)),
                entryRepository.countByPeriodAndStatuses(period.getCode(), EnumSet.of(EntryStatus.DRAFT)));
    }

    static AccountingResponses.Journal toResponse(AccJournal journal) {
        return new AccountingResponses.Journal(journal.getCode(), journal.getLabel(), journal.getType(), journal.getTreasuryAccountNumber(),
                journal.getNumberingReset(), journal.getAutoValidate(), split(journal.getAllowedAccounts()), journal.getCashRegisterCode(),
                journal.getActive());
    }

    public static List<String> split(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> cleaned = new ArrayList<>();
        values.stream().filter(StringUtils::hasText).map(String::trim).distinct().forEach(cleaned::add);
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }
}

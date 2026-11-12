package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.AccountNature;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Etats du socle comptable (lot F1) : journal, grand livre, balance, controle de continuite des
 * numeros. Seules les pieces validees (et contre-passees, qui restent avec leur contre-passation)
 * y entrent ; le brouillard et les pieces en attente n'y figurent jamais.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountingReportService {

    private final AccEntryRepository entryRepository;
    private final AccEntryLineRepository lineRepository;
    private final AccAccountRepository accountRepository;
    private final AccFiscalYearRepository fiscalYearRepository;
    private final AccJournalRepository journalRepository;
    private final AccNumberingCounterRepository counterRepository;
    private final AccNumberingCheckRepository checkRepository;
    private final AccountingEntryService entries;
    private final AccountingLetteringService lettering;
    private final AccountingNotifier notifier;
    private final CashActorResolver actors;

    public AccountingResponses.JournalReport journal(String journalCode, LocalDate from, LocalDate to) {
        if (!StringUtils.hasText(journalCode)) {
            throw new BadRequestException("Precisez le journal");
        }
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        List<AccEntry> posted = entryRepository.search(journalCode.trim().toUpperCase(Locale.ROOT), null, start, end).stream()
                .filter(entry -> EntryNumbering.NUMBERED.contains(entry.getStatus()))
                .sorted(Comparator.comparing(AccEntry::getSequenceNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<AccountingResponses.Entry> responses = entries.toResponses(posted);
        BigDecimal debit = posted.stream().map(AccEntry::getTotalDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = posted.stream().map(AccEntry::getTotalCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccountingResponses.JournalReport(journalCode.trim().toUpperCase(Locale.ROOT), start, end, responses, debit, credit,
                Instant.now(), actors.current());
    }

    /**
     * Grand livre : par compte (et sous-comptes), solde d'ouverture au debut de la periode depuis le
     * debut de l'exercice, lignes et soldes progressifs.
     */
    public AccountingResponses.GeneralLedger generalLedger(String accountPrefix, String thirdPartyCode, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate yearStart = fiscalYearStart(start);
        String prefix = StringUtils.hasText(accountPrefix) ? accountPrefix.trim() : "";
        String thirdParty = StringUtils.hasText(thirdPartyCode) ? thirdPartyCode.trim() : null;
        List<AccEntryLine> before = start.isAfter(yearStart)
                ? lineRepository.findPosted(prefix, thirdParty, yearStart, start.minusDays(1), EntryNumbering.NUMBERED) : List.of();
        List<AccEntryLine> during = lineRepository.findPosted(prefix, thirdParty, start, end, EntryNumbering.NUMBERED);
        Map<Long, AccEntry> parents = new HashMap<>();
        entryRepository.findAllById(during.stream().map(AccEntryLine::getEntryId).distinct().toList()).forEach(entry -> parents.put(entry.getId(), entry));
        Map<String, AccAccount> accounts = accountRepository.findAllByOrderByNumberAsc().stream()
                .collect(Collectors.toMap(AccAccount::getNumber, account -> account));
        Map<String, BigDecimal> opening = new TreeMap<>();
        for (AccEntryLine line : before) {
            opening.merge(key(line, thirdParty), line.getDebit().subtract(line.getCredit()), BigDecimal::add);
        }
        Map<String, List<AccEntryLine>> grouped = new TreeMap<>();
        during.forEach(line -> grouped.computeIfAbsent(key(line, thirdParty), k -> new ArrayList<>()).add(line));
        opening.keySet().forEach(k -> grouped.computeIfAbsent(k, x -> new ArrayList<>()));
        List<AccountingResponses.LedgerAccount> result = new ArrayList<>();
        for (Map.Entry<String, List<AccEntryLine>> group : grouped.entrySet()) {
            String[] parts = group.getKey().split("\\|", -1);
            BigDecimal balance = opening.getOrDefault(group.getKey(), BigDecimal.ZERO);
            BigDecimal openingBalance = balance;
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            List<AccountingResponses.LedgerLine> lines = new ArrayList<>();
            for (AccEntryLine line : group.getValue()) {
                AccEntry entry = parents.get(line.getEntryId());
                balance = balance.add(line.getDebit()).subtract(line.getCredit());
                debit = debit.add(line.getDebit());
                credit = credit.add(line.getCredit());
                lines.add(new AccountingResponses.LedgerLine(entry.getEntryDate(), entry.getEntryNumber(), entry.getJournalCode(),
                        line.getLabel(), line.getThirdPartyCode(), line.getReference(), line.getDebit(), line.getCredit(), balance,
                        lettering.activeCode(line.getId()).orElse(null)));
            }
            AccAccount account = accounts.get(parts[0]);
            result.add(new AccountingResponses.LedgerAccount(parts[0], account == null ? null : account.getLabel(),
                    parts[1].isEmpty() ? null : parts[1], openingBalance, debit, credit, balance, lines));
        }
        return new AccountingResponses.GeneralLedger(start, end, result, Instant.now(), actors.current());
    }

    private static String key(AccEntryLine line, String thirdParty) {
        return line.getAccountNumber() + "|" + (thirdParty != null || line.getThirdPartyCode() != null ? Objects.toString(line.getThirdPartyCode(), "") : "");
    }

    /**
     * Balance a six colonnes : soldes d'ouverture (depuis le debut de l'exercice), mouvements de la
     * periode, soldes de cloture. Classes 1 a 8 par defaut ; {@code scope=9} pour la classe 9.
     */
    public AccountingResponses.TrialBalance trialBalance(LocalDate from, LocalDate to, String scope) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate yearStart = fiscalYearStart(start);
        boolean classNine = "9".equals(scope);
        Map<String, BigDecimal[]> opening = totals(start.isAfter(yearStart) ? lineRepository.totalsByAccount(yearStart, start.minusDays(1), EntryNumbering.NUMBERED) : List.of());
        Map<String, BigDecimal[]> movements = totals(lineRepository.totalsByAccount(start, end, EntryNumbering.NUMBERED));
        Map<String, AccAccount> accounts = accountRepository.findAllByOrderByNumberAsc().stream()
                .collect(Collectors.toMap(AccAccount::getNumber, account -> account));
        Set<String> numbers = new TreeSet<>();
        numbers.addAll(opening.keySet());
        numbers.addAll(movements.keySet());
        List<AccountingResponses.TrialBalanceLine> lines = new ArrayList<>();
        BigDecimal[] sums = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (String number : numbers) {
            boolean nine = number.startsWith("9");
            if (nine != classNine) {
                continue;
            }
            BigDecimal[] open = opening.getOrDefault(number, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal[] move = movements.getOrDefault(number, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal openingNet = open[0].subtract(open[1]);
            BigDecimal closingNet = openingNet.add(move[0]).subtract(move[1]);
            BigDecimal[] row = new BigDecimal[]{positive(openingNet), positive(openingNet.negate()), move[0], move[1],
                    positive(closingNet), positive(closingNet.negate())};
            for (int index = 0; index < 6; index++) {
                sums[index] = sums[index].add(row[index]);
            }
            AccAccount account = accounts.get(number);
            lines.add(new AccountingResponses.TrialBalanceLine(number, account == null ? null : account.getLabel(), number.substring(0, 1),
                    row[0], row[1], row[2], row[3], row[4], row[5]));
        }
        AccountingResponses.TrialBalanceLine totals = new AccountingResponses.TrialBalanceLine(null, "Totaux", null, sums[0], sums[1],
                sums[2], sums[3], sums[4], sums[5]);
        BigDecimal imbalance = sums[2].subtract(sums[3]).add(sums[0].subtract(sums[1]));
        // En mode ACCEPT, la balance peut etre desequilibree : l'ecart et ses pieces s'affichent en tete.
        List<String> culprits = imbalance.signum() == 0 ? List.of()
                : entryRepository.findAcceptedUnbalanced(EntryNumbering.NUMBERED).stream().map(AccEntry::getEntryNumber).toList();
        return new AccountingResponses.TrialBalance(start, end, classNine ? "9" : "1-8", lines, totals, imbalance.signum() == 0,
                imbalance, culprits, Instant.now(), actors.current());
    }

    private static Map<String, BigDecimal[]> totals(List<Object[]> rows) {
        Map<String, BigDecimal[]> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], new BigDecimal[]{(BigDecimal) row[1], (BigDecimal) row[2]});
        }
        return result;
    }

    private static BigDecimal positive(BigDecimal value) {
        return value.signum() > 0 ? value : BigDecimal.ZERO;
    }

    private LocalDate fiscalYearStart(LocalDate date) {
        return fiscalYearRepository.findAllByOrderByStartDateAsc().stream()
                .filter(year -> !date.isBefore(year.getStartDate()) && !date.isAfter(year.getEndDate()))
                .map(AccFiscalYear::getStartDate)
                .findFirst()
                .orElse(date.withDayOfYear(1));
    }

    // ------------------------------------------------ continuite des numeros

    /**
     * Controle de continuite d'un journal sur un exercice : aucun numero manquant ni en double, le
     * compteur egal au dernier numero, la chaine d'empreintes intacte.
     */
    @Transactional
    public AccountingResponses.NumberingCheck checkContinuity(String journalCode, String fiscalYearCode) {
        List<AccEntry> numbered = entryRepository.findNumbered(journalCode, fiscalYearCode, EntryNumbering.NUMBERED);
        AccJournal journal = journalRepository.findByCode(journalCode).orElseThrow(() -> new BadRequestException("Journal inconnu : " + journalCode));
        boolean byPeriod = journal.getNumberingReset() == com.sni.bokaticowork.features.accounting.enums.NumberingReset.PERIOD;
        Map<String, List<AccEntry>> scopes = numbered.stream().collect(Collectors.groupingBy(entry -> byPeriod ? entry.getPeriodCode() : "",
                TreeMap::new, Collectors.toList()));
        List<Integer> gaps = new ArrayList<>();
        List<Integer> duplicates = new ArrayList<>();
        boolean chainOk = true;
        String brokenAt = null;
        Integer first = null;
        Integer last = null;
        int counterTotal = 0;
        for (Map.Entry<String, List<AccEntry>> scope : scopes.entrySet()) {
            List<AccEntry> list = scope.getValue().stream().sorted(Comparator.comparing(AccEntry::getSequenceNumber)).toList();
            Set<Integer> seen = new HashSet<>();
            int expected = 1;
            String previousHash = EntryNumbering.genesis(journalCode, byPeriod ? scope.getKey() : fiscalYearCode);
            for (AccEntry entry : list) {
                int number = entry.getSequenceNumber();
                if (!seen.add(number)) {
                    duplicates.add(number);
                }
                while (expected < number) {
                    gaps.add(expected++);
                }
                expected = number + 1;
                first = first == null ? number : Math.min(first, number);
                last = last == null ? number : Math.max(last, number);
                List<AccEntryLine> lines = lineRepository.findByEntryIdOrderByLineNoAsc(entry.getId());
                String recomputed = EntryNumbering.hash(entry, lines, entry.getPreviousHash());
                if (chainOk && (!Objects.equals(entry.getPreviousHash(), previousHash) || !Objects.equals(recomputed, entry.getContentHash()))) {
                    chainOk = false;
                    brokenAt = entry.getEntryNumber();
                }
                previousHash = entry.getContentHash();
            }
            counterTotal += counterRepository.findByJournalCodeAndFiscalYearCode(journalCode, fiscalYearCode).stream()
                    .filter(counter -> counter.getPeriodCode().equals(scope.getKey()))
                    .mapToInt(AccNumberingCounter::getLastNumber).sum();
            if (!list.isEmpty() && counterRepository.findByJournalCodeAndFiscalYearCode(journalCode, fiscalYearCode).stream()
                    .filter(counter -> counter.getPeriodCode().equals(scope.getKey()))
                    .noneMatch(counter -> counter.getLastNumber() == list.get(list.size() - 1).getSequenceNumber())) {
                chainOk = chainOk && false;
                if (brokenAt == null) {
                    brokenAt = "compteur";
                }
            }
        }
        boolean ok = gaps.isEmpty() && duplicates.isEmpty() && chainOk;
        AccNumberingCheck check = checkRepository.save(AccNumberingCheck.builder().journalCode(journalCode).fiscalYearCode(fiscalYearCode)
                .checkedBy(actors.current() == null ? "SYSTEM" : actors.current()).firstNumber(first).lastNumber(last).entryCount(numbered.size())
                .counterValue(counterTotal).gaps(gaps.isEmpty() ? null : gaps.toString()).duplicates(duplicates.isEmpty() ? null : duplicates.toString())
                .chainOk(chainOk).ok(ok).build());
        if (!ok) {
            notifier.notifyAccountants("Anomalie de numerotation · " + journalCode + " " + fiscalYearCode,
                    "Le controle de continuite du journal " + journalCode + " pour l'exercice " + fiscalYearCode + " a trouve : "
                            + (gaps.isEmpty() ? "" : "numeros manquants " + gaps + " ; ") + (duplicates.isEmpty() ? "" : "doublons " + duplicates + " ; ")
                            + (chainOk ? "" : "chaine d'empreintes rompue a " + brokenAt + ". ") + "La cloture est bloquee.",
                    journalCode, "/accounting/numbering", null);
        }
        return new AccountingResponses.NumberingCheck(journalCode, fiscalYearCode, check.getCreatedAt(), check.getCheckedBy(), first, last,
                numbered.size(), counterTotal, gaps, duplicates, chainOk, ok, brokenAt);
    }

    /** Controle de tous les journaux de tous les exercices ouverts (traitement de nuit). */
    @Transactional
    public List<AccountingResponses.NumberingCheck> checkAll() {
        List<AccountingResponses.NumberingCheck> results = new ArrayList<>();
        for (AccFiscalYear year : fiscalYearRepository.findAllByOrderByStartDateAsc()) {
            for (AccJournal journal : journalRepository.findAllByOrderByCodeAsc()) {
                try {
                    results.add(checkContinuity(journal.getCode(), year.getCode()));
                } catch (RuntimeException exception) {
                    log.error("Comptabilite · controle de continuite en echec pour {} {} : {}", journal.getCode(), year.getCode(),
                            exception.getMessage(), exception);
                }
            }
        }
        return results;
    }

    static boolean isGeneral(AccAccount account) {
        return account.getNature() != AccountNature.ANALYTIC && account.getNature() != AccountNature.OFF_BALANCE;
    }

    static boolean posted(EntryStatus status) {
        return EntryNumbering.NUMBERED.contains(status);
    }
}

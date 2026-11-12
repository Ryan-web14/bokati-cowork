package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.AccountSide;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.LetteringKind;
import com.sni.bokaticowork.features.accounting.enums.LetteringStatus;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccEntryLine;
import com.sni.bokaticowork.features.accounting.model.AccLettering;
import com.sni.bokaticowork.features.accounting.model.AccLetteringLine;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccLetteringLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccLetteringRepository;
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
import java.util.stream.Collectors;

/**
 * Lettrage (plan, section 4.7) : rapprochement, sur un compte lettrable et pour un meme tiers, des
 * lignes qui se compensent. Manuel, total ou partiel, avec ecart de reglement sous un seuil ;
 * delettrage trace. Une contre-passation se lettre avec sa piece d'origine.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingLetteringService {

    private final AccLetteringRepository letteringRepository;
    private final AccLetteringLineRepository letteringLineRepository;
    private final AccEntryLineRepository lineRepository;
    private final AccEntryRepository entryRepository;
    private final AccountingChartService chart;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    @Lazy
    private final AccountingEntryService entries;

    /** Lignes encore ouvertes (non lettrees ou partiellement) d'un compte lettrable, pour un tiers. */
    @Transactional(readOnly = true)
    public List<AccountingResponses.OpenLine> openLines(String accountNumber, String thirdPartyCode) {
        AccAccount account = chart.account(accountNumber);
        List<AccEntryLine> lines = lineRepository.findPosted(account.getNumber(), StringUtils.hasText(thirdPartyCode) ? thirdPartyCode.trim() : null,
                        LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31), EntryNumbering.NUMBERED).stream()
                .filter(line -> line.getAccountNumber().equals(account.getNumber()))
                .toList();
        Map<Long, BigDecimal> matched = matched(lines.stream().map(AccEntryLine::getId).toList());
        Map<Long, AccEntry> parents = parents(lines);
        List<AccountingResponses.OpenLine> open = new ArrayList<>();
        for (AccEntryLine line : lines) {
            BigDecimal remaining = amount(line).subtract(matched.getOrDefault(line.getId(), BigDecimal.ZERO));
            if (remaining.signum() > 0) {
                AccEntry entry = parents.get(line.getEntryId());
                open.add(new AccountingResponses.OpenLine(line.getId(), entry.getEntryNumber(), entry.getEntryDate(), entry.getJournalCode(),
                        line.getLabel(), line.getReference(), line.getThirdPartyCode(), line.getDebit(), line.getCredit(), remaining,
                        line.getDebit().signum() > 0 ? AccountSide.DEBIT : AccountSide.CREDIT));
            }
        }
        return open;
    }

    public AccountingResponses.Lettering letter(AccountingRequests.Letter request) {
        return letter(request, false);
    }

    /**
     * {@code exchange} : l'ecart vient du change (les montants en devise se soldent) ; il est passe
     * en perte ou gain de change (676 / 776), sans seuil.
     */
    private AccountingResponses.Lettering letter(AccountingRequests.Letter request, boolean exchange) {
        AccAccount account = chart.account(request.accountNumber());
        if (!Boolean.TRUE.equals(account.getReconcilable())) {
            throw new BadRequestException("Le compte " + account.getNumber() + " n'est pas lettrable");
        }
        String thirdParty = StringUtils.hasText(request.thirdPartyCode()) ? request.thirdPartyCode().trim() : null;
        String actor = actors.resolve("SYSTEM");
        List<Long> ids = request.lines().stream().map(AccountingRequests.LetterLine::lineId).distinct().toList();
        if (ids.size() < 2) {
            throw new BadRequestException("Un lettrage rapproche au moins deux lignes");
        }
        Map<Long, AccEntryLine> lines = lineRepository.findAllById(ids).stream().collect(Collectors.toMap(AccEntryLine::getId, line -> line));
        Map<Long, AccEntry> parents = parents(new ArrayList<>(lines.values()));
        Map<Long, BigDecimal> already = matched(ids);
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        for (AccountingRequests.LetterLine requested : request.lines()) {
            AccEntryLine line = lines.get(requested.lineId());
            if (line == null) {
                throw new ResourceNotFoundException("Ligne d'ecriture introuvable : " + requested.lineId());
            }
            AccEntry parent = parents.get(line.getEntryId());
            if (parent == null || !EntryNumbering.NUMBERED.contains(parent.getStatus())) {
                throw new BadRequestException("Seules les lignes de pieces validees se lettrent");
            }
            if (!line.getAccountNumber().equals(account.getNumber()) || !Objects.equals(thirdParty, line.getThirdPartyCode())) {
                throw new BadRequestException("Toutes les lignes d'un lettrage sont sur le compte " + account.getNumber()
                        + (thirdParty == null ? "" : " et pour le tiers " + thirdParty));
            }
            BigDecimal remaining = amount(line).subtract(already.getOrDefault(line.getId(), BigDecimal.ZERO));
            BigDecimal value = requested.amount() == null ? remaining : requested.amount().setScale(4, RoundingMode.HALF_UP);
            if (value.signum() <= 0 || value.compareTo(remaining) > 0) {
                throw new BadRequestException("Ligne " + line.getId() + " : le montant rapproche depasse son reste (" + plain(remaining) + ")");
            }
            amounts.put(line.getId(), value);
            if (line.getDebit().signum() > 0) {
                debit = debit.add(value);
            } else {
                credit = credit.add(value);
            }
        }
        BigDecimal difference = debit.subtract(credit);
        LetteringStatus status = LetteringStatus.COMPLETE;
        String differenceEntry = null;
        if (difference.signum() != 0) {
            if (Boolean.TRUE.equals(request.withDifference())) {
                if (!exchange && difference.abs().compareTo(settings.letteringDifferenceThreshold()) > 0) {
                    throw new BadRequestException("L'ecart de " + plain(difference.abs()) + " XAF depasse le seuil d'ecart de reglement ("
                            + plain(settings.letteringDifferenceThreshold()) + " XAF)");
                }
                AccEntry gap = differenceEntry(account, thirdParty, difference, actor, exchange);
                differenceEntry = gap.getEntryNumber();
                AccEntryLine gapLine = lineRepository.findByEntryIdOrderByLineNoAsc(gap.getId()).stream()
                        .filter(line -> line.getAccountNumber().equals(account.getNumber())).findFirst().orElseThrow();
                amounts.put(gapLine.getId(), difference.abs());
                if (gapLine.getDebit().signum() > 0) {
                    debit = debit.add(difference.abs());
                } else {
                    credit = credit.add(difference.abs());
                }
            } else if (Boolean.TRUE.equals(request.partial())) {
                // Partiel : on ne rapproche que le montant commun ; chaque ligne garde son reste.
                status = LetteringStatus.PARTIAL;
                BigDecimal common = debit.min(credit);
                amounts = scaleTo(amounts, lines, common);
                debit = common;
                credit = common;
            } else {
                throw new BadRequestException("Les lignes ne se soldent pas (difference " + plain(difference)
                        + ") : choisissez un lettrage partiel, ou avec ecart de reglement");
            }
        }
        AccLettering lettering = letteringRepository.save(AccLettering.builder()
                .code(nextCode(account.getNumber(), thirdParty))
                .accountNumber(account.getNumber())
                .thirdPartyCode(thirdParty)
                .kind(LetteringKind.MANUAL)
                .status(status)
                .totalDebit(debit)
                .totalCredit(credit)
                .difference(difference)
                .differenceEntryNumber(differenceEntry)
                .letteredBy(actor)
                .build());
        amounts.forEach((lineId, value) -> letteringLineRepository.save(AccLetteringLine.builder()
                .letteringId(lettering.getId()).entryLineId(lineId).matchedAmount(value).build()));
        return toResponse(lettering);
    }

    /** Repartit le montant commun d'un lettrage partiel : debits puis credits, dans l'ordre. */
    private static Map<Long, BigDecimal> scaleTo(Map<Long, BigDecimal> amounts, Map<Long, AccEntryLine> lines, BigDecimal common) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal debitLeft = common;
        BigDecimal creditLeft = common;
        for (Map.Entry<Long, BigDecimal> entry : amounts.entrySet()) {
            boolean isDebit = lines.get(entry.getKey()).getDebit().signum() > 0;
            BigDecimal left = isDebit ? debitLeft : creditLeft;
            BigDecimal take = entry.getValue().min(left);
            if (take.signum() > 0) {
                result.put(entry.getKey(), take);
            }
            if (isDebit) {
                debitLeft = debitLeft.subtract(take);
            } else {
                creditLeft = creditLeft.subtract(take);
            }
        }
        return result;
    }

    /**
     * Ecriture d'ecart de reglement en partie double : perte D 658 / C compte du tiers, gain
     * D compte du tiers / C 758.
     */
    private AccEntry differenceEntry(AccAccount account, String thirdParty, BigDecimal difference, String actor, boolean exchange) {
        BigDecimal value = difference.abs();
        boolean debitHeavy = difference.signum() > 0;
        String label = exchange ? "Ecart de change" : "Ecart de reglement";
        String loss = exchange ? settings.text("accounting.exchange.loss-account", "676") : settings.letteringLossAccount();
        String gain = exchange ? settings.text("accounting.exchange.gain-account", "776") : settings.letteringGainAccount();
        List<AccountingRequests.EntryLine> lines = debitHeavy
                ? List.of(new AccountingRequests.EntryLine(loss, null, label, value, null, null, null, null, null, null, null, null),
                new AccountingRequests.EntryLine(account.getNumber(), thirdParty, label, null, value, null, null, null, null, null, null, null))
                : List.of(new AccountingRequests.EntryLine(account.getNumber(), thirdParty, label, value, null, null, null, null, null, null, null, null),
                new AccountingRequests.EntryLine(gain, null, label, null, value, null, null, null, null, null, null, null));
        AccEntry entry = entries.post("OD", LocalDate.now(), label + " au lettrage", EntryOrigin.MANUAL, "LETTERING",
                account.getNumber() + (thirdParty == null ? "" : "/" + thirdParty), lines, actor);
        if (entry.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture d'ecart de reglement n'a pas pu etre validee : " + entry.getPendingDetail());
        }
        return entry;
    }

    /**
     * Lettrage automatique par reference (lot F4) : les lignes ouvertes d'un compte et d'un tiers
     * qui portent la meme reference (numero de facture) se lettrent ; complet si elles se soldent,
     * partiel sinon. Rien si un seul cote est present.
     */
    public java.util.Optional<AccountingResponses.Lettering> autoLetterByReference(String accountNumber, String thirdPartyCode, String reference) {
        if (!StringUtils.hasText(reference)) {
            return java.util.Optional.empty();
        }
        List<AccountingResponses.OpenLine> open = openLines(accountNumber, thirdPartyCode).stream()
                .filter(line -> reference.equals(line.reference()) && java.util.Objects.equals(thirdPartyCode, line.thirdPartyCode()))
                .toList();
        boolean hasDebit = open.stream().anyMatch(line -> line.side() == AccountSide.DEBIT);
        boolean hasCredit = open.stream().anyMatch(line -> line.side() == AccountSide.CREDIT);
        if (!hasDebit || !hasCredit) {
            return java.util.Optional.empty();
        }
        BigDecimal debit = open.stream().filter(line -> line.side() == AccountSide.DEBIT).map(AccountingResponses.OpenLine::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = open.stream().filter(line -> line.side() == AccountSide.CREDIT).map(AccountingResponses.OpenLine::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Ecart en XAF : partiel, sauf si les montants en devise se soldent (ecart de change, solde en 676 / 776).
        boolean exchange = debit.compareTo(credit) != 0 && foreignBalanced(open);
        AccountingResponses.Lettering result = letter(new AccountingRequests.Letter(accountNumber, thirdPartyCode,
                open.stream().map(line -> new AccountingRequests.LetterLine(line.lineId(), null)).toList(),
                debit.compareTo(credit) != 0 && !exchange, exchange), exchange);
        letteringRepository.findById(result.id()).ifPresent(lettering -> {
            lettering.setKind(LetteringKind.AUTO_REFERENCE);
            letteringRepository.save(lettering);
        });
        return java.util.Optional.of(result);
    }

    /**
     * Vrai quand toutes les lignes sont dans la meme devise etrangere et que leurs restes en devise
     * (au prorata du reste en XAF) se soldent : l'ecart en XAF est alors un ecart de change.
     */
    private boolean foreignBalanced(List<AccountingResponses.OpenLine> open) {
        Map<Long, AccEntryLine> lines = lineRepository.findAllById(open.stream().map(AccountingResponses.OpenLine::lineId).toList()).stream()
                .collect(Collectors.toMap(AccEntryLine::getId, line -> line));
        String currency = null;
        BigDecimal balance = BigDecimal.ZERO;
        for (AccountingResponses.OpenLine item : open) {
            AccEntryLine line = lines.get(item.lineId());
            if (line == null || !StringUtils.hasText(line.getCurrency()) || line.getForeignAmount() == null || line.getForeignAmount().signum() == 0
                    || "XAF".equalsIgnoreCase(line.getCurrency())) {
                return false;
            }
            if (currency != null && !currency.equalsIgnoreCase(line.getCurrency())) {
                return false;
            }
            currency = line.getCurrency();
            BigDecimal full = amount(line);
            BigDecimal foreign = full.signum() == 0 ? BigDecimal.ZERO
                    : line.getForeignAmount().abs().multiply(item.remaining()).divide(full, 4, RoundingMode.HALF_UP);
            balance = item.side() == AccountSide.DEBIT ? balance.add(foreign) : balance.subtract(foreign);
        }
        return balance.abs().compareTo(new BigDecimal("0.01")) <= 0;
    }

    /** Proposition de lettrage : deux lignes du meme tiers, de sens oppose, au meme reste, sans reference commune. */
    public record Proposal(String accountNumber, String thirdPartyCode, AccountingResponses.OpenLine debit, AccountingResponses.OpenLine credit) {
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Proposal> proposals(String accountNumber, int limit) {
        List<AccountingResponses.OpenLine> open = openLines(accountNumber, null);
        Map<String, List<AccountingResponses.OpenLine>> byParty = open.stream().filter(line -> line.thirdPartyCode() != null)
                .collect(Collectors.groupingBy(AccountingResponses.OpenLine::thirdPartyCode, LinkedHashMap::new, Collectors.toList()));
        List<Proposal> proposals = new ArrayList<>();
        for (Map.Entry<String, List<AccountingResponses.OpenLine>> party : byParty.entrySet()) {
            List<AccountingResponses.OpenLine> credits = new ArrayList<>(party.getValue().stream().filter(line -> line.side() == AccountSide.CREDIT).toList());
            for (AccountingResponses.OpenLine debit : party.getValue()) {
                if (debit.side() != AccountSide.DEBIT) {
                    continue;
                }
                for (AccountingResponses.OpenLine credit : credits) {
                    if (credit.remaining().compareTo(debit.remaining()) == 0 && !java.util.Objects.equals(credit.reference(), debit.reference())) {
                        proposals.add(new Proposal(accountNumber, party.getKey(), debit, credit));
                        credits.remove(credit);
                        break;
                    }
                }
                if (proposals.size() >= limit) {
                    return proposals;
                }
            }
        }
        return proposals;
    }

    public AccountingResponses.Lettering unletter(Long letteringId, String reason) {
        AccLettering lettering = letteringRepository.findById(letteringId)
                .orElseThrow(() -> new ResourceNotFoundException("Lettrage introuvable : " + letteringId));
        if (lettering.getStatus() == LetteringStatus.UNLETTERED) {
            throw new BadRequestException("Ce lettrage est deja defait");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez le motif du delettrage");
        }
        String actor = actors.resolve("SYSTEM");
        lettering.setStatus(LetteringStatus.UNLETTERED);
        lettering.setUnletteredAt(Instant.now());
        lettering.setUnletteredBy(actor);
        lettering.setUnletteringReason(reason.trim());
        letteringRepository.save(lettering);
        // L'ecriture d'ecart generee au lettrage est contre-passee ; le delettrage ne modifie aucune ecriture.
        if (lettering.getDifferenceEntryNumber() != null) {
            entryRepository.findByEntryNumber(lettering.getDifferenceEntryNumber())
                    .filter(entry -> entry.getStatus() == EntryStatus.VALIDATED)
                    .ifPresent(entry -> entries.reverse(entry.getEntryNumber(), new AccountingRequests.Reverse("Delettrage : " + reason.trim(), null)));
        }
        return toResponse(lettering);
    }

    /**
     * Une contre-passation se lettre avec la piece d'origine, ligne a ligne, sur les comptes
     * lettrables ; un lettrage existant de la piece d'origine est d'abord defait.
     */
    public void pairReversal(List<AccEntryLine> originalLines, List<AccEntryLine> reversalLines, String actor) {
        Map<Integer, AccEntryLine> byNumber = reversalLines.stream().collect(Collectors.toMap(AccEntryLine::getLineNo, line -> line));
        for (AccEntryLine original : originalLines) {
            AccAccount account = chart.find(original.getAccountNumber()).orElse(null);
            if (account == null || !Boolean.TRUE.equals(account.getReconcilable())) {
                continue;
            }
            for (AccLetteringLine link : letteringLineRepository.findByEntryLineId(original.getId())) {
                {
                    letteringRepository.findById(link.getLetteringId())
                            .filter(existing -> existing.getStatus() != LetteringStatus.UNLETTERED)
                            .ifPresent(existing -> {
                                existing.setStatus(LetteringStatus.UNLETTERED);
                                existing.setUnletteredAt(Instant.now());
                                existing.setUnletteredBy(actor);
                                existing.setUnletteringReason("Contre-passation de la piece d'origine");
                                letteringRepository.save(existing);
                            });
                }
            }
            AccEntryLine reversal = byNumber.get(original.getLineNo());
            BigDecimal value = amount(original);
            AccLettering lettering = letteringRepository.save(AccLettering.builder()
                    .code(nextCode(account.getNumber(), original.getThirdPartyCode()))
                    .accountNumber(account.getNumber())
                    .thirdPartyCode(original.getThirdPartyCode())
                    .kind(LetteringKind.AUTO_REFERENCE)
                    .status(LetteringStatus.COMPLETE)
                    .totalDebit(value)
                    .totalCredit(value)
                    .letteredBy(actor)
                    .build());
            letteringLineRepository.save(AccLetteringLine.builder().letteringId(lettering.getId()).entryLineId(original.getId()).matchedAmount(value).build());
            letteringLineRepository.save(AccLetteringLine.builder().letteringId(lettering.getId()).entryLineId(reversal.getId()).matchedAmount(value).build());
        }
    }

    /** Code du lettrage actif d'une ligne (le plus recent). */
    @Transactional(readOnly = true)
    public Optional<String> activeCode(Long lineId) {
        return letteringLineRepository.findByEntryLineId(lineId).stream()
                .map(link -> letteringRepository.findById(link.getLetteringId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(lettering -> lettering.getStatus() != LetteringStatus.UNLETTERED)
                .map(AccLettering::getCode)
                .reduce((first, second) -> second);
    }

    @Transactional(readOnly = true)
    public List<AccountingResponses.Lettering> letterings(String accountNumber, String thirdPartyCode) {
        return letteringRepository.findByAccountNumberAndThirdPartyCodeOrderByCreatedAtAsc(accountNumber,
                        StringUtils.hasText(thirdPartyCode) ? thirdPartyCode.trim() : null).stream()
                .map(this::toResponse).toList();
    }

    /** Codes AA, AB... AZ, BA... par compte et par tiers. */
    String nextCode(String accountNumber, String thirdParty) {
        long count = letteringRepository.findByAccountNumberAndThirdPartyCodeOrderByCreatedAtAsc(accountNumber, thirdParty).size();
        StringBuilder code = new StringBuilder();
        long value = count;
        do {
            code.insert(0, (char) ('A' + (value % 26)));
            value = value / 26;
        } while (value > 0);
        while (code.length() < 2) {
            code.insert(0, 'A');
        }
        return code.toString();
    }

    private Map<Long, BigDecimal> matched(Collection<Long> lineIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (lineIds.isEmpty()) {
            return result;
        }
        letteringLineRepository.matchedByLine(lineIds).forEach(row -> result.put((Long) row[0], (BigDecimal) row[1]));
        return result;
    }

    private Map<Long, AccEntry> parents(List<AccEntryLine> lines) {
        Map<Long, AccEntry> parents = new HashMap<>();
        entryRepository.findAllById(lines.stream().map(AccEntryLine::getEntryId).distinct().toList())
                .forEach(entry -> parents.put(entry.getId(), entry));
        return parents;
    }

    private AccountingResponses.Lettering toResponse(AccLettering lettering) {
        return new AccountingResponses.Lettering(lettering.getId(), lettering.getCode(), lettering.getAccountNumber(), lettering.getThirdPartyCode(),
                lettering.getKind(), lettering.getStatus(), lettering.getTotalDebit(), lettering.getTotalCredit(), lettering.getDifference(),
                lettering.getDifferenceEntryNumber(), lettering.getLetteredBy(), lettering.getCreatedAt(), lettering.getUnletteredBy(),
                lettering.getUnletteredAt(), lettering.getUnletteringReason(),
                letteringLineRepository.findByLetteringId(lettering.getId()).stream().map(AccLetteringLine::getEntryLineId).toList());
    }

    private static BigDecimal amount(AccEntryLine line) {
        return line.getDebit().signum() > 0 ? line.getDebit() : line.getCredit();
    }

    private static String plain(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }
}

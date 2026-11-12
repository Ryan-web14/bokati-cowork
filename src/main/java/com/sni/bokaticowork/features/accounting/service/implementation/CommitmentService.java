package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.model.AccCommitment;
import com.sni.bokaticowork.features.accounting.model.AccCommitmentType;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.repository.AccCommitmentRepository;
import com.sni.bokaticowork.features.accounting.repository.AccCommitmentTypeRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Registre des engagements hors bilan (lot F17), tenu en partie double en classe 90 au journal
 * {@code HB} : engagement donne D 901 / C 902, engagement recu D 906 / C 905 ; chaque variation,
 * levee ou expiration ecrit la difference. Baux et credit-bail : engagement = loyers restant a
 * courir, recalcule chaque jour. Rapprochement du registre et des comptes 90, alertes, note annexe.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CommitmentService {

    private final AccCommitmentRepository commitments;
    private final AccCommitmentTypeRepository types;
    private final LoanService loans;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;
    private final CashActorResolver actors;
    private final DataSource dataSource;
    private final jakarta.persistence.EntityManager entityManager;

    public record CommitmentRequest(String code, String typeCode, String direction, String thirdPartyCode, String thirdPartyName, String description,
                                    BigDecimal amount, BigDecimal monthlyAmount, String currency, LocalDate startDate, LocalDate endDate,
                                    LocalDate renewalDate, LocalDate noticeDate, String linkedType, String linkedCode, String documentRefs) {
    }

    @Transactional(readOnly = true)
    public List<AccCommitmentType> types() {
        return types.findAllByOrderByCodeAsc();
    }

    public AccCommitmentType saveType(AccCommitmentType type) {
        require();
        if (!StringUtils.hasText(type.getCode()) || !StringUtils.hasText(type.getLabel())) {
            throw new BadRequestException("Code et libelle du type obligatoires");
        }
        type.setCode(type.getCode().trim().toUpperCase(Locale.ROOT));
        type.setDefaultDirection("RECEIVED".equalsIgnoreCase(type.getDefaultDirection()) ? "RECEIVED" : "GIVEN");
        return types.save(type);
    }

    @Transactional(readOnly = true)
    public List<AccCommitment> list() {
        return commitments.findAllByOrderByCodeAsc();
    }

    /** Inscription ou modification : l'ecriture HB porte la difference avec le montant deja inscrit. */
    public AccCommitment save(CommitmentRequest request) {
        require();
        AccCommitmentType type = types.findById(request.typeCode() == null ? "" : request.typeCode().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("Type d'engagement inconnu : " + request.typeCode()));
        String code = StringUtils.hasText(request.code()) ? request.code().trim().toUpperCase(Locale.ROOT) : "HB-" + (commitments.count() + 1);
        AccCommitment commitment = commitments.findByCode(code).orElseGet(() -> AccCommitment.builder().code(code).createdBy(actors.resolve("SYSTEM")).build());
        if (!"ACTIVE".equals(commitment.getStatus())) {
            throw new BadRequestException("L'engagement " + code + " est leve ou expire");
        }
        if (!StringUtils.hasText(request.thirdPartyName()) || request.startDate() == null) {
            throw new BadRequestException("Tiers et date de debut obligatoires");
        }
        commitment.setTypeCode(type.getCode());
        commitment.setDirection(StringUtils.hasText(request.direction()) ? ("RECEIVED".equalsIgnoreCase(request.direction()) ? "RECEIVED" : "GIVEN")
                : type.getDefaultDirection());
        commitment.setThirdPartyCode(request.thirdPartyCode());
        commitment.setThirdPartyName(request.thirdPartyName());
        commitment.setDescription(request.description());
        commitment.setMonthlyAmount(request.monthlyAmount());
        commitment.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "XAF");
        commitment.setStartDate(request.startDate());
        commitment.setEndDate(request.endDate());
        commitment.setRenewalDate(request.renewalDate());
        commitment.setNoticeDate(request.noticeDate());
        commitment.setLinkedType(request.linkedType());
        commitment.setLinkedCode(request.linkedCode());
        commitment.setDocumentRefs(request.documentRefs());
        BigDecimal amount = lease(commitment) ? remainingRent(commitment, LocalDate.now()) : LoanService.money(request.amount());
        if (amount.signum() < 0) {
            throw new BadRequestException("Le montant est positif");
        }
        commitment.setAmount(amount);
        commitments.save(commitment);
        book(commitment, amount, LocalDate.now(), commitment.getBookedAmount().signum() == 0 ? "Inscription" : "Variation");
        return commitment;
    }

    /** Levee (avec date et piece) : contre-passation du solde inscrit. */
    public AccCommitment release(String code, LocalDate date, String reference) {
        require();
        AccCommitment commitment = active(code);
        LocalDate day = date == null ? LocalDate.now() : date;
        book(commitment, BigDecimal.ZERO, day, "Levee");
        commitment.setStatus("RELEASED");
        commitment.setReleasedOn(day);
        commitment.setReleaseReference(reference);
        return commitments.save(commitment);
    }

    /**
     * Tenue quotidienne : recalcul des loyers restant dus des baux, expiration des engagements
     * arrives a echeance, alertes (echeance, preavis, renouvellement).
     */
    public Map<String, Integer> refresh(LocalDate today) {
        int updated = 0;
        int expired = 0;
        int alerts = 0;
        int alertDays = (int) Math.max(1, number(settings.text("accounting.commitment.alert-days", "30"), 30));
        for (AccCommitment commitment : commitments.findAllByStatus("ACTIVE")) {
            if (commitment.getEndDate() != null && commitment.getEndDate().isBefore(today)) {
                book(commitment, BigDecimal.ZERO, today, "Expiration");
                commitment.setStatus("EXPIRED");
                commitment.setReleasedOn(today);
                commitments.save(commitment);
                expired++;
                continue;
            }
            if (lease(commitment)) {
                BigDecimal amount = remainingRent(commitment, today);
                if (amount.compareTo(commitment.getBookedAmount()) != 0) {
                    commitment.setAmount(amount);
                    commitments.save(commitment);
                    book(commitment, amount, today, "Variation mensuelle des loyers restant dus");
                    updated++;
                }
            }
            alerts += alert(commitment, commitment.getEndDate(), "arrive a echeance", today, alertDays);
            alerts += alert(commitment, commitment.getNoticeDate(), "atteint sa date de preavis de resiliation", today, alertDays);
            alerts += alert(commitment, commitment.getRenewalDate(), "doit etre renouvele ou non", today, alertDays);
        }
        Reconciliation reconciliation = reconciliation();
        if (!reconciliation.balanced()) {
            notifier.notifyAccountants("Hors bilan : registre et comptes 90 en ecart", "Le registre des engagements et les comptes de la classe 90 ne "
                    + "concordent pas (engagements donnes : ecart " + LoanService.plain(reconciliation.givenGap()) + ", recus : ecart "
                    + LoanService.plain(reconciliation.receivedGap()) + ").", null, "/accounting/commitments", null);
        }
        return Map.of("updated", updated, "expired", expired, "alerts", alerts);
    }

    private int alert(AccCommitment commitment, LocalDate date, String what, LocalDate today, int alertDays) {
        if (date == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(today, date);
        if (days != alertDays && days != 7 && days != 1) {
            return 0;
        }
        notifier.notifyAccountants("Engagement " + commitment.getCode() + " : " + what + " dans " + days + " jours", "L'engagement " + commitment.getCode()
                + " (" + commitment.getTypeCode() + ", " + commitment.getThirdPartyName() + ") " + what + " le " + date + ".", commitment.getCode(),
                "/accounting/commitments/" + commitment.getCode(), null);
        return 1;
    }

    public record Reconciliation(BigDecimal givenRegister, BigDecimal givenAccount, BigDecimal givenCounterAccount, BigDecimal givenGap,
                                 BigDecimal receivedRegister, BigDecimal receivedAccount, BigDecimal receivedCounterAccount, BigDecimal receivedGap,
                                 boolean balanced) {
    }

    /** Rapprochement du registre (montants inscrits des engagements actifs) et des soldes des comptes 90. */
    @Transactional(readOnly = true)
    public Reconciliation reconciliation() {
        BigDecimal given = commitments.findAllByStatus("ACTIVE").stream().filter(item -> "GIVEN".equals(item.getDirection()))
                .map(AccCommitment::getBookedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = commitments.findAllByStatus("ACTIVE").stream().filter(item -> "RECEIVED".equals(item.getDirection()))
                .map(AccCommitment::getBookedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal givenAccount = balance(account("given"));
        BigDecimal givenCounter = balance(account("given-counter")).negate();
        BigDecimal receivedAccount = balance(account("received")).negate();
        BigDecimal receivedCounter = balance(account("received-counter"));
        BigDecimal givenGap = givenAccount.subtract(given).abs().max(givenCounter.subtract(given).abs());
        BigDecimal receivedGap = receivedAccount.subtract(received).abs().max(receivedCounter.subtract(received).abs());
        return new Reconciliation(given, givenAccount, givenCounter, givenGap, received, receivedAccount, receivedCounter, receivedGap,
                givenGap.signum() == 0 && receivedGap.signum() == 0);
    }

    public record NoteLine(String direction, String typeCode, String label, int count, BigDecimal total, BigDecimal withinOneYear,
                           BigDecimal oneToFiveYears, BigDecimal beyondFiveYears) {
    }

    /** Note annexe des engagements hors bilan a une date, donnes et recus, par type et par echeance. */
    @Transactional(readOnly = true)
    public List<NoteLine> note(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        Map<String, BigDecimal[]> buckets = new TreeMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (AccCommitment commitment : commitments.findAllByOrderByCodeAsc()) {
            boolean live = "ACTIVE".equals(commitment.getStatus()) ? !commitment.getStartDate().isAfter(day)
                    : commitment.getReleasedOn() != null && commitment.getReleasedOn().isAfter(day) && !commitment.getStartDate().isAfter(day);
            if (!live) {
                continue;
            }
            String key = commitment.getDirection() + "|" + commitment.getTypeCode();
            BigDecimal[] values = buckets.computeIfAbsent(key, ignored -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            counts.merge(key, 1, Integer::sum);
            if (lease(commitment)) {
                BigDecimal within = commitment.getMonthlyAmount().multiply(BigDecimal.valueOf(months(day, min(commitment.getEndDate(), day.plusYears(1)))));
                BigDecimal oneToFive = commitment.getMonthlyAmount().multiply(BigDecimal.valueOf(
                        months(day, min(commitment.getEndDate(), day.plusYears(5))) - months(day, min(commitment.getEndDate(), day.plusYears(1)))));
                BigDecimal total = remainingRent(commitment, day);
                add(values, total, within, oneToFive, total.subtract(within).subtract(oneToFive));
            } else {
                BigDecimal amount = commitment.getAmount();
                LocalDate end = commitment.getEndDate();
                if (end != null && !end.isAfter(day.plusYears(1))) {
                    add(values, amount, amount, BigDecimal.ZERO, BigDecimal.ZERO);
                } else if (end != null && !end.isAfter(day.plusYears(5))) {
                    add(values, amount, BigDecimal.ZERO, amount, BigDecimal.ZERO);
                } else {
                    add(values, amount, BigDecimal.ZERO, BigDecimal.ZERO, amount);
                }
            }
        }
        List<NoteLine> result = new ArrayList<>();
        buckets.forEach((key, values) -> {
            String[] parts = key.split("\\|");
            String label = types.findById(parts[1]).map(AccCommitmentType::getLabel).orElse(parts[1]);
            result.add(new NoteLine(parts[0], parts[1], label, counts.get(key), values[0], values[1], values[2], values[3]));
        });
        return result;
    }

    // ------------------------------------------------------------------ outils

    /** Ecrit la difference entre le montant voulu et le montant inscrit. */
    private void book(AccCommitment commitment, BigDecimal target, LocalDate date, String why) {
        BigDecimal difference = target.subtract(commitment.getBookedAmount());
        if (difference.signum() == 0) {
            return;
        }
        boolean given = "GIVEN".equals(commitment.getDirection());
        String debit = given ? account("given") : account("received-counter");
        String credit = given ? account("given-counter") : account("received");
        if (difference.signum() < 0) {
            String swap = debit;
            debit = credit;
            credit = swap;
        }
        BigDecimal value = difference.abs();
        String label = why + " engagement " + commitment.getCode() + " " + commitment.getThirdPartyName();
        AccEntry entry = loans.post(settings.text("accounting.commitment.journal", "HB"), date, label, "COMMITMENT", commitment.getCode(), List.of(
                loans.line(debit, null, label, value, null, commitment.getCode(), null),
                loans.line(credit, null, label, null, value, commitment.getCode(), null)));
        commitment.setBookedAmount(target);
        commitment.setLastEntryNumber(entry.getEntryNumber());
        commitments.save(commitment);
    }

    private String account(String kind) {
        return switch (kind) {
            case "given" -> settings.text("accounting.commitment.given-account", "901");
            case "given-counter" -> settings.text("accounting.commitment.given-counter-account", "902");
            case "received" -> settings.text("accounting.commitment.received-account", "905");
            default -> settings.text("accounting.commitment.received-counter-account", "906");
        };
    }

    private boolean lease(AccCommitment commitment) {
        return commitment.getMonthlyAmount() != null && commitment.getMonthlyAmount().signum() > 0 && commitment.getEndDate() != null
                && types.findById(commitment.getTypeCode()).map(type -> Boolean.TRUE.equals(type.getLease())).orElse(false);
    }

    /** Loyers restant a courir : loyer mensuel x mois entamés jusqu'a la fin du bail. */
    static BigDecimal remainingRent(AccCommitment commitment, LocalDate date) {
        LocalDate from = date.isBefore(commitment.getStartDate()) ? commitment.getStartDate() : date;
        return LoanService.money(commitment.getMonthlyAmount().multiply(BigDecimal.valueOf(months(from, commitment.getEndDate()))));
    }

    static long months(LocalDate from, LocalDate to) {
        if (to == null || !to.isAfter(from)) {
            return 0;
        }
        long months = ChronoUnit.MONTHS.between(from, to);
        return from.plusMonths(months).isBefore(to) ? months + 1 : months;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a == null ? b : (a.isBefore(b) ? a : b);
    }

    private static void add(BigDecimal[] values, BigDecimal total, BigDecimal within, BigDecimal oneToFive, BigDecimal beyond) {
        values[0] = values[0].add(total);
        values[1] = values[1].add(within);
        values[2] = values[2].add(oneToFive);
        values[3] = values[3].add(beyond);
    }

    private BigDecimal balance(String account) {
        // Les ecritures de la transaction en cours doivent etre visibles de la requete JDBC.
        entityManager.flush();
        BigDecimal value = new JdbcTemplate(dataSource).queryForObject("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM acc_entry_line l "
                + "JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number = ? AND e.status IN ('VALIDATED', 'REVERSED')", BigDecimal.class, account);
        return value == null ? BigDecimal.ZERO : value;
    }

    private AccCommitment active(String code) {
        AccCommitment commitment = commitments.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Engagement introuvable : " + code));
        if (!"ACTIVE".equals(commitment.getStatus())) {
            throw new BadRequestException("L'engagement " + code + " n'est plus actif");
        }
        return commitment;
    }

    private void require() {
        if (!actors.hasAuthority(LoanService.MANAGE)) {
            throw new ForbiddenException("Action reservee : " + LoanService.MANAGE);
        }
    }

    private static long number(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}

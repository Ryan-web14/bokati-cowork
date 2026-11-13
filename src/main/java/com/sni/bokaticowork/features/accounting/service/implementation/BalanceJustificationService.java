package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.enums.AccountSide;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccConfirmationRequest;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.model.BalanceJustification;
import com.sni.bokaticowork.features.accounting.repository.AccAccountRepository;
import com.sni.bokaticowork.features.accounting.repository.AccConfirmationRequestRepository;
import com.sni.bokaticowork.features.accounting.repository.BalanceJustificationRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Justification des soldes (plan des comptes de tiers, section 4 ; lot F12) : justificatif du solde
 * d'un tiers par ses pieces ouvertes, justification des comptes collectifs et soldes anormaux,
 * dossier de justification des soldes de cloture, confirmations de solde (circularisation).
 *
 * <p>Pieces ouvertes a une date : lignes datees au plus tard ce jour, moins ce que lettrent les
 * lettrages actifs dont toutes les lignes sont datees au plus tard ce jour (une facture reglee en
 * decembre et lettree en janvier n'est pas ouverte au 31 decembre).</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BalanceJustificationService {

    public static final String PREPARE = "BALANCE_JUSTIFICATION:PREPARE";
    public static final String REVIEW = "BALANCE_JUSTIFICATION:REVIEW";
    private static final String COLLECTIVES = "411,401,421,4191,422,462,165";

    private final JdbcTemplate jdbc;
    private final FinancialStatementService statements;
    private final AccAccountRepository accounts;
    private final BalanceJustificationRepository justifications;
    private final AccConfirmationRequestRepository confirmations;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<DeferralService> deferrals;
    private final ObjectProvider<FixedAssetReportService> fixedAssets;
    private final ObjectProvider<com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender> mailer;
    private final ObjectProvider<com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageSender> whatsappSender;
    private final ObjectProvider<com.sni.bokaticowork.core.identity.OperatorIdentityService> operator;

    // ================================================================== justificatif d'un tiers

    /** Piece ouverte : montant signe (positif au debit), jours de retard sur l'echeance. */
    public record OpenItem(Long lineId, String entryNumber, LocalDate date, String reference, String label, LocalDate dueDate, BigDecimal amount,
                           Long daysOverdue) {
    }

    public record Justification(String accountNumber, String thirdPartyCode, LocalDate date, List<OpenItem> items, BigDecimal totalOpen,
                                BigDecimal bookBalance, BigDecimal difference) {
    }

    /** Justificatif du solde d'un compte (prefixe) pour un tiers, ou pour tous ses tiers. */
    @Transactional(readOnly = true)
    public Justification justification(String account, String thirdParty, LocalDate date) {
        statements.require(FinancialStatementService.READ, PREPARE, "ACCOUNTING_REPORT:READ");
        return compute(account, thirdParty, date == null ? LocalDate.now() : date);
    }

    Justification compute(String account, String thirdParty, LocalDate date) {
        if (!StringUtils.hasText(account)) {
            throw new BadRequestException("Indiquez le compte");
        }
        String prefix = account.trim();
        String party = StringUtils.hasText(thirdParty) ? thirdParty.trim() : null;
        LocalDate scope = statements.scopeStart(date);
        Map<Long, BigDecimal> matched = new HashMap<>();
        jdbc.query("SELECT ll.entry_line_id, SUM(ll.matched_amount) FROM acc_lettering_line ll JOIN acc_lettering lt ON lt.id = ll.lettering_id "
                        + "JOIN acc_entry_line el ON el.id = ll.entry_line_id JOIN acc_entry e ON e.id = el.entry_id "
                        + "WHERE el.account_number LIKE ? AND (CAST(? AS VARCHAR) IS NULL OR el.third_party_code = ?) AND e.entry_date BETWEEN ? AND ? "
                        + "AND lt.status <> 'UNLETTERED' "
                        + "AND NOT EXISTS (SELECT 1 FROM acc_lettering_line o JOIN acc_entry_line ol ON ol.id = o.entry_line_id "
                        + "JOIN acc_entry oe ON oe.id = ol.entry_id WHERE o.lettering_id = lt.id AND oe.entry_date > ?) GROUP BY ll.entry_line_id",
                rs -> {
                    matched.put(rs.getLong(1), rs.getBigDecimal(2));
                }, prefix + "%", party, party, scope, date, date);
        List<OpenItem> items = new ArrayList<>();
        BigDecimal[] book = {BigDecimal.ZERO};
        jdbc.query("SELECT l.id, e.entry_number, e.entry_date, l.reference, l.label, l.due_date, l.debit, l.credit FROM acc_entry_line l "
                        + "JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number LIKE ? AND (CAST(? AS VARCHAR) IS NULL OR l.third_party_code = ?) "
                        + "AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? ORDER BY e.entry_date, e.id, l.line_no",
                rs -> {
                    BigDecimal debit = rs.getBigDecimal(7);
                    BigDecimal credit = rs.getBigDecimal(8);
                    BigDecimal signed = debit.subtract(credit);
                    book[0] = book[0].add(signed);
                    BigDecimal rest = signed.abs().subtract(matched.getOrDefault(rs.getLong(1), BigDecimal.ZERO));
                    if (rest.signum() <= 0) {
                        return;
                    }
                    LocalDate due = rs.getDate(6) == null ? null : rs.getDate(6).toLocalDate();
                    Long overdue = due == null || !due.isBefore(date) ? null : ChronoUnit.DAYS.between(due, date);
                    items.add(new OpenItem(rs.getLong(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getString(4), rs.getString(5), due,
                            signed.signum() >= 0 ? rest : rest.negate(), overdue));
                }, prefix + "%", party, party, scope, date);
        BigDecimal open = items.stream().map(OpenItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Justification(prefix, party, date, items, open, book[0], book[0].subtract(open));
    }

    public record SavedJustification(Long id, String accountNumber, String thirdPartyCode, LocalDate date, BigDecimal totalOpen, BigDecimal bookBalance,
                                     BigDecimal difference, String comment, String preparedBy, Instant preparedAt, String reviewedBy, Instant reviewedAt) {
    }

    /** Fige un justificatif, signe par son preparateur. */
    public SavedJustification prepare(String account, String thirdParty, LocalDate date, String comment) {
        statements.require(PREPARE);
        Justification computed = compute(account, thirdParty, date == null ? LocalDate.now() : date);
        String items;
        try {
            items = objectMapper.writeValueAsString(computed.items());
        } catch (Exception exception) {
            throw new BadRequestException("Justificatif illisible : " + exception.getMessage());
        }
        return saved(justifications.save(BalanceJustification.builder().accountNumber(computed.accountNumber()).thirdPartyCode(computed.thirdPartyCode())
                .justifiedAt(computed.date()).openItems(items).totalOpen(computed.totalOpen()).bookBalance(computed.bookBalance())
                .difference(computed.difference()).comment(StringUtils.hasText(comment) ? comment.trim() : null)
                .preparedBy(actors.resolve("SYSTEM")).preparedAt(Instant.now()).build()));
    }

    /** Revue d'un justificatif, par une autre personne que son preparateur. */
    public SavedJustification review(Long id) {
        statements.require(REVIEW);
        BalanceJustification justification = justifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificatif introuvable : " + id));
        String actor = actors.resolve("SYSTEM");
        if (actor.equalsIgnoreCase(justification.getPreparedBy())) {
            throw new BadRequestException("La revue revient a une autre personne que celle qui a prepare le justificatif");
        }
        justification.setReviewedBy(actor);
        justification.setReviewedAt(Instant.now());
        return saved(justifications.save(justification));
    }

    private static SavedJustification saved(BalanceJustification item) {
        return new SavedJustification(item.getId(), item.getAccountNumber(), item.getThirdPartyCode(), item.getJustifiedAt(), item.getTotalOpen(),
                item.getBookBalance(), item.getDifference(), item.getComment(), item.getPreparedBy(), item.getPreparedAt(), item.getReviewedBy(),
                item.getReviewedAt());
    }

    // ================================================================== comptes collectifs

    public record PartyBalance(String thirdPartyCode, BigDecimal balance, boolean abnormal, BigDecimal justificationGap) {
    }

    public record Collective(String prefix, LocalDate date, BigDecimal balance, BigDecimal partiesTotal, BigDecimal withoutThirdParty,
                             List<PartyBalance> parties, List<PartyBalance> abnormal, boolean ok) {
    }

    /**
     * Justification d'un compte collectif : soldes par tiers, dont la somme egale le collectif ; les
     * lignes sans tiers et les ecarts de justification par tiers sont des anomalies ; les soldes
     * anormaux (client crediteur, fournisseur debiteur) sont listes a part.
     */
    @Transactional(readOnly = true)
    public Collective collective(String prefix, LocalDate date) {
        statements.require(FinancialStatementService.READ, PREPARE, "ACCOUNTING_REPORT:READ");
        return collectiveAt(prefix, date == null ? LocalDate.now() : date, true);
    }

    Collective collectiveAt(String prefix, LocalDate date, boolean withGaps) {
        String value = prefix.trim();
        LocalDate scope = statements.scopeStart(date);
        Map<String, BigDecimal> parties = new TreeMap<>();
        jdbc.query("SELECT COALESCE(l.third_party_code, ''), COALESCE(SUM(l.debit), 0) - COALESCE(SUM(l.credit), 0) FROM acc_entry_line l "
                        + "JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number LIKE ? AND e.status IN ('VALIDATED', 'REVERSED') "
                        + "AND e.entry_date BETWEEN ? AND ? GROUP BY COALESCE(l.third_party_code, '')",
                rs -> {
                    parties.put(rs.getString(1), rs.getBigDecimal(2));
                }, value + "%", scope, date);
        AccountSide normal = accounts.findByNumber(value).map(AccAccount::getNormalSide).orElse(AccountSide.DEBIT);
        Optional<AccAccount> definition = accounts.findByNumber(value);
        boolean lettrable = definition.map(item -> Boolean.TRUE.equals(item.getReconcilable())).orElse(false);
        BigDecimal without = parties.getOrDefault("", BigDecimal.ZERO);
        List<PartyBalance> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : parties.entrySet()) {
            if (entry.getKey().isEmpty() || entry.getValue().signum() == 0) {
                continue;
            }
            boolean abnormal = normal == AccountSide.DEBIT ? entry.getValue().signum() < 0 : entry.getValue().signum() > 0;
            BigDecimal gap = withGaps && lettrable ? compute(value, entry.getKey(), date).difference() : BigDecimal.ZERO;
            list.add(new PartyBalance(entry.getKey(), entry.getValue(), abnormal, gap));
        }
        BigDecimal total = list.stream().map(PartyBalance::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = parties.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean ok = without.signum() == 0 && list.stream().allMatch(item -> item.justificationGap().signum() == 0);
        return new Collective(value, date, balance, total, without, list, list.stream().filter(PartyBalance::abnormal).toList(), ok);
    }

    /** Pour la liste de cloture : chaque collectif se justifie par ses tiers. */
    List<String> collectiveProblems(LocalDate date) {
        List<String> problems = new ArrayList<>();
        for (String prefix : AccountingPeriodService.split(settings.text("accounting.closing.collective-accounts", COLLECTIVES))) {
            Collective collective = collectiveAt(prefix, date, true);
            if (collective.withoutThirdParty().signum() != 0) {
                problems.add("Collectif " + prefix + " : " + plain(collective.withoutThirdParty()) + " sans tiers");
            }
            collective.parties().stream().filter(item -> item.justificationGap().signum() != 0)
                    .forEach(item -> problems.add("Collectif " + prefix + ", tiers " + item.thirdPartyCode() + " : pieces ouvertes differentes du solde de "
                            + plain(item.justificationGap())));
        }
        return problems;
    }

    // ================================================================== dossier de justification

    public record FileItem(String accountNumber, String label, BigDecimal balance, String kind, String status, String detail, String preparedBy,
                           String reviewedBy) {
    }

    /** Dossier de justification des soldes de cloture : chaque compte de bilan, son justificatif et son etat. */
    @Transactional(readOnly = true)
    public List<FileItem> justificationFile(String yearCode) {
        statements.require(FinancialStatementService.READ, PREPARE);
        AccFiscalYear year = statements.year(yearCode);
        LocalDate date = year.getEndDate();
        Map<String, AccAccount> chart = accounts.findAllByOrderByNumberAsc().stream().collect(Collectors.toMap(AccAccount::getNumber, item -> item, (a, b) -> a));
        List<String> collectives = AccountingPeriodService.split(settings.text("accounting.closing.collective-accounts", COLLECTIVES));
        List<String> suspense = AccountingPeriodService.split(settings.text("accounting.closing.suspense-accounts", "471,585"));
        Map<String, Collective> collectiveCache = new HashMap<>();
        List<FileItem> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : statements.balances(statements.scopeStart(date), date).entrySet()) {
            String number = entry.getKey();
            BigDecimal balance = entry.getValue();
            if (!FinancialStatementService.isBalanceSheet(number) || balance.signum() == 0) {
                continue;
            }
            String kind;
            String status;
            String detail;
            Optional<String> collective = collectives.stream().filter(number::startsWith).max(Comparator.comparingInt(String::length));
            if (suspense.stream().anyMatch(number::startsWith)) {
                kind = "SUSPENSE";
                status = "GAP";
                detail = "Compte d'attente non solde : a solder ou a expliquer";
            } else if (collective.isPresent()) {
                kind = "THIRD_PARTIES";
                Collective result = collectiveCache.computeIfAbsent(number, key -> collectiveAt(key, date, true));
                status = result.ok() ? "JUSTIFIED" : "GAP";
                detail = result.parties().size() + " tiers, " + result.abnormal().size() + " solde(s) anormal(aux)"
                        + (result.ok() ? "" : ", ecart de justification");
            } else if (number.startsWith("52") || number.startsWith("53") || number.startsWith("55")) {
                kind = "BANK_RECONCILIATION";
                Boolean tracked = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM bank_account WHERE account_number = ?)", Boolean.class, number);
                Boolean reconciled = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM bank_reconciliation r JOIN bank_account a ON a.code = r.bank_account_code "
                        + "WHERE a.account_number = ? AND r.status = 'VALIDATED' AND r.reconciliation_date >= ?)", Boolean.class, number, date);
                status = Boolean.TRUE.equals(reconciled) ? "JUSTIFIED" : Boolean.TRUE.equals(tracked) ? "GAP" : "TO_REVIEW";
                detail = Boolean.TRUE.equals(reconciled) ? "Etat de rapprochement valide" : Boolean.TRUE.equals(tracked)
                        ? "Rapprochement non valide au " + date : "Compte non suivi en banque : releve a joindre";
            } else if (number.startsWith("57")) {
                kind = "CASH_COUNT";
                status = "TO_REVIEW";
                detail = "Proces-verbal de billetage au " + date + " a joindre";
            } else if (number.startsWith("513")) {
                kind = "CHEQUES";
                BigDecimal cheques = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM bank_cheque WHERE status IN ('RECEIVED', 'DEPOSITED')", BigDecimal.class);
                status = cheques != null && cheques.compareTo(balance) == 0 ? "JUSTIFIED" : "TO_REVIEW";
                detail = "Cheques en portefeuille : " + plain(cheques);
            } else if (number.startsWith("476") || number.startsWith("477")) {
                kind = "DEFERRALS";
                DeferralService service = deferrals.getIfAvailable();
                boolean blocking = service != null && service.controls(date).stream().anyMatch(item -> "BLOCKING".equals(item.severity()));
                status = service == null ? "TO_REVIEW" : blocking ? "GAP" : "JUSTIFIED";
                detail = "Detail des echeanciers de constatations d'avance";
            } else if (number.startsWith("2")) {
                kind = "FIXED_ASSETS";
                FixedAssetReportService service = fixedAssets.getIfAvailable();
                boolean blocking = service != null && service.closingCheck(date.getYear()).stream().anyMatch(item -> "BLOCKING".equals(item.severity()));
                status = service == null ? "TO_REVIEW" : blocking ? "GAP" : "JUSTIFIED";
                detail = "Tableau des immobilisations et des amortissements";
            } else if (number.startsWith("16") || number.startsWith("17")) {
                kind = "LOANS";
                status = "TO_REVIEW";
                detail = "Tableau d'amortissement de l'emprunt a joindre";
            } else if (number.startsWith("44")) {
                kind = "TAXES";
                status = "TO_REVIEW";
                detail = "Derniere declaration a joindre";
            } else {
                kind = "OTHER";
                status = "TO_REVIEW";
                detail = "Justificatif a joindre";
            }
            Optional<BalanceJustification> saved = justifications.findAllByAccountNumberAndJustifiedAtOrderByPreparedAtDesc(number, date).stream().findFirst();
            if (saved.isPresent() && "TO_REVIEW".equals(status) && saved.get().getReviewedBy() != null) {
                status = "JUSTIFIED";
                detail = detail + " ; justificatif revu";
            }
            items.add(new FileItem(number, chart.containsKey(number) ? chart.get(number).getLabel() : null, balance, kind, status, detail,
                    saved.map(BalanceJustification::getPreparedBy).orElse(null), saved.map(BalanceJustification::getReviewedBy).orElse(null)));
        }
        return items;
    }

    public FixedAssetReportService.Table fileTable(String yearCode) {
        List<FileItem> items = justificationFile(yearCode);
        List<List<Object>> rows = new ArrayList<>();
        for (FileItem item : items) {
            rows.add(List.of(item.accountNumber(), item.label() == null ? "" : item.label(), item.balance(), item.kind(), item.status(), item.detail(),
                    item.preparedBy() == null ? "" : item.preparedBy(), item.reviewedBy() == null ? "" : item.reviewedBy()));
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("Justifies", items.stream().filter(item -> "JUSTIFIED".equals(item.status())).count());
        totals.put("En ecart", items.stream().filter(item -> "GAP".equals(item.status())).count());
        totals.put("A revoir", items.stream().filter(item -> "TO_REVIEW".equals(item.status())).count());
        return new FixedAssetReportService.Table("justification-" + yearCode, "Dossier de justification des soldes de cloture " + yearCode,
                List.of("Compte", "Libelle", "Solde", "Justificatif", "Etat", "Detail", "Prepare par", "Revu par"),
                List.of(false, false, true, false, false, false, false, false), rows, totals);
    }

    // ================================================================== confirmations de solde

    public record SelectionRequest(String accountNumber, LocalDate date, Integer top, BigDecimal threshold, List<String> thirdParties) {
    }

    public record ReplyRequest(String channel, BigDecimal confirmedBalance, String detail) {
    }

    /**
     * Prepare les confirmations : les tiers designes, ou les N plus gros soldes et tout solde au-dela
     * du seuil (20 et 1 000 000 par defaut). Un tiers deja circularise a cette date n'est pas repris.
     */
    public List<AccConfirmationRequest> select(SelectionRequest request) {
        statements.require(PREPARE);
        if (request == null || !StringUtils.hasText(request.accountNumber())) {
            throw new BadRequestException("Indiquez le compte collectif (411, 401...)");
        }
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        Collective collective = collectiveAt(request.accountNumber(), date, false);
        List<PartyBalance> chosen;
        if (request.thirdParties() != null && !request.thirdParties().isEmpty()) {
            chosen = collective.parties().stream().filter(item -> request.thirdParties().contains(item.thirdPartyCode())).toList();
        } else {
            int top = request.top() != null ? request.top() : Integer.parseInt(settings.text("accounting.confirmation.top-count", "20").trim());
            BigDecimal threshold = request.threshold() != null ? request.threshold()
                    : new BigDecimal(settings.text("accounting.confirmation.threshold", "1000000").trim());
            List<PartyBalance> sorted = collective.parties().stream().sorted(Comparator.comparing((PartyBalance item) -> item.balance().abs()).reversed()).toList();
            chosen = new ArrayList<>(sorted.stream().limit(top).toList());
            sorted.stream().skip(top).filter(item -> item.balance().abs().compareTo(threshold) >= 0).forEach(chosen::add);
        }
        String actor = actors.resolve("SYSTEM");
        List<AccConfirmationRequest> created = new ArrayList<>();
        for (PartyBalance party : chosen) {
            if (confirmations.existsByAccountNumberAndThirdPartyCodeAndBalanceDate(collective.prefix(), party.thirdPartyCode(), date)) {
                continue;
            }
            String items;
            try {
                items = objectMapper.writeValueAsString(compute(collective.prefix(), party.thirdPartyCode(), date).items());
            } catch (Exception exception) {
                items = null;
            }
            created.add(confirmations.save(AccConfirmationRequest.builder()
                    .number("CONF-" + date.getYear() + "-" + String.format("%05d", confirmations.count() + 1))
                    .accountNumber(collective.prefix()).thirdPartyCode(party.thirdPartyCode()).balanceDate(date).balance(party.balance())
                    .openItems(items).status("PREPARED").createdBy(actor).createdAt(Instant.now()).build()));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<AccConfirmationRequest> confirmations() {
        statements.require(FinancialStatementService.READ, PREPARE);
        return confirmations.findAllByOrderByCreatedAtDesc();
    }

    public static final List<String> CHANNELS = List.of("EMAIL", "WHATSAPP", "PRINT");

    /** Resultat d'un envoi : la confirmation suivie, le canal, l'adresse ou le numero. */
    public record SendResult(AccConfirmationRequest confirmation, String channel, String recipient) {
    }

    /**
     * Envoi (ou relance) d'une confirmation, sans aucune ouverture vers l'exterieur : la lettre PDF
     * part par courriel ou par WhatsApp (envoi par l'operateur de messagerie, Infobip), ou la lettre
     * est imprimee. Le tiers repond par retour de courriel, par WhatsApp ou par courrier ; le comptable
     * saisit la reponse.
     *
     * @param channel   {@code EMAIL} (defaut), {@code WHATSAPP} ou {@code PRINT}
     * @param recipient adresse ou numero ; a defaut, ceux du tiers
     */
    public SendResult send(String number, boolean reminder, String channel, String recipient) {
        statements.require(PREPARE);
        AccConfirmationRequest request = confirmation(number);
        String mode = StringUtils.hasText(channel) ? channel.trim().toUpperCase(Locale.ROOT) : "EMAIL";
        if (!CHANNELS.contains(mode)) {
            throw new BadRequestException("Canal EMAIL, WHATSAPP ou PRINT");
        }
        if (List.of("AGREED", "DISPUTED", "RECONCILED").contains(request.getStatus())) {
            throw new BadRequestException("La confirmation " + number + " a deja sa reponse");
        }
        if (reminder && request.getSentAt() == null) {
            throw new BadRequestException("Envoyez la confirmation avant de la relancer");
        }
        String target = StringUtils.hasText(recipient) ? recipient.trim() : null;
        if ("EMAIL".equals(mode)) {
            target = target != null ? target : emailOf(request.getThirdPartyCode())
                    .orElseThrow(() -> new BadRequestException("Aucune adresse de courriel pour " + request.getThirdPartyCode()
                            + " : indiquez-la, ou choisissez WhatsApp ou l'impression"));
            mail(request, target, reminder);
        } else if ("WHATSAPP".equals(mode)) {
            target = target != null ? target : whatsappOf(request.getThirdPartyCode())
                    .orElseThrow(() -> new BadRequestException("Aucun numero WhatsApp pour " + request.getThirdPartyCode()
                            + " : indiquez-le, ou choisissez le courriel ou l'impression"));
            if (target.replaceAll("[^0-9]", "").length() < 8) {
                throw new BadRequestException("Numero WhatsApp invalide : " + target);
            }
            var sender = whatsappSender.getIfAvailable();
            if (sender == null) {
                throw new BadRequestException("L'envoi WhatsApp n'est pas configure (Infobip) : choisissez le courriel ou l'impression");
            }
            // Texte seul : le modele approuve BALANCE_CONFIRMATION, ou le texte libre si le parametre l'admet.
            sender.send(target, "BALANCE_CONFIRMATION", List.of(company(),
                    request.getBalanceDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    String.format(Locale.FRANCE, "%,d", request.getBalance().abs().setScale(0, java.math.RoundingMode.HALF_UP).longValue()).replace('\u202f', ' ').replace('\u00a0', ' '),
                    request.getBalance().signum() >= 0 ? "en notre faveur" : "en votre faveur", request.getNumber()),
                    message(request, reminder), request.getNumber());
        }
        if (reminder) {
            request.setStatus("REMINDED");
            request.setRemindedAt(Instant.now());
        } else {
            request.setStatus("SENT");
            request.setSentAt(Instant.now());
        }
        request.setSentChannel(mode);
        request.setRecipient(target);
        return new SendResult(confirmations.save(request), mode, target);
    }

    /** Adresse du tiers : client (facturation d'abord), membre du client, organisation, fournisseur. */
    Optional<String> emailOf(String thirdParty) {
        return first(thirdParty, List.of(
                "SELECT COALESCE(NULLIF(billing_email, ''), email) FROM customer WHERE customer_id = ?",
                "SELECT m.email FROM member m JOIN customer c ON c.id = m.customer_id WHERE c.customer_id = ?",
                "SELECT email FROM business_entity WHERE entity_code = ?",
                "SELECT email FROM acc_supplier WHERE code = ?",
                "SELECT email FROM inventory_supplier WHERE supplier_code = ?"));
    }

    /** Numero WhatsApp du tiers : client, puis membre du client. */
    Optional<String> whatsappOf(String thirdParty) {
        return first(thirdParty, List.of(
                "SELECT whatsapp_phone FROM customer WHERE customer_id = ?",
                "SELECT m.whatsapp_phone FROM member m JOIN customer c ON c.id = m.customer_id WHERE c.customer_id = ?"));
    }

    private Optional<String> first(String thirdParty, List<String> queries) {
        for (String query : queries) {
            Optional<String> found = jdbc.queryForList(query, String.class, thirdParty).stream().filter(StringUtils::hasText).findFirst();
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private String company() {
        var service = operator.getIfAvailable();
        return service == null ? com.sni.bokaticowork.core.identity.OperatorIdentity.DEFAULT_NAME : service.current().name();
    }

    /** Texte de la demande, commun au courriel et a WhatsApp. */
    private String message(AccConfirmationRequest request, boolean reminder) {
        return (reminder ? "Relance. " : "") + "Confirmation de solde " + request.getNumber() + " : dans nos livres, le solde de votre compte au "
                + request.getBalanceDate() + " est de " + plain(request.getBalance()) + " XAF " + (request.getBalance().signum() >= 0 ? "en notre faveur" : "en votre faveur")
                + ". Merci de nous confirmer ce solde, ou de nous indiquer le votre avec le detail de l'ecart, en rappelant la reference "
                + request.getNumber() + ".";
    }

    private void mail(AccConfirmationRequest request, String recipient, boolean reminder) {
        var sender = mailer.getIfAvailable();
        if (sender == null) {
            throw new BadRequestException("L'envoi de courriels n'est pas disponible");
        }
        String subject = (reminder ? "Relance : " : "") + "confirmation de solde " + request.getNumber();
        String body = "<p>Madame, Monsieur,</p><p>" + message(request, reminder) + "</p><p>Le detail des pieces ouvertes et le coupon-reponse sont joints. "
                + "Vous pouvez repondre a ce courriel.</p><p>Avec nos remerciements.</p>";
        try {
            FixedAssetReportService renderer = fixedAssets.getIfAvailable();
            byte[] pdf = renderer == null ? null : renderer.pdf(letterTable(request));
            if (pdf == null) {
                sender.sendHtmlEmail(recipient, subject, body);
            } else {
                sender.sendHtmlEmailWithPdfAttachment(recipient, subject, body, request.getNumber() + ".pdf", pdf);
            }
        } catch (Exception exception) {
            throw new BadRequestException("Courriel de confirmation non envoye a " + recipient + " : " + exception.getMessage());
        }
    }

    /** Reponse du tiers : conforme si le solde confirme egale le solde envoye, sinon en ecart. */
    public AccConfirmationRequest reply(String number, ReplyRequest reply) {
        statements.require(PREPARE);
        return record(confirmation(number), reply);
    }

    private AccConfirmationRequest record(AccConfirmationRequest request, ReplyRequest reply) {
        if (reply == null || reply.confirmedBalance() == null) {
            throw new BadRequestException("Indiquez le solde confirme par le tiers");
        }
        BigDecimal difference = request.getBalance().subtract(reply.confirmedBalance());
        request.setReplyChannel(StringUtils.hasText(reply.channel()) ? reply.channel().trim().toUpperCase(Locale.ROOT) : "MAIL");
        request.setRepliedBalance(reply.confirmedBalance());
        request.setReplyDetail(StringUtils.hasText(reply.detail()) ? reply.detail().trim() : null);
        request.setRepliedAt(Instant.now());
        request.setDifference(difference);
        request.setStatus(difference.signum() == 0 ? "AGREED" : "DISPUTED");
        return confirmations.save(request);
    }

    /** Un ecart explique et rapproche, ligne a ligne avec l'etat du tiers. */
    public AccConfirmationRequest reconcile(String number, String comment) {
        statements.require(REVIEW);
        AccConfirmationRequest request = confirmation(number);
        if (!"DISPUTED".equals(request.getStatus())) {
            throw new BadRequestException("Seule une confirmation en ecart se rapproche");
        }
        if (!StringUtils.hasText(comment)) {
            throw new BadRequestException("Expliquez l'ecart");
        }
        request.setStatus("RECONCILED");
        request.setReconciledBy(actors.resolve("SYSTEM"));
        request.setReconciledAt(Instant.now());
        request.setReplyDetail((request.getReplyDetail() == null ? "" : request.getReplyDetail() + " ; ") + "Rapprochement : " + comment.trim());
        return confirmations.save(request);
    }

    /** Lettre de confirmation : solde, pieces ouvertes, coupon-reponse. */
    @Transactional(readOnly = true)
    public FixedAssetReportService.Table letter(String number) {
        statements.require(FinancialStatementService.READ, PREPARE);
        return letterTable(confirmation(number));
    }

    private FixedAssetReportService.Table letterTable(AccConfirmationRequest request) {
        Justification justification = compute(request.getAccountNumber(), request.getThirdPartyCode(), request.getBalanceDate());
        List<List<Object>> rows = new ArrayList<>();
        for (OpenItem item : justification.items()) {
            rows.add(List.of(item.date().toString(), item.reference() == null ? item.entryNumber() : item.reference(),
                    item.label() == null ? "" : item.label(), item.dueDate() == null ? "" : item.dueDate().toString(), item.amount()));
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("Solde au " + request.getBalanceDate() + " dans nos livres", request.getBalance());
        totals.put("Coupon-reponse", "Solde confirme [ ]   Solde conteste [ ], montant selon vos livres : ............ ; explication : ............");
        totals.put("Reference a rappeler", request.getNumber());
        return new FixedAssetReportService.Table("confirmation-" + request.getNumber(), "Confirmation de solde " + request.getNumber() + " · "
                + request.getThirdPartyCode(), List.of("Date", "Piece", "Libelle", "Echeance", "Montant"),
                List.of(false, false, false, false, true), rows, totals);
    }

    private AccConfirmationRequest confirmation(String number) {
        return confirmations.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Confirmation introuvable : " + number));
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}

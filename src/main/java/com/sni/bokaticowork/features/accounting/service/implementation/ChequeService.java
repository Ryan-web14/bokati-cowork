package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Cheques et remises en banque (lot F10, C5) : chaque cheque recu (transaction {@code CHEQUE}) est
 * suivi ({@code RECEIVED}, {@code DEPOSITED}, {@code CLEARED}, {@code REJECTED}) ; le bordereau de
 * remise ({@code BRB}) regroupe especes et cheques ; son credit au releve passe D banque C 513
 * (cheques) et C 585 (especes) et se pointe ; un cheque rejete contre-passe son encaissement (la
 * facture redevient due), avec les frais de rejet le cas echeant.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChequeService {

    private final BankChequeRepository chequeRepository;
    private final BankDepositSlipRepository slipRepository;
    private final BankService banks;
    private final BankStatementLineRepository statementLineRepository;
    private final AccEntryLineRepository entryLineRepository;
    private final AccountingEntryService entries;
    private final AccountingSettings settings;
    private final CashSessionRepository sessionRepository;
    private final VoucherService vouchers;
    private final CashActorResolver actors;
    @Lazy
    private final PaymentService paymentService;
    private final DataSource dataSource;

    public record SlipInput(String bankAccountCode, String registerCode, BigDecimal cashAmount, List<String> chequeTransactions) {
    }

    public record Slip(BankDepositSlip slip, List<BankCheque> cheques) {
    }

    /** Reprend les transactions par cheque pas encore suivies. */
    public int sync() {
        int created = 0;
        for (Map<String, Object> row : new JdbcTemplate(dataSource).queryForList("SELECT t.transaction_number, t.provider_reference, t.bank_name, "
                + "t.payer_name, t.amount, t.cash_session_number, COALESCE(t.paid_at, t.created_at) AS received FROM payment_transaction t "
                + "WHERE t.payment_method = 'CHEQUE' AND t.status IN ('SUCCEEDED', 'REFUNDED', 'REVERSED') AND (t.metadata_json IS NULL OR t.metadata_json NOT LIKE '%originalTransactionNumber%') "
                + "AND NOT EXISTS (SELECT 1 FROM bank_cheque c WHERE c.transaction_number = t.transaction_number)")) {
            String session = (String) row.get("cash_session_number");
            String register = session == null ? null
                    : sessionRepository.findBySessionNumber(session).map(CashSession::getCashRegister).map(r -> r.getRegisterCode()).orElse(null);
            Object received = row.get("received");
            chequeRepository.save(BankCheque.builder().transactionNumber((String) row.get("transaction_number"))
                    .chequeNumber((String) row.get("provider_reference")).bankName((String) row.get("bank_name")).drawer((String) row.get("payer_name"))
                    .amount((BigDecimal) row.get("amount")).sessionNumber(session).registerCode(register)
                    .receivedAt(received instanceof java.sql.Timestamp t ? t.toInstant() : Instant.now()).build());
            created++;
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<BankCheque> cheques(String status) {
        return StringUtils.hasText(status) ? chequeRepository.findByStatusOrderByReceivedAtAsc(status.toUpperCase(Locale.ROOT)) : chequeRepository.findAll();
    }

    /** Bordereau de remise : especes et cheques recus d'une caisse. */
    public Slip prepare(SlipInput input) {
        require("BANK_RECONCILIATION:MANAGE");
        BankAccount bank = banks.account(input.bankAccountCode());
        sync();
        List<BankCheque> cheques = new ArrayList<>();
        for (String transaction : input.chequeTransactions() == null ? List.<String>of() : input.chequeTransactions()) {
            BankCheque cheque = chequeRepository.findByTransactionNumber(transaction)
                    .orElseThrow(() -> new ResourceNotFoundException("Cheque introuvable : " + transaction));
            if (!"RECEIVED".equals(cheque.getStatus()) || cheque.getSlipNumber() != null) {
                throw new BadRequestException("Le cheque " + cheque.getChequeNumber() + " est deja " + cheque.getStatus());
            }
            cheques.add(cheque);
        }
        BigDecimal cash = input.cashAmount() == null ? BigDecimal.ZERO : input.cashAmount();
        if (cheques.isEmpty() && cash.signum() <= 0) {
            throw new BadRequestException("Un bordereau remet des especes ou des cheques");
        }
        String actor = actors.resolve("SYSTEM");
        BigDecimal total = cheques.stream().map(BankCheque::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String reference = "SLIP-" + UUID.randomUUID();
        VoucherService.Issued issued = vouchers.issueAndPrint("BRB", "BANK_DEPOSIT_SLIP", reference, total.add(cash), bank.getCode(), actor);
        BankDepositSlip slip = slipRepository.save(BankDepositSlip.builder().number(issued.number()).bankAccountCode(bank.getCode())
                .registerCode(input.registerCode()).cashAmount(cash).chequeTotal(total).createdBy(actor).build());
        cheques.forEach(cheque -> {
            cheque.setSlipNumber(slip.getNumber());
            chequeRepository.save(cheque);
        });
        return new Slip(slip, cheques);
    }

    public Slip deposit(String number) {
        require("BANK_RECONCILIATION:MANAGE");
        BankDepositSlip slip = slip(number);
        if (!"PREPARED".equals(slip.getStatus())) {
            throw new BadRequestException("Le bordereau est deja " + slip.getStatus());
        }
        slip.setStatus("DEPOSITED");
        slip.setDepositedBy(actors.resolve("SYSTEM"));
        slip.setDepositedAt(Instant.now());
        slipRepository.save(slip);
        List<BankCheque> cheques = chequeRepository.findBySlipNumber(number);
        cheques.forEach(cheque -> {
            cheque.setStatus("DEPOSITED");
            cheque.setDepositedAt(Instant.now());
            chequeRepository.save(cheque);
        });
        return new Slip(slip, cheques);
    }

    /**
     * Credit du bordereau au releve : D banque, C 513 (cheques) et C 585 (especes, deja sorties de la
     * caisse par la remise) ; la ligne de releve indiquee est pointee avec l'ecriture.
     */
    public Slip credit(String number, Long statementLineId, LocalDate date) {
        require("BANK_RECONCILIATION:MANAGE");
        BankDepositSlip slip = slip(number);
        if (!"DEPOSITED".equals(slip.getStatus())) {
            throw new BadRequestException("Seul un bordereau depose se credite");
        }
        BankAccount bank = banks.account(slip.getBankAccountCode());
        BigDecimal total = slip.getCashAmount().add(slip.getChequeTotal());
        String actor = actors.resolve("SYSTEM");
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        lines.add(new AccountingRequests.EntryLine(bank.getAccountNumber(), null, "Remise " + number, total, null, null, null, null, null, null, null, number));
        if (slip.getChequeTotal().signum() > 0) {
            lines.add(new AccountingRequests.EntryLine(settings.text("accounting.payment.CHEQUE.account", "513"), null, "Cheques de la remise " + number,
                    null, slip.getChequeTotal(), null, null, null, null, null, null, number));
        }
        if (slip.getCashAmount().signum() > 0) {
            lines.add(new AccountingRequests.EntryLine("585", null, "Especes de la remise " + number, null, slip.getCashAmount(), null, null, null, null,
                    null, null, number));
        }
        LocalDate day = date == null ? LocalDate.now() : date;
        if (statementLineId != null) {
            BankStatementLine line = statementLineRepository.findById(statementLineId).orElseThrow(() -> new ResourceNotFoundException("Ligne de releve introuvable"));
            if (line.getAmount().compareTo(total) != 0) {
                throw new BadRequestException("La ligne du releve (" + BankService.plain(line.getAmount()) + ") differe du bordereau (" + BankService.plain(total) + ")");
            }
            day = line.getOperationDate();
        }
        AccEntry entry = entries.post(new AccountingEntryService.AutoPost(bank.getJournalCode(), day, null, "Credit de la remise " + number,
                EntryOrigin.PLATFORM, "BANK_DEPOSIT_SLIP", number, lines, actor, true, null));
        if (entry.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture de la remise n'a pas pu etre validee : " + entry.getPendingDetail());
        }
        if (statementLineId != null) {
            AccEntryLine bankLine = entryLineRepository.findByEntryIdOrderByLineNoAsc(entry.getId()).stream()
                    .filter(l -> l.getAccountNumber().equals(bank.getAccountNumber())).findFirst().orElseThrow();
            banks.match(bank, List.of(statementLineId), List.of(bankLine.getId()), "AUTO_REFERENCE", actor);
        }
        slip.setStatus("CREDITED");
        slip.setCreditedAt(Instant.now());
        slip.setCreditEntry(entry.getEntryNumber());
        slipRepository.save(slip);
        List<BankCheque> cheques = chequeRepository.findBySlipNumber(number);
        cheques.forEach(cheque -> {
            cheque.setStatus("CLEARED");
            cheque.setClearedAt(Instant.now());
            chequeRepository.save(cheque);
        });
        return new Slip(slip, cheques);
    }

    /**
     * Cheque rejete : l'encaissement est annule par la transaction de paiement (la facture redevient
     * due, l'ecriture inverse suit) ; des frais de rejet eventuels passent D 631 C banque.
     */
    public BankCheque reject(String transactionNumber, String reason, BigDecimal fees, String bankAccountCode) {
        require("BANK_RECONCILIATION:MANAGE");
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez le motif du rejet");
        }
        BankCheque cheque = chequeRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Cheque introuvable : " + transactionNumber));
        if ("REJECTED".equals(cheque.getStatus())) {
            throw new BadRequestException("Ce cheque est deja rejete");
        }
        String actor = actors.resolve("SYSTEM");
        paymentService.reverse(transactionNumber, new RefundPaymentRequest(cheque.getAmount(), "Cheque rejete : " + reason.trim(), actor));
        if (fees != null && fees.signum() > 0 && StringUtils.hasText(bankAccountCode)) {
            BankAccount bank = banks.account(bankAccountCode);
            entries.post(new AccountingEntryService.AutoPost(bank.getJournalCode(), LocalDate.now(), null, "Frais de rejet du cheque " + cheque.getChequeNumber(),
                    EntryOrigin.PLATFORM, "CHEQUE_REJECTION_FEES", transactionNumber, List.of(
                    new AccountingRequests.EntryLine("631", null, "Frais de rejet", fees, null, null, null, null, null, null, null, cheque.getChequeNumber()),
                    new AccountingRequests.EntryLine(bank.getAccountNumber(), null, "Frais de rejet", null, fees, null, null, null, null, null, null,
                            cheque.getChequeNumber())), actor, true, null));
        }
        cheque.setStatus("REJECTED");
        cheque.setRejectedAt(Instant.now());
        cheque.setRejectedReason(reason.trim());
        return chequeRepository.save(cheque);
    }

    @Transactional(readOnly = true)
    public List<BankDepositSlip> slips() {
        return slipRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Bordereau de remise en banque imprimable (modeles de documents, lot D4) : especes et cheques
     * remis, totaux, cases du caissier, du deposant et de la banque. Une reimpression est marquee
     * DUPLICATA, comme les autres pieces.
     */
    public byte[] slipPdf(String number) {
        BankDepositSlip slip = slip(number);
        VoucherService.Issued registered = vouchers.find(number)
                .orElseThrow(() -> new ResourceNotFoundException("Bordereau absent du registre des pièces : " + number));
        BigDecimal total = slip.getCashAmount().add(slip.getChequeTotal());
        VoucherService.Issued issued = vouchers.issueAndPrint("BRB", "BANK_DEPOSIT_SLIP", registered.sourceCode(), total, slip.getBankAccountCode(),
                actors.resolve("SYSTEM"));
        BankAccount bank = banks.account(slip.getBankAccountCode());
        List<BankCheque> cheques = chequeRepository.findBySlipNumber(number);
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Remise").pair("Bordereau", slip.getNumber())
                .pair("Banque", bank.getBankName() == null ? bank.getLabel() : bank.getBankName() + (bank.getBranch() == null ? "" : ", " + bank.getBranch()))
                .pair("Compte", bank.getRib() == null ? bank.getCode() : bank.getRib()).pair("Caisse", slip.getRegisterCode())
                .pair("Préparé par", slip.getCreatedBy()).pair("Préparé le", VoucherService.date(slip.getCreatedAt()))
                .pair("État", switch (slip.getStatus()) {
                    case "PREPARED" -> "Préparé";
                    case "DEPOSITED" -> "Remis à la banque";
                    case "CREDITED" -> "Crédité";
                    default -> slip.getStatus();
                })
                .pair("Remis par", slip.getDepositedBy()).pair("Remis le", VoucherService.date(slip.getDepositedAt())));
        VoucherService.Section table = new VoucherService.Section("Chèques").columns(List.of("N° chèque", "Banque émettrice", "Tireur", "Transaction", "Montant"),
                List.of(false, false, false, false, true));
        cheques.forEach(cheque -> table.row(cheque.getChequeNumber(), cheque.getBankName(), cheque.getDrawer(), cheque.getTransactionNumber(),
                VoucherService.money(cheque.getAmount())));
        if (!cheques.isEmpty()) {
            sections.add(table);
        }
        sections.add(new VoucherService.Section("Montants").pair("Espèces", VoucherService.money(slip.getCashAmount()) + " XAF")
                .pair("Chèques (" + cheques.size() + ")", VoucherService.money(slip.getChequeTotal()) + " XAF")
                .pair("Total remis", VoucherService.money(total) + " XAF"));
        return vouchers.render("Bordereau de remise en banque", issued, sections, total, null, List.of(
                new VoucherService.Signature("Caissier", slip.getCreatedBy(), VoucherService.date(slip.getCreatedAt())),
                new VoucherService.Signature("Déposant", slip.getDepositedBy(), VoucherService.date(slip.getDepositedAt())),
                new VoucherService.Signature("Cachet de la banque", null, null)));
    }

    private BankDepositSlip slip(String number) {
        return slipRepository.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Bordereau introuvable : " + number));
    }

    private void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Cette action demande " + authority);
        }
    }
}

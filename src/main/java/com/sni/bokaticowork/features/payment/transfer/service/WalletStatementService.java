package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Le releve et le recu, en PDF.
 *
 * <p>Le releve reproduit le journal tel qu'il est tenu, avec les soldes d'ouverture et de cloture
 * recalcules depuis les ecritures · pas depuis le compte. Si les deux divergent, c'est le releve qui
 * dit la verite, parce qu'il est la somme de ce qui a ete ecrit. Il porte l'empreinte de la derniere
 * ecriture : un releve imprime devient ainsi une preuve de l'etat de la chaine a sa date.</p>
 */
@Service
@RequiredArgsConstructor
public class WalletStatementService {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WalletLedgerEntryRepository ledgerRepository;
    private final PaymentMapper paymentMapper;
    private final TransactionContextResolver contextResolver;
    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;
    private final OperatorIdentityService operatorIdentity;
    private final BrandLogo brandLogo;

    public record StatementLine(String date, String reference, String nature, String label,
                                String credit, String debit, String balance) {
    }

    /** Modeles de documents (lot D5), facultatif dans les tests. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter templatePrinter;

    @Transactional(readOnly = true)
    public byte[] statement(WalletAccount wallet, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("La période du relevé est invalide");
        }
        Instant start = from.atStartOfDay(APP_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(APP_ZONE).toInstant();

        List<WalletLedgerEntry> entries = ledgerRepository.findBetween(wallet.getId(), start, end);
        BigDecimal opening = ledgerRepository.findBalanceBefore(wallet.getId(), start).orElse(BigDecimal.ZERO);
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal closing = opening;
        String lastHash = null;

        List<StatementLine> lines = new java.util.ArrayList<>();
        for (WalletLedgerEntry entry : entries) {
            boolean movesLedger = entry.getEntryType() != WalletEntryType.HOLD
                    && entry.getEntryType() != WalletEntryType.HOLD_RELEASE;
            boolean credit = entry.getDirection() == WalletEntryDirection.CREDIT;
            if (movesLedger) {
                if (credit) {
                    credits = credits.add(entry.getAmount());
                } else {
                    debits = debits.add(entry.getAmount());
                }
            }
            closing = entry.getBalanceAfter();
            lastHash = entry.getCurrentHash() == null ? lastHash : entry.getCurrentHash();
            lines.add(new StatementLine(
                    DATE_TIME.format(entry.getCreatedAt().atZone(APP_ZONE)),
                    StringUtils.hasText(entry.getTransactionNumber()) ? entry.getTransactionNumber() : entry.getEntryNumber(),
                    nature(entry.getEntryType()),
                    StringUtils.hasText(entry.getReference()) ? entry.getReference() : "",
                    credit ? money(entry.getAmount()) : "",
                    credit ? "" : money(entry.getAmount()),
                    money(entry.getBalanceAfter())));
        }

        Context context = new Context(appLocale);
        context.setVariable("walletNumber", wallet.getWalletNumber());
        context.setVariable("holderName", holderName(wallet));
        context.setVariable("periodLabel", "du " + DATE.format(from) + " au " + DATE.format(to));
        context.setVariable("currency", wallet.getCurrency());
        context.setVariable("issuedAt", DATE_TIME.format(Instant.now().atZone(APP_ZONE)));
        context.setVariable("openingBalance", money(opening));
        context.setVariable("totalCredits", money(credits));
        context.setVariable("totalDebits", money(debits));
        context.setVariable("closingBalance", money(closing));
        context.setVariable("lines", lines);
        context.setVariable("lastHash", lastHash);
        brand(context);
        if (templatePrinter != null) {
            com.sni.bokaticowork.features.document.template.render.DocumentData data =
                    new com.sni.bokaticowork.features.document.template.render.DocumentData("WALLET_STATEMENT").title("Relevé de portefeuille")
                            .number(wallet.getWalletNumber() + "-" + DATE.format(to).replace("/", "")).source(wallet.getWalletNumber())
                            .fieldsFrom(context, "portefeuille", "Portefeuille", "walletNumber", "titulaire", "Titulaire", "holderName",
                                    "periode", "Période", "periodLabel", "devise", "Devise", "currency", "edite_le", "Édité le", "issuedAt",
                                    "solde_ouverture", "Solde d'ouverture", "openingBalance", "total_credits", "Total des crédits", "totalCredits",
                                    "total_debits", "Total des débits", "totalDebits", "solde_cloture", "Solde de clôture", "closingBalance",
                                    "empreinte", "Empreinte du registre", "lastHash");
            data.table("operations", new com.sni.bokaticowork.features.document.template.render.DocumentData.Table("Opérations", java.util.List.of(
                    new com.sni.bokaticowork.features.document.template.render.DocumentData.Column("date", "Date", false),
                    new com.sni.bokaticowork.features.document.template.render.DocumentData.Column("reference", "Référence", false),
                    new com.sni.bokaticowork.features.document.template.render.DocumentData.Column("nature", "Nature", false),
                    new com.sni.bokaticowork.features.document.template.render.DocumentData.Column("libelle", "Libellé", false),
                    new com.sni.bokaticowork.features.document.template.render.DocumentData.Column("credit", "Crédit", true),
                    new com.sni.bokaticowork.features.document.template.render.DocumentData.Column("debit", "Débit", true),
                    new com.sni.bokaticowork.features.document.template.render.DocumentData.Column("solde", "Solde", true)),
                    lines.stream().map(line -> java.util.List.of(line.date(), line.reference(), line.nature(), line.label(), line.credit(), line.debit(),
                            line.balance())).toList()));
            data.section("Portefeuille", java.util.List.of("portefeuille", "titulaire", "periode", "devise", "edite_le"), null)
                    .section("Soldes", java.util.List.of("solde_ouverture", "total_credits", "total_debits", "solde_cloture"), null)
                    .section("Opérations", java.util.List.of(), "operations")
                    .section("Intégrité", java.util.List.of("empreinte"), null);
            java.util.Optional<byte[]> printed = templatePrinter.tryRender("WALLET_STATEMENT", data);
            if (printed.isPresent()) {
                return printed.get();
            }
        }
        return render(templateEngine.process("wallet/statement", context));
    }

    /**
     * Le recu d'une ecriture, designee par l'un ou l'autre de ses numeros.
     *
     * <p>Une ecriture porte le sien (WLE) et celui de la transaction (WTX) · le releve affiche les
     * deux, et les ecritures anterieures au chainage n'ont que le premier. Exiger le second
     * rendait leur recu introuvable.</p>
     */
    @Transactional(readOnly = true)
    public byte[] receipt(WalletAccount wallet, String reference) {
        WalletLedgerEntry entry = ledgerRepository.findByTransactionNumber(reference)
                .or(() -> ledgerRepository.findByEntryNumber(reference))
                .orElseThrow(() -> new ResourceNotFoundException("Écriture introuvable · " + reference));
        if (!entry.getWallet().getId().equals(wallet.getId())) {
            // Meme message que l'absence · dire « elle existe mais pas chez vous » apprendrait
            // a un tiers qu'un numero est valide.
            throw new ResourceNotFoundException("Écriture introuvable · " + reference);
        }
        WalletLedgerEntryResponse view = paymentMapper.toLedgerEntryResponse(entry);
        Context context = new Context(appLocale);
        context.setVariable("entry", view);
        context.setVariable("holderName", holderName(wallet));
        context.setVariable("date", DATE_TIME.format(entry.getCreatedAt().atZone(APP_ZONE)));
        context.setVariable("amount", money(entry.getAmount()));
        context.setVariable("balanceAfter", money(entry.getBalanceAfter()));
        context.setVariable("nature", nature(entry.getEntryType()));
        context.setVariable("directionLabel",
                entry.getDirection() == WalletEntryDirection.CREDIT ? "Montant reçu" : "Montant débité");
        brand(context);
        if (templatePrinter != null) {
            String number = StringUtils.hasText(entry.getTransactionNumber()) ? entry.getTransactionNumber() : entry.getEntryNumber();
            com.sni.bokaticowork.features.document.template.render.DocumentData data =
                    new com.sni.bokaticowork.features.document.template.render.DocumentData("WALLET_RECEIPT").title("Reçu d'opération")
                            .number(number).source(number).amount(entry.getAmount(), wallet.getCurrency())
                            .fieldsFrom(context, "titulaire", "Titulaire", "holderName", "date", "Date", "date", "nature", "Nature", "nature",
                                    "sens", "Sens", "directionLabel", "montant", "Montant", "amount", "solde_apres", "Solde après l'opération",
                                    "balanceAfter")
                            .field("portefeuille", "Portefeuille", wallet.getWalletNumber())
                            .field("reference", "Référence", entry.getReference());
            data.section("Opération", java.util.List.of("portefeuille", "titulaire", "date", "nature", "sens", "reference", "montant", "solde_apres"),
                    null);
            java.util.Optional<byte[]> printed = templatePrinter.tryRender("WALLET_RECEIPT", data);
            if (printed.isPresent()) {
                return printed.get();
            }
        }
        return render(templateEngine.process("wallet/transaction-receipt", context));
    }

    // -----------------------------------------------------------------------------------------

    /** En-tete commun des pieces · identite de l'exploitant et logo encre. */
    private void brand(Context context) {
        context.setVariable("operator", operatorIdentity.current());
        context.setVariable("logo", brandLogo.base64());
    }

    private String nature(WalletEntryType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case ADMIN_TOPUP -> "Rechargement au guichet";
            case TOPUP -> "Rechargement";
            case TRANSFER_IN -> "Transfert reçu";
            case TRANSFER_OUT -> "Transfert envoyé";
            case TRANSFER_FEE -> "Frais de transfert";
            case ADMIN_DEBIT -> "Débit administratif";
            case PAYMENT -> "Paiement";
            case REFUND -> "Remboursement";
            case REVERSAL -> "Contre-passation";
            case HOLD -> "Retenue";
            case HOLD_RELEASE -> "Retenue libérée";
            case ADJUSTMENT -> "Ajustement";
            case CASHBACK -> "Cashback";
            case PROMOTIONAL_CREDIT -> "Crédit promotionnel";
            case OVERPAYMENT_CREDIT -> "Trop-perçu crédité";
        };
    }

    private String holderName(WalletAccount wallet) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode());
        return StringUtils.hasText(party.name()) ? party.name() : wallet.getOwnerCode();
    }

    private String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private byte[] render(String html) {
        try {
            return PdfRenderer.render(html);
        } catch (Exception ex) {
            throw new BadRequestException("Impossible de générer le document PDF du portefeuille", ex);
        }
    }
}

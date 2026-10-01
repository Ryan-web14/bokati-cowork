package com.sni.bokaticowork.features.payment.transfer.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
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

import java.io.ByteArrayOutputStream;
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

    public record StatementLine(String date, String reference, String nature, String label,
                                String credit, String debit, String balance) {
    }

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
        return render(templateEngine.process("wallet/transaction-receipt", context));
    }

    // -----------------------------------------------------------------------------------------

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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(html);
            document.outputSettings()
                    .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                    .charset(java.nio.charset.StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(document), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Impossible de générer le document PDF du portefeuille", ex);
        }
    }
}

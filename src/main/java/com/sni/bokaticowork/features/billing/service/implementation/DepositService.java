package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.DepositPlanRequest;
import com.sni.bokaticowork.features.billing.dto.request.DepositSettlementRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.DepositResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDepositMode;
import com.sni.bokaticowork.features.billing.enums.BillingDepositStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDeposit;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDepositRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Acomptes, du devis a la facture (lot B3).
 *
 * <p>Un devis (a tout moment avant sa conversion) ou une facture encore en brouillon porte un plan
 * d'acomptes. Chaque acompte donne une <b>facture d'acompte</b> : une facture ordinaire, numerotee,
 * envoyee et encaissable par tous les moyens, rattachee a la piece. L'encaissement suit l'acompte ;
 * un devis passe en DEPOSIT_PAID quand ses acomptes factures sont couverts. A l'emission de la
 * facture finale, chaque acompte encaisse est deduit (une imputation par acompte) et seul le reste
 * est reclame ; un acompte facture mais non regle tombe, la facture finale reclamant le tout.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DepositService {

    private static final Set<BillingDocumentStatus> QUOTE_CLOSED = EnumSet.of(BillingDocumentStatus.CONVERTED, BillingDocumentStatus.REJECTED,
            BillingDocumentStatus.CANCELLED, BillingDocumentStatus.EXPIRED);
    static final Set<BillingDepositStatus> OPEN = EnumSet.of(BillingDepositStatus.PLANNED, BillingDepositStatus.INVOICED,
            BillingDepositStatus.PARTIALLY_PAID, BillingDepositStatus.PAID);

    private final BillingDepositRepository deposits;
    private final BillingDocumentRepository documents;
    private final BillingDocumentLineRepository lines;
    private final BillingDocumentServiceImpl billing;
    private final BillingEventWriter eventWriter;

    // ------------------------------------------------------------------ plan d'acomptes

    /** Un ou plusieurs acomptes sur un devis (avant conversion) ou une facture en brouillon. */
    public List<DepositResponse> plan(String documentNumber, DepositPlanRequest request) {
        BillingDocument document = billing.serviceByNumber(documentNumber);
        if (document.getDocumentType() == BillingDocumentType.QUOTE) {
            if (QUOTE_CLOSED.contains(document.getStatus())) {
                throw new BadRequestException("Aucun acompte sur un devis au statut " + document.getStatus());
            }
        } else if (document.getDocumentType() == BillingDocumentType.INVOICE) {
            if (StringUtils.hasText(document.getDepositForDocumentNumber())) {
                throw new BadRequestException("Une facture d'acompte ne porte pas d'acompte");
            }
            if (document.getStatus() != BillingDocumentStatus.DRAFT) {
                throw new BadRequestException("Une facture émise se règle par échéancier ; les acomptes se prévoient avant son émission");
            }
        } else {
            throw new BadRequestException("Un acompte se demande sur un devis ou une facture");
        }
        List<BillingDocumentLine> documentLines = lines.findAllByDocumentOrderByLineOrderAscIdAsc(document);
        BigDecimal planned = deposits.findByDocumentNumberOrderByRankAsc(documentNumber).stream()
                .filter(deposit -> OPEN.contains(deposit.getStatus()) || deposit.getStatus() == BillingDepositStatus.DEDUCTED)
                .map(BillingDeposit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int rank = deposits.findByDocumentNumberOrderByRankAsc(documentNumber).stream().mapToInt(BillingDeposit::getRank).max().orElse(0);
        String actor = actor();
        List<BillingDeposit> created = new java.util.ArrayList<>();
        for (DepositPlanRequest.Deposit input : request.deposits()) {
            BigDecimal amount = amountOf(document, documentLines, input);
            planned = planned.add(amount);
            if (planned.compareTo(document.getTotalAmount()) > 0) {
                throw new BadRequestException("Les acomptes dépassent le total de la pièce (" + plain(document.getTotalAmount()) + ")");
            }
            BillingDeposit deposit = deposits.save(BillingDeposit.builder().documentId(document.getId()).documentNumber(documentNumber)
                    .finalDocumentNumber(document.getDocumentType() == BillingDocumentType.INVOICE ? documentNumber : null)
                    .rank(++rank).label(StringUtils.hasText(input.label()) ? input.label().trim() : null).mode(input.mode()).value(input.value())
                    .baseLineOrders(input.lineOrders() == null || input.lineOrders().isEmpty() ? null
                            : input.lineOrders().stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .amount(amount).dueDate(input.dueDate()).status(BillingDepositStatus.PLANNED).createdBy(actor).build());
            if (input.invoiceNow() == null || input.invoiceNow()) {
                issueDepositInvoice(document, deposit);
            }
            created.add(deposit);
        }
        if (document.getDocumentType() == BillingDocumentType.QUOTE && document.getStatus() == BillingDocumentStatus.ACCEPTED
                && created.stream().anyMatch(deposit -> deposit.getStatus() == BillingDepositStatus.INVOICED)) {
            document.setStatus(BillingDocumentStatus.DEPOSIT_REQUESTED);
            documents.save(document);
        }
        eventWriter.write(document, "DEPOSITS_PLANNED", Map.of("count", created.size(),
                "amount", plain(created.stream().map(BillingDeposit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))));
        return created.stream().map(DepositService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> list(String documentNumber) {
        List<BillingDeposit> own = deposits.findByDocumentNumberOrderByRankAsc(documentNumber);
        List<BillingDeposit> inherited = deposits.findByFinalDocumentNumberOrderByRankAsc(documentNumber).stream()
                .filter(deposit -> !deposit.getDocumentNumber().equals(documentNumber)).toList();
        return java.util.stream.Stream.concat(own.stream(), inherited.stream()).map(DepositService::view).toList();
    }

    /** Facture un acompte prevu au plan. */
    public DepositResponse invoice(Long id) {
        BillingDeposit deposit = deposit(id);
        if (deposit.getStatus() != BillingDepositStatus.PLANNED) {
            throw new BadRequestException("Cet acompte est déjà facturé ou clos");
        }
        issueDepositInvoice(billing.serviceByNumber(deposit.getDocumentNumber()), deposit);
        return view(deposit);
    }

    /** Retrait d'un acompte non encaisse : il tombe, sa facture d'acompte est annulee. */
    public DepositResponse cancel(Long id, String reason) {
        BillingDeposit deposit = deposit(id);
        if (deposit.getPaidAmount().signum() > 0 || !OPEN.contains(deposit.getStatus())) {
            throw new BadRequestException("Un acompte encaissé ne s'annule pas : remboursez-le ou gardez-le en avoir");
        }
        drop(deposit, StringUtils.hasText(reason) ? reason.trim() : "Acompte retiré du plan");
        return view(deposit);
    }

    /**
     * Acompte encaisse sur un devis refuse, expire ou annule (D4) : avoir sur la facture d'acompte,
     * garde pour le client ou reverse a son portefeuille.
     */
    public DepositResponse settle(Long id, DepositSettlementRequest request) {
        BillingDeposit deposit = deposit(id);
        BillingDocument source = billing.serviceByNumber(deposit.getDocumentNumber());
        if (deposit.getPaidAmount().signum() <= 0 || deposit.getStatus() == BillingDepositStatus.DEDUCTED || !OPEN.contains(deposit.getStatus())) {
            throw new BadRequestException("Seul un acompte encaissé, non déduit, se rembourse ou se garde en avoir");
        }
        if (source.getDocumentType() == BillingDocumentType.QUOTE && !QUOTE_CLOSED.contains(source.getStatus())) {
            throw new BadRequestException("Le devis est toujours en cours : l'acompte sera déduit de sa facture");
        }
        BillingDocumentResponse creditNote = billing.createCreditNote(deposit.getDepositInvoiceNumber(),
                new CreateCreditNoteRequest(deposit.getPaidAmount(), request.reason().trim(), false, true, null));
        if ("REFUND".equals(request.mode())) {
            billing.refundCreditNoteToWallet(creditNote.documentNumber(), request.reason().trim(), null, null);
            deposit.setStatus(BillingDepositStatus.REFUNDED);
        } else {
            deposit.setStatus(BillingDepositStatus.CREDITED);
        }
        deposit.setSettlementDocument(creditNote.documentNumber());
        deposit.setCancelReason(request.reason().trim());
        return view(deposits.save(deposit));
    }

    // ------------------------------------------------------------------ crochets du service des documents

    /** Encaissement (ou annulation d'encaissement) d'une facture d'acompte : l'acompte et le devis suivent. */
    void onDepositInvoicePayment(BillingDocument depositInvoice) {
        deposits.findFirstByDepositInvoiceNumber(depositInvoice.getDocumentNumber()).ifPresent(deposit -> {
            if (!OPEN.contains(deposit.getStatus())) {
                return;
            }
            BigDecimal paid = depositInvoice.getPaidAmount() == null ? BigDecimal.ZERO : depositInvoice.getPaidAmount();
            deposit.setPaidAmount(paid);
            deposit.setStatus(paid.signum() <= 0 ? BillingDepositStatus.INVOICED
                    : paid.compareTo(deposit.getAmount()) >= 0 ? BillingDepositStatus.PAID : BillingDepositStatus.PARTIALLY_PAID);
            deposits.save(deposit);
            BillingDocument source = documents.findByDocumentNumber(deposit.getDocumentNumber()).orElse(null);
            if (source != null && source.getDocumentType() == BillingDocumentType.QUOTE) {
                syncQuote(source);
            }
        });
    }

    /** Conversion du devis : ses acomptes ouverts seront deduits de la facture creee. */
    void transferQuoteDeposits(BillingDocument quote, String invoiceNumber) {
        for (BillingDeposit deposit : deposits.findByDocumentNumberOrderByRankAsc(quote.getDocumentNumber())) {
            if (OPEN.contains(deposit.getStatus())) {
                deposit.setFinalDocumentNumber(invoiceNumber);
                deposits.save(deposit);
            }
        }
    }

    /**
     * Emission de la facture finale : chaque acompte encaisse est deduit (imputation sur la facture,
     * piece comptable 4191 contre 411) ; un acompte non encaisse tombe, la facture reclamant le tout.
     */
    void deductOnFinalInvoice(BillingDocument invoice) {
        for (BillingDeposit deposit : deposits.findByFinalDocumentNumberOrderByRankAsc(invoice.getDocumentNumber())) {
            if (!OPEN.contains(deposit.getStatus())) {
                continue;
            }
            BigDecimal paid = deposit.getPaidAmount();
            if (paid.signum() > 0) {
                BillingDocument current = billing.serviceByNumber(invoice.getDocumentNumber());
                BigDecimal deducted = paid.min(current.getBalanceDue() == null ? paid : current.getBalanceDue());
                if (deducted.signum() > 0) {
                    billing.applyPayment(invoice.getDocumentNumber(), deducted);
                }
                if (deposit.getStatus() == BillingDepositStatus.PARTIALLY_PAID && StringUtils.hasText(deposit.getDepositInvoiceNumber())) {
                    // Le reste non regle de la facture d'acompte est repris par la facture finale.
                    billing.cancelAndArchive(deposit.getDepositInvoiceNumber(), "Reste repris par la facture finale " + invoice.getDocumentNumber());
                }
                deposit.setStatus(BillingDepositStatus.DEDUCTED);
                deposit.setDeductedAmount(deducted);
                deposit.setDeductedOn(invoice.getDocumentNumber());
                deposit.setDeductedAt(Instant.now());
                deposits.save(deposit);
            } else {
                drop(deposit, "Acompte non réglé : repris par la facture finale " + invoice.getDocumentNumber());
            }
        }
    }

    /** Devis refuse, expire ou annule : ses acomptes non encaisses tombent avec lui. */
    void dropUnpaidQuoteDeposits(BillingDocument quote, String reason) {
        for (BillingDeposit deposit : deposits.findByDocumentNumberOrderByRankAsc(quote.getDocumentNumber())) {
            if (OPEN.contains(deposit.getStatus()) && deposit.getPaidAmount().signum() == 0) {
                drop(deposit, reason);
            }
        }
    }

    /** Lignes « Acompte n° ... du ... » de la facture finale, pour le PDF. */
    @Transactional(readOnly = true)
    public List<DepositResponse> deducted(String finalInvoiceNumber) {
        return deposits.findByFinalDocumentNumberOrderByRankAsc(finalInvoiceNumber).stream()
                .filter(deposit -> deposit.getStatus() == BillingDepositStatus.DEDUCTED).map(DepositService::view).toList();
    }

    // ------------------------------------------------------------------ outils

    private void issueDepositInvoice(BillingDocument source, BillingDeposit deposit) {
        boolean quote = source.getDocumentType() == BillingDocumentType.QUOTE;
        String label = "Acompte n° " + deposit.getRank() + " sur " + (quote ? "le devis " : "la facture ") + source.getDocumentNumber()
                + (deposit.getLabel() != null ? " : " + deposit.getLabel() : "")
                + (deposit.getMode() == BillingDepositMode.PERCENTAGE ? " (" + plain(deposit.getValue()) + " %)" : "");
        // Ligne hors TVA : la TVA est portee par la facture finale (decision D3).
        CreateBillingDocumentLineRequest line = new CreateBillingDocumentLineRequest(1, BillingLineType.ADJUSTMENT, null, label,
                "TVA comprise dans la facture finale, qui déduira cet acompte.", BigDecimal.ONE, deposit.getAmount(), BigDecimal.ZERO, BigDecimal.ZERO,
                false, false, BigDecimal.ZERO, BigDecimal.ZERO, "DEPOSIT", source.getDocumentNumber(), null, null, null, false, null);
        BillingDocumentResponse created = billing.create(new CreateBillingDocumentRequest(BillingDocumentType.INVOICE, source.getCustomerType(),
                source.getCustomerCode(), source.getCustomerName(), source.getCustomerEmail(), source.getCustomerPhone(), source.getBillingAddressJson(),
                quote ? "QUOTE_DEPOSIT" : "INVOICE_DEPOSIT", source.getDocumentNumber(), "Facture d'acompte n° " + deposit.getRank(),
                null, source.getTerms(), source.getCurrency(), LocalDate.now(), deposit.getDueDate() != null ? deposit.getDueDate() : LocalDate.now(),
                null, List.of(line), List.of(), List.of(), source.getPaymentReference(), source.getPaymentInstructions(), source.getBankDetailsJson(),
                null, source.getCustomerReference(), source.getPoNumber(), source.getProjectCode(), source.getSalespersonCode(), null,
                source.getLanguage(), source.getExchangeRate(), null,
                source.getCompanyCustomerCode() == null ? CreateBillingDocumentRequest.NO_COMPANY : source.getCompanyCustomerCode()));
        BillingDocument invoice = billing.serviceByNumber(created.documentNumber());
        invoice.setDepositForDocumentNumber(source.getDocumentNumber());
        documents.save(invoice);
        billing.issue(invoice.getDocumentNumber(), null);
        deposit.setDepositInvoiceNumber(invoice.getDocumentNumber());
        deposit.setStatus(BillingDepositStatus.INVOICED);
        deposits.save(deposit);
    }

    private void drop(BillingDeposit deposit, String reason) {
        if (StringUtils.hasText(deposit.getDepositInvoiceNumber())) {
            BillingDocument depositInvoice = documents.findByDocumentNumber(deposit.getDepositInvoiceNumber()).orElse(null);
            if (depositInvoice != null && depositInvoice.getStatus() != BillingDocumentStatus.CANCELLED) {
                billing.cancelAndArchive(deposit.getDepositInvoiceNumber(), reason);
            }
        }
        deposit.setStatus(BillingDepositStatus.CANCELLED);
        deposit.setCancelReason(reason);
        deposits.save(deposit);
    }

    /** Devis accepte dont les acomptes factures sont tous encaisses : DEPOSIT_PAID. */
    private void syncQuote(BillingDocument quote) {
        List<BillingDeposit> invoiced = deposits.findByDocumentNumberOrderByRankAsc(quote.getDocumentNumber()).stream()
                .filter(deposit -> deposit.getStatus() == BillingDepositStatus.INVOICED || deposit.getStatus() == BillingDepositStatus.PARTIALLY_PAID
                        || deposit.getStatus() == BillingDepositStatus.PAID)
                .toList();
        boolean covered = !invoiced.isEmpty() && invoiced.stream().allMatch(deposit -> deposit.getStatus() == BillingDepositStatus.PAID);
        if (covered && (quote.getStatus() == BillingDocumentStatus.DEPOSIT_REQUESTED || quote.getStatus() == BillingDocumentStatus.ACCEPTED)) {
            quote.setStatus(BillingDocumentStatus.DEPOSIT_PAID);
            documents.save(quote);
        } else if (!covered && quote.getStatus() == BillingDocumentStatus.DEPOSIT_PAID) {
            quote.setStatus(BillingDocumentStatus.DEPOSIT_REQUESTED);
            documents.save(quote);
        }
    }

    static BigDecimal amountOf(BillingDocument document, List<BillingDocumentLine> documentLines, DepositPlanRequest.Deposit input) {
        if (input.mode() == BillingDepositMode.AMOUNT) {
            return input.value().setScale(2, RoundingMode.HALF_UP);
        }
        if (input.value().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Un pourcentage d'acompte ne dépasse pas 100");
        }
        BigDecimal base;
        if (input.lineOrders() == null || input.lineOrders().isEmpty()) {
            base = document.getTotalAmount();
        } else {
            Set<Integer> wanted = Set.copyOf(input.lineOrders());
            base = documentLines.stream().filter(line -> wanted.contains(line.getLineOrder()) && !Boolean.TRUE.equals(line.getOptional()))
                    .map(line -> line.getTotalAmount() == null ? BigDecimal.ZERO : line.getTotalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (base.signum() == 0) {
                throw new BadRequestException("Les lignes choisies (" + input.lineOrders() + ") ne portent aucun montant");
            }
        }
        return base.multiply(input.value()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BillingDeposit deposit(Long id) {
        return deposits.findById(id).orElseThrow(() -> new ResourceNotFoundException("Acompte introuvable : " + id));
    }

    static DepositResponse view(BillingDeposit deposit) {
        return new DepositResponse(deposit.getId(), deposit.getDocumentNumber(), deposit.getFinalDocumentNumber(), deposit.getRank(),
                deposit.getLabel(), deposit.getMode().name(), deposit.getValue(), deposit.getBaseLineOrders(), deposit.getAmount(),
                deposit.getDueDate(), deposit.getStatus().name(), deposit.getDepositInvoiceNumber(), deposit.getPaidAmount(),
                deposit.getDeductedAmount(), deposit.getDeductedOn(), deposit.getDeductedAt(), deposit.getSettlementDocument(),
                deposit.getCancelReason(), deposit.getCreatedAt());
    }

    private static String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !StringUtils.hasText(authentication.getName()) ? "SYSTEM" : authentication.getName();
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}

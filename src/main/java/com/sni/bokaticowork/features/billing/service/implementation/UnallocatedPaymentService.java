package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.UnallocatedPaymentDtos;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lot C9 : paiements non affectes. Une somme encaissee sans facture, ou au-dela de ses factures,
 * attend au compte 4719. Pour chacune, les factures ouvertes du meme client sont proposees, notees
 * (meme montant, numero de facture dans la reference du paiement, echeance proche) ; l'affectation
 * se fait en un geste, sur une ou plusieurs factures, le reliquat reste en attente.
 *
 * <p>Les rechargements de portefeuille et les paiements par portefeuille ne sont pas des sommes en
 * attente : le portefeuille les porte deja.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UnallocatedPaymentService {

    static final String LATE = "LATE";

    private final JdbcTemplate jdbc;
    private final PaymentTransactionRepository transactions;
    private final PaymentAllocationRepository allocations;
    private final BillingDocumentRepository documents;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<BillingDocumentServiceImpl> billingServices;
    private com.sni.bokaticowork.features.billing.element.service.BillingElementResolver elementResolver;
    private com.sni.bokaticowork.features.billing.element.service.BillingElementLinkService elementLinks;

    /** Lot R3 : affectation a un element (facultatif dans les tests). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setElements(com.sni.bokaticowork.features.billing.element.service.BillingElementResolver elementResolver,
                     com.sni.bokaticowork.features.billing.element.service.BillingElementLinkService elementLinks) {
        this.elementResolver = elementResolver;
        this.elementLinks = elementLinks;
    }

    /**
     * Affectation a des factures et a des elements en un geste (lots C9 et R3). Chaque element recoit
     * son montant sur ses factures ouvertes, echeance la plus ancienne d'abord, sans depasser leur
     * reste du : le reliquat reste en attente.
     */
    public UnallocatedPaymentDtos.AllocationResult allocate(String transactionNumber, UnallocatedPaymentDtos.AllocateRequest request) {
        List<UnallocatedPaymentDtos.Allocation> wanted = new ArrayList<>(request == null || request.allocations() == null ? List.of() : request.allocations());
        List<UnallocatedPaymentDtos.ElementAllocation> elements = request == null || request.elements() == null ? List.of() : request.elements();
        Map<String, java.util.Map.Entry<com.sni.bokaticowork.features.billing.element.model.ElementRef, String>> modes = new java.util.LinkedHashMap<>();
        for (UnallocatedPaymentDtos.ElementAllocation element : elements) {
            if (elementResolver == null) {
                throw BadRequestException.coded("AFFECTATION_INVALIDE", "L'affectation à un élément n'est pas disponible.");
            }
            com.sni.bokaticowork.features.billing.element.model.ElementRef ref =
                    com.sni.bokaticowork.features.billing.element.model.ElementRef.of(element.type(), element.code());
            String mode = com.sni.bokaticowork.features.billing.element.model.ElementRef.mode(element.activationMode());
            elementLinks.assertOpen(ref);
            if (element.amount() == null || element.amount().signum() <= 0) {
                throw BadRequestException.coded("AFFECTATION_MONTANT", "Chaque montant affecté est positif.");
            }
            BigDecimal left = element.amount();
            List<com.sni.bokaticowork.features.billing.element.dto.ElementDtos.Invoice> open = elementResolver.openInvoices(ref);
            if (open.isEmpty()) {
                throw BadRequestException.coded("ELEMENT_SANS_FACTURE", ref.type() + " " + ref.code()
                        + " n'a aucune facture à régler : rattachez-lui une facture ou émettez la sienne.");
            }
            for (com.sni.bokaticowork.features.billing.element.dto.ElementDtos.Invoice invoice : open) {
                if (left.signum() <= 0) {
                    break;
                }
                BigDecimal already = wanted.stream().filter(a -> a.documentNumber().equals(invoice.documentNumber()))
                        .map(UnallocatedPaymentDtos.Allocation::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal room = invoice.balanceDue().subtract(already);
                if (room.signum() <= 0) {
                    continue;
                }
                BigDecimal part = left.min(room);
                wanted.add(new UnallocatedPaymentDtos.Allocation(invoice.documentNumber(), part));
                modes.put(invoice.documentNumber() + "|" + ref.type() + "|" + ref.code(), java.util.Map.entry(ref, mode));
                left = left.subtract(part);
            }
        }
        // Le mode choisi est pose avant le paiement : l'annonce de reglement le lit.
        modes.forEach((key, entry) -> {
            if (com.sni.bokaticowork.features.billing.element.model.ElementRef.FIRST_PAYMENT.equals(entry.getValue())) {
                elementLinks.setMode(key.substring(0, key.indexOf('|')), entry.getKey(), entry.getValue());
            }
        });
        return allocate(transactionNumber, wanted);
    }

    /** Sommes en attente, chacune avec ses factures candidates les mieux notees d'abord. */
    @Transactional(readOnly = true)
    public List<UnallocatedPaymentDtos.Suggestion> suggestions(String customerType, String customerCode, int limit) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT t.transaction_number, t.amount, t.currency, t.paid_at, t.created_at, t.provider_reference, t.payer_name,
                       i.customer_type, i.customer_code, i.purpose, i.metadata_json,
                       COALESCE((SELECT SUM(a.allocated_amount) FROM payment_allocation a WHERE a.payment_transaction_id = t.id), 0) AS allocated
                FROM payment_transaction t JOIN payment_intent i ON i.id = t.payment_intent_id
                WHERE t.status = 'SUCCEEDED' AND t.payment_method <> 'WALLET'
                  AND COALESCE(i.source_type, '') NOT LIKE 'WALLET%'
                  AND (CAST(? AS VARCHAR) IS NULL OR (i.customer_type = ? AND i.customer_code = ?))
                  AND t.amount > COALESCE((SELECT SUM(a.allocated_amount) FROM payment_allocation a WHERE a.payment_transaction_id = t.id), 0)
                ORDER BY COALESCE(t.paid_at, t.created_at) DESC
                LIMIT ?
                """, customerCode, customerType, customerCode, Math.max(1, Math.min(limit, 500)));
        List<UnallocatedPaymentDtos.Suggestion> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            BigDecimal amount = (BigDecimal) row.get("amount");
            BigDecimal pending = amount.subtract((BigDecimal) row.get("allocated"));
            String type = (String) row.get("customer_type");
            String code = (String) row.get("customer_code");
            Instant at = row.get("paid_at") instanceof Timestamp paid ? paid.toInstant()
                    : row.get("created_at") instanceof Timestamp created ? created.toInstant() : null;
            String reference = String.join(" ", text(row.get("provider_reference")), text(row.get("payer_name")), text(row.get("purpose")),
                    text(row.get("metadata_json"))).toUpperCase(Locale.ROOT);
            List<UnallocatedPaymentDtos.Candidate> candidates = StringUtils.hasText(code)
                    ? openInvoices(type, code).stream().map(invoice -> candidate(invoice, pending, reference)).sorted(
                    Comparator.comparingInt(UnallocatedPaymentDtos.Candidate::score).reversed()).limit(10).toList()
                    : List.of();
            result.add(new UnallocatedPaymentDtos.Suggestion((String) row.get("transaction_number"), type, code, amount, pending,
                    (String) row.get("currency"), at, text(row.get("provider_reference")), text(row.get("payer_name")), candidates));
        }
        return result;
    }

    /**
     * Affecte une somme en attente a une ou plusieurs factures ouvertes du meme client. Chaque
     * montant ne depasse ni le reste du de sa facture, ni ce qui reste en attente.
     */
    public UnallocatedPaymentDtos.AllocationResult allocate(String transactionNumber, List<UnallocatedPaymentDtos.Allocation> wanted) {
        if (wanted == null || wanted.isEmpty()) {
            throw BadRequestException.coded("AFFECTATION_INVALIDE", "Indiquez au moins une facture et un montant.");
        }
        PaymentTransaction transaction = transactions.findByTransactionNumber(transactionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable : " + transactionNumber));
        if (transaction.getStatus() != com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus.SUCCEEDED) {
            throw BadRequestException.coded("PAIEMENT_NON_REUSSI", "Le paiement " + transactionNumber + " est " + transaction.getStatus()
                    + " : seul un paiement réussi, non remboursé, s'affecte.");
        }
        BigDecimal pending = transaction.getAmount().subtract(allocations.findAllByPaymentTransactionId(transaction.getId()).stream()
                .map(PaymentAllocation::getAllocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal total = wanted.stream().map(UnallocatedPaymentDtos.Allocation::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0 || total.compareTo(pending) > 0) {
            throw BadRequestException.coded("AFFECTATION_MONTANT", "Il reste " + plain(pending) + " en attente sur " + transactionNumber
                    + " : l'affectation demandée (" + plain(total) + ") le dépasse.");
        }
        String customerCode = transaction.getPaymentIntent() == null ? null : transaction.getPaymentIntent().getCustomerCode();
        String author = staffProfiles.currentAuthor().login();
        List<String> done = new ArrayList<>();
        for (UnallocatedPaymentDtos.Allocation allocation : wanted) {
            if (allocation.amount() == null || allocation.amount().signum() <= 0) {
                throw BadRequestException.coded("AFFECTATION_MONTANT", "Chaque montant affecté est positif.");
            }
            BillingDocument invoice = documents.findByDocumentNumber(allocation.documentNumber().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + allocation.documentNumber()));
            if (customerCode != null && !customerCode.equals(invoice.getCustomerCode())) {
                throw BadRequestException.coded("AFFECTATION_INVALIDE", "La facture " + invoice.getDocumentNumber()
                        + " n'est pas du client qui a payé (" + customerCode + ").");
            }
            if (invoice.getBalanceDue() == null || allocation.amount().compareTo(invoice.getBalanceDue()) > 0) {
                throw BadRequestException.coded("AFFECTATION_MONTANT", "Le montant dépasse le reste dû de " + invoice.getDocumentNumber() + ".");
            }
            billingServices.getObject().applyPayment(invoice.getDocumentNumber(), allocation.amount());
            allocations.save(PaymentAllocation.builder().paymentTransaction(transaction).billingDocumentNumber(invoice.getDocumentNumber())
                    .allocatedAmount(allocation.amount()).allocatedAt(Instant.now()).allocationMode(LATE).allocatedBy(author).build());
            done.add(invoice.getDocumentNumber());
        }
        return new UnallocatedPaymentDtos.AllocationResult(transactionNumber, total, pending.subtract(total), done);
    }

    private List<BillingDocument> openInvoices(String customerType, String customerCode) {
        return jdbc.queryForList("""
                SELECT d.document_number FROM billing_document d
                WHERE d.customer_type = ? AND d.customer_code = ? AND d.document_type IN ('INVOICE', 'CORRECTIVE_INVOICE', 'DEBIT_NOTE')
                  AND d.status NOT IN ('DRAFT', 'CANCELLED', 'PAID') AND d.balance_due > 0 AND d.deleted_at IS NULL
                ORDER BY d.due_date NULLS LAST LIMIT 50
                """, String.class, customerType, customerCode).stream()
                .map(number -> documents.findByDocumentNumber(number).orElse(null)).filter(java.util.Objects::nonNull).toList();
    }

    /** Note : meme montant 50, numero dans la reference 40, echeance a moins de 15 jours 10. */
    static UnallocatedPaymentDtos.Candidate candidate(BillingDocument invoice, BigDecimal pending, String reference) {
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (invoice.getBalanceDue().compareTo(pending) == 0) {
            score += 50;
            reasons.add("même montant");
        }
        if (reference != null && reference.contains(invoice.getDocumentNumber().toUpperCase(Locale.ROOT))) {
            score += 40;
            reasons.add("numéro dans la référence du paiement");
        }
        if (invoice.getDueDate() != null && Math.abs(ChronoUnit.DAYS.between(LocalDate.now(), invoice.getDueDate())) <= 15) {
            score += 10;
            reasons.add("échéance proche");
        }
        return new UnallocatedPaymentDtos.Candidate(invoice.getDocumentNumber(), invoice.getTitle(), invoice.getBalanceDue(),
                invoice.getDueDate(), score, reasons, invoice.getBalanceDue().min(pending));
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}

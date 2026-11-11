package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEditHistory;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEditHistoryRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.booking.model.CancellationPolicy;
import com.sni.bokaticowork.features.booking.repository.CancellationPolicyRepository;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCancellationRefundSupport {

    private static final Set<BillingDocumentStatus> ELIGIBLE_INVOICE_STATUSES = Set.of(
            BillingDocumentStatus.ISSUED, BillingDocumentStatus.SENT,
            BillingDocumentStatus.PAID, BillingDocumentStatus.PARTIALLY_PAID
    );

    private final BillingDocumentRepository billingDocumentRepository;
    private final BillingDocumentEditHistoryRepository editHistoryRepository;
    private final BillingDocumentService billingDocumentService;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final BillableItemRepository billableItemRepository;
    private final BookingRepository bookingRepository;

    /**
     * Solde la facture d'une reservation annulee, selon que de l'argent a ete encaisse ou non.
     *
     * <p>Rien n'a ete percu : la facture est annulee et archivee. Elle ne vaut plus rien, et la
     * laisser vivante revenait a reclamer le prix d'une reservation qui n'existe plus · c'est ce
     * que le systeme faisait, silencieusement, pour toute reservation annulee avant paiement.</p>
     *
     * <p>De l'argent a ete percu : la facture ne s'annule plus, elle se corrige. La politique
     * d'annulation dit quelle part revient au client, un avoir la constate, et le remboursement
     * suit le chemin du paiement d'origine · un reglement au portefeuille y retourne.</p>
     *
     * @param wasActive la reservation occupait-elle reellement son creneau. La politique
     *                  d'annulation ne s'applique qu'a ce cas ; une reservation qui n'avait rien
     *                  engage ne donne lieu a aucun calcul de penalite.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCancellation(Booking booking, boolean wasActive) {
        if (!org.springframework.util.StringUtils.hasText(booking.getBillableNumber())) {
            return;
        }
        BillingDocument invoice = billingDocumentRepository
                .findFirstBySourceAndType("BILLABLE_ITEM", booking.getBillableNumber(), BillingDocumentType.INVOICE.name())
                .orElse(null);
        if (invoice == null && org.springframework.util.StringUtils.hasText(booking.getBatchNumber())) {
            BillingDocument grouped = groupInvoice(booking);
            if (grouped != null) {
                processGroupedCancellation(booking, grouped, wasActive);
            }
            return;
        }
        if (invoice == null || !ELIGIBLE_INVOICE_STATUSES.contains(invoice.getStatus())) {
            return;
        }
        BigDecimal paidAmount = invoice.getPaidAmount();
        if (paidAmount == null || paidAmount.signum() <= 0) {
            billingDocumentService.cancelAndArchive(invoice.getDocumentNumber(),
                    "Annulation reservation " + booking.getBookingNumber() + " · aucun reglement encaisse");
            return;
        }
        if (!wasActive) {
            return;
        }

        long hoursBeforeStart = Duration.between(LocalDateTime.now(), booking.getStartedAt()).toHours();
        CancellationPolicy policy = cancellationPolicyRepository.findAllByActiveTrueOrderByRuleOrderAsc().stream()
                .filter(p -> hoursBeforeStart >= p.getHoursBeforeStart())
                .findFirst()
                .orElse(null);
        if (policy == null || policy.getRefundPercentage() == null || policy.getRefundPercentage().signum() <= 0) {
            return;
        }

        BigDecimal refundAmount = paidAmount.multiply(policy.getRefundPercentage())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (refundAmount.signum() <= 0) {
            return;
        }

        String reason = "Annulation reservation " + booking.getBookingNumber()
                + " · politique d'annulation (" + policy.getRefundPercentage().stripTrailingZeros().toPlainString() + "%)";

        billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                new CreateCreditNoteRequest(refundAmount, reason, true, null, null));

        refundPaidTransactions(invoice.getDocumentNumber(), refundAmount, reason);

        writeHistory(invoice, policy, hoursBeforeStart, refundAmount, booking.getBookingNumber());
    }

    /**
     * La facture commune d'un lot multi-ressources, retrouvee par l'element facturable de la
     * reservation · sa source liste plusieurs elements, la recherche exacte ne la trouve pas.
     */
    private BillingDocument groupInvoice(Booking booking) {
        return billableItemRepository.findByBillableNumber(booking.getBillableNumber())
                .map(BillableItem::getInvoiceId)
                .flatMap(billingDocumentRepository::findById)
                .filter(document -> document.getDocumentType() == BillingDocumentType.INVOICE)
                .orElse(null);
    }

    /**
     * Une reservation annulee dans une facture qui en regle plusieurs (lot multi-ressources).
     *
     * <p>Annuler toute la facture annulerait aussi les autres reservations du membre. On retire
     * donc seulement la part de cette reservation : rien d'encaisse, un avoir de son montant reduit
     * le reste du ; deja encaisse, la politique d'annulation s'applique a sa part, comme pour une
     * reservation seule. Quand plus aucune reservation de la facture ne reste et que rien n'a ete
     * encaisse, la facture est annulee.</p>
     */
    /**
     * Une reservation de lot refusee a l'approbation : sa part sort de la facture commune. Rien
     * n'avait ete promis au client, donc pas de politique d'annulation · ce qu'il a deja paye pour
     * elle lui est rendu en entier.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRejection(Booking booking) {
        if (!org.springframework.util.StringUtils.hasText(booking.getBillableNumber())
                || !org.springframework.util.StringUtils.hasText(booking.getBatchNumber())) {
            return;
        }
        BillingDocument invoice = groupInvoice(booking);
        if (invoice == null) {
            return;
        }
        if (invoice.getSourceCode() == null || !invoice.getSourceCode().contains(",")) {
            // Seule reservation de sa facture · la facture tombe avec elle
            processCancellation(booking, false);
            return;
        }
        processGroupedCancellation(booking, invoice, false, true);
    }

    private void processGroupedCancellation(Booking booking, BillingDocument invoice, boolean wasActive) {
        processGroupedCancellation(booking, invoice, wasActive, false);
    }

    private void processGroupedCancellation(Booking booking, BillingDocument invoice, boolean wasActive, boolean fullRefund) {
        if (!ELIGIBLE_INVOICE_STATUSES.contains(invoice.getStatus())) {
            return;
        }
        BigDecimal share = booking.getPaymentMode() != null && booking.getPaymentMode().isPayable()
                ? booking.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paidAmount = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
        String reason = "Annulation reservation " + booking.getBookingNumber() + " (lot " + booking.getBatchNumber() + ")";

        if (paidAmount.signum() <= 0) {
            if (!otherBookingsStillBilled(booking, invoice)) {
                billingDocumentService.cancelAndArchive(invoice.getDocumentNumber(),
                        reason + " · plus aucune reservation facturee, aucun reglement encaisse");
                return;
            }
            if (share == null || share.signum() <= 0) {
                return;
            }
            billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                    new CreateCreditNoteRequest(share, reason + " · aucun reglement encaisse", true, null, null));
            return;
        }
        if (share == null || share.signum() <= 0) {
            return;
        }
        if (fullRefund) {
            // L'avoir retire toute la part ; on ne rend que ce qui depasse le nouveau total, un
            // reglement partiel ayant pu servir aux autres reservations de la facture.
            String refusal = reason.replace("Annulation", "Refus") + " · retire de la facture";
            billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                    new CreateCreditNoteRequest(share, refusal, true, null, null));
            BigDecimal total = invoice.getTotalAmount() == null ? share : invoice.getTotalAmount();
            BigDecimal refund = paidAmount.subtract(total.subtract(share)).min(share);
            if (refund.signum() > 0) {
                refundPaidTransactions(invoice.getDocumentNumber(), refund, refusal);
            }
            return;
        }
        if (!wasActive) {
            return;
        }

        long hoursBeforeStart = Duration.between(LocalDateTime.now(), booking.getStartedAt()).toHours();
        CancellationPolicy policy = cancellationPolicyRepository.findAllByActiveTrueOrderByRuleOrderAsc().stream()
                .filter(p -> hoursBeforeStart >= p.getHoursBeforeStart())
                .findFirst()
                .orElse(null);
        if (policy == null || policy.getRefundPercentage() == null || policy.getRefundPercentage().signum() <= 0) {
            return;
        }
        BigDecimal refundAmount = share.min(paidAmount).multiply(policy.getRefundPercentage())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (refundAmount.signum() <= 0) {
            return;
        }
        String policyReason = reason + " · politique d'annulation ("
                + policy.getRefundPercentage().stripTrailingZeros().toPlainString() + "%)";
        billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                new CreateCreditNoteRequest(refundAmount, policyReason, true, null, null));
        refundPaidTransactions(invoice.getDocumentNumber(), refundAmount, policyReason);
        writeHistory(invoice, policy, hoursBeforeStart, refundAmount, booking.getBookingNumber());
    }

    /** Une autre reservation de la facture reste-t-elle a facturer · ni annulee, ni refusee. */
    private boolean otherBookingsStillBilled(Booking booking, BillingDocument invoice) {
        if (invoice.getSourceCode() == null) {
            return false;
        }
        for (String billableNumber : invoice.getSourceCode().split(",")) {
            String number = billableNumber.trim();
            if (number.isEmpty() || number.equalsIgnoreCase(booking.getBillableNumber())) {
                continue;
            }
            boolean live = bookingRepository.findByBillableNumber(number)
                    .filter(other -> !other.getBookingNumber().equals(booking.getBookingNumber()))
                    .map(other -> other.getStatus() != BookingStatus.CANCELLED && other.getStatus() != BookingStatus.REJECTED)
                    .orElse(false);
            if (live) {
                return true;
            }
        }
        return false;
    }

    private void refundPaidTransactions(String invoiceNumber, BigDecimal refundAmount, String reason) {
        List<PaymentAllocation> allocations = paymentAllocationRepository.findAllByBillingDocumentNumberFetchTransaction(invoiceNumber);
        BigDecimal remaining = refundAmount;
        for (PaymentAllocation allocation : allocations) {
            if (remaining.signum() <= 0) {
                break;
            }
            PaymentTransaction transaction = allocation.getPaymentTransaction();
            if (transaction == null || transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
                continue;
            }
            BigDecimal refundable = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
            BigDecimal toRefund = refundable.min(remaining);
            if (toRefund.signum() <= 0) {
                continue;
            }
            try {
                paymentService.refund(transaction.getTransactionNumber(),
                        new RefundPaymentRequest(toRefund, reason, "SYSTEM_BOOKING_CANCELLATION"));
                remaining = remaining.subtract(toRefund);
            } catch (Exception ex) {
                log.warn("Failed to refund transaction {} for cancellation credit note on invoice {}",
                        transaction.getTransactionNumber(), invoiceNumber, ex);
            }
        }
    }

    private void writeHistory(BillingDocument invoice, CancellationPolicy policy, long hoursBeforeStart, BigDecimal refundAmount, String bookingNumber) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("bookingNumber", bookingNumber);
            snapshot.put("refundPercentage", policy.getRefundPercentage());
            snapshot.put("hoursBeforeStart", hoursBeforeStart);
            snapshot.put("policyRuleOrder", policy.getRuleOrder());
            snapshot.put("refundAmount", refundAmount);
            editHistoryRepository.save(BillingDocumentEditHistory.builder()
                    .document(invoice)
                    .editType("CANCELLATION_AUTO_CREDIT_NOTE")
                    .changedBy("SYSTEM_BOOKING_CANCELLATION")
                    .snapshotJson(objectMapper.writeValueAsString(snapshot))
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to record cancellation credit note history for invoice {}", invoice.getDocumentNumber(), ex);
        }
    }
}

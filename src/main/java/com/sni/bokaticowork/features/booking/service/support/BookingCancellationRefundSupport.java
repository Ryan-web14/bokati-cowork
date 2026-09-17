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
import com.sni.bokaticowork.features.booking.model.Booking;
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

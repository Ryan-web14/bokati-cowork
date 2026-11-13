package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateInstallmentInput;
import com.sni.bokaticowork.features.billing.dto.request.CreatePaymentScheduleRequest;
import com.sni.bokaticowork.features.billing.dto.response.PaymentScheduleInstallmentResponse;
import com.sni.bokaticowork.features.billing.dto.response.PaymentScheduleResponse;
import com.sni.bokaticowork.features.billing.enums.PaymentScheduleInstallmentStatus;
import com.sni.bokaticowork.features.billing.enums.PaymentScheduleStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.PaymentSchedule;
import com.sni.bokaticowork.features.billing.model.PaymentScheduleInstallment;
import com.sni.bokaticowork.features.billing.repository.PaymentScheduleInstallmentRepository;
import com.sni.bokaticowork.features.billing.repository.PaymentScheduleRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.PaymentScheduleService;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentScheduleServiceImpl implements PaymentScheduleService {

    private final PaymentScheduleRepository scheduleRepository;
    private final PaymentScheduleInstallmentRepository installmentRepository;
    private final BillingDocumentService billingDocumentService;
    private final PaymentService paymentService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public PaymentScheduleResponse createSchedule(String documentNumber, CreatePaymentScheduleRequest request) {
        BillingDocument document = billingDocumentService.serviceByNumber(documentNumber);
        // Lot B9 (E12) : seulement une facture, rectificative ou note de debit emise, non soldee.
        if (document.getDocumentType() != com.sni.bokaticowork.features.billing.enums.BillingDocumentType.INVOICE
                && document.getDocumentType() != com.sni.bokaticowork.features.billing.enums.BillingDocumentType.CORRECTIVE_INVOICE
                && document.getDocumentType() != com.sni.bokaticowork.features.billing.enums.BillingDocumentType.DEBIT_NOTE) {
            throw new BadRequestException("Un échéancier se fait sur une facture, une rectificative ou une note de débit");
        }
        if (document.getStatus() == com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus.DRAFT
                || document.getStatus() == com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus.CANCELLED) {
            throw new BadRequestException("Un échéancier se fait sur une pièce émise");
        }
        if (document.getBalanceDue().signum() <= 0) {
            throw new BadRequestException("Cette facture n'a pas de solde restant à échelonner");
        }
        scheduleRepository.findActiveByDocumentNumber(documentNumber).ifPresent(s -> {
            throw ConflictException.coded("PAYMENT_SCHEDULE", "Un échéancier actif existe déjà pour cette facture");
        });

        BigDecimal totalInstallments = request.installments().stream()
                .map(CreateInstallmentInput::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalInstallments.compareTo(document.getBalanceDue()) > 0) {
            throw new BadRequestException("Le total des acomptes (" + totalInstallments
                    + ") dépasse le solde restant de la facture (" + document.getBalanceDue() + ")");
        }

        PaymentSchedule schedule = scheduleRepository.save(PaymentSchedule.builder()
                .scheduleNumber(sequenceGenerator.next("payment_schedule"))
                .billingDocumentNumber(documentNumber)
                .status(PaymentScheduleStatus.ACTIVE)
                .totalAmount(totalInstallments)
                .paidAmount(BigDecimal.ZERO)
                .paidBefore(document.getPaidAmount() == null ? BigDecimal.ZERO : document.getPaidAmount())
                .currency(document.getCurrency())
                .notes(trim(request.notes()))
                .build());

        List<PaymentScheduleInstallment> saved = new ArrayList<>();
        for (int i = 0; i < request.installments().size(); i++) {
            CreateInstallmentInput input = request.installments().get(i);
            int order = i + 1;
            String label = StringUtils.hasText(input.label()) ? input.label().trim() : "Acompte " + order;
            saved.add(installmentRepository.save(PaymentScheduleInstallment.builder()
                    .installmentNumber(sequenceGenerator.next("payment_schedule_installment"))
                    .schedule(schedule)
                    .installmentOrder(order)
                    .label(label)
                    .amount(input.amount())
                    .dueDate(input.dueDate())
                    .paidAmount(BigDecimal.ZERO)
                    .status(PaymentScheduleInstallmentStatus.PENDING)
                    .build()));
        }
        return toResponse(schedule, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentScheduleResponse getSchedule(String scheduleNumber) {
        PaymentSchedule schedule = findByNumber(scheduleNumber);
        return toResponse(schedule, installmentRepository.findByScheduleIdOrdered(schedule.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentScheduleResponse getScheduleByDocument(String documentNumber) {
        PaymentSchedule schedule = scheduleRepository.findActiveByDocumentNumber(documentNumber)
                .or(() -> scheduleRepository.findLatestByDocumentNumber(documentNumber))
                .orElseThrow(() -> new ResourceNotFoundException("Aucun échéancier trouvé pour cette facture"));
        return toResponse(schedule, installmentRepository.findByScheduleIdOrdered(schedule.getId()));
    }

    @Override
    public PaymentScheduleResponse payInstallment(String installmentNumber, PayInvoiceRequest request) {
        PaymentScheduleInstallment installment = installmentRepository.findByInstallmentNumber(installmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Acompte introuvable: " + installmentNumber));

        if (installment.getStatus() == PaymentScheduleInstallmentStatus.PAID
                || installment.getStatus() == PaymentScheduleInstallmentStatus.CANCELLED) {
            throw new BadRequestException("Cet acompte ne peut plus être payé (statut: " + installment.getStatus() + ")");
        }

        PaymentSchedule schedule = installment.getSchedule();
        BigDecimal installmentBalance = installment.getAmount().subtract(installment.getPaidAmount());
        BigDecimal amountToPay = (request.amount() != null && request.amount().signum() > 0)
                ? request.amount().min(installmentBalance)
                : installmentBalance;

        paymentService.payInvoice(schedule.getBillingDocumentNumber(), new PayInvoiceRequest(
                request.paymentMethod(),
                amountToPay,
                request.walletNumber(),
                request.cashSessionNumber(),
                request.providerReference(),
                request.processedBy(),
                request.idempotencyKey(),
                request.metadataJson()
        ));

        // Lot B9 (E12) : le paiement s'impute sur la facture, qui repartit sur les echeances dans l'ordre.
        syncWithInvoice(billingDocumentService.serviceByNumber(schedule.getBillingDocumentNumber()));
        return getSchedule(schedule.getScheduleNumber());
    }

    @Override
    public PaymentScheduleResponse cancelSchedule(String scheduleNumber) {
        PaymentSchedule schedule = findByNumber(scheduleNumber);
        if (schedule.getStatus() == PaymentScheduleStatus.COMPLETED) {
            throw new BadRequestException("Un échéancier complété ne peut pas être annulé");
        }
        schedule.setStatus(PaymentScheduleStatus.CANCELLED);
        scheduleRepository.save(schedule);
        installmentRepository.findByScheduleIdOrdered(schedule.getId()).stream()
                .filter(i -> i.getStatus() == PaymentScheduleInstallmentStatus.PENDING
                        || i.getStatus() == PaymentScheduleInstallmentStatus.OVERDUE)
                .forEach(i -> {
                    i.setStatus(PaymentScheduleInstallmentStatus.CANCELLED);
                    installmentRepository.save(i);
                });
        return getSchedule(scheduleNumber);
    }

    @Override
    public int markOverdueInstallments() {
        return installmentRepository.markOverdue(LocalDate.now());
    }

    private PaymentSchedule findByNumber(String scheduleNumber) {
        return scheduleRepository.findByScheduleNumber(scheduleNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Échéancier introuvable: " + scheduleNumber));
    }

    private PaymentScheduleResponse toResponse(PaymentSchedule schedule, List<PaymentScheduleInstallment> installments) {
        List<PaymentScheduleInstallmentResponse> responses = installments.stream()
                .map(i -> new PaymentScheduleInstallmentResponse(
                        i.getInstallmentNumber(),
                        i.getInstallmentOrder(),
                        i.getLabel(),
                        i.getAmount(),
                        i.getDueDate(),
                        i.getPaidAmount(),
                        i.getAmount().subtract(i.getPaidAmount()),
                        i.getStatus(),
                        i.getPaidAt()
                ))
                .toList();
        return new PaymentScheduleResponse(
                schedule.getScheduleNumber(),
                schedule.getBillingDocumentNumber(),
                schedule.getStatus(),
                schedule.getTotalAmount(),
                schedule.getPaidAmount(),
                schedule.getTotalAmount().subtract(schedule.getPaidAmount()),
                schedule.getCurrency(),
                schedule.getNotes(),
                responses
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * Lot B9 (E12) : tout paiement sur la facture (direct, par echeance, avoir) se repartit sur les
     * echeances dans l'ordre ; une annulation de paiement les rouvre. Ce qui etait paye avant
     * l'echeancier n'y compte pas.
     */
    @Override
    public void syncWithInvoice(BillingDocument document) {
        scheduleRepository.findActiveByDocumentNumber(document.getDocumentNumber()).ifPresent(schedule -> {
            BigDecimal before = schedule.getPaidBefore() == null ? BigDecimal.ZERO : schedule.getPaidBefore();
            BigDecimal paid = document.getPaidAmount() == null ? BigDecimal.ZERO : document.getPaidAmount();
            BigDecimal covered = paid.subtract(before).max(BigDecimal.ZERO).min(schedule.getTotalAmount());
            BigDecimal left = covered;
            for (PaymentScheduleInstallment installment : installmentRepository.findByScheduleIdOrdered(schedule.getId())) {
                if (installment.getStatus() == PaymentScheduleInstallmentStatus.CANCELLED) {
                    continue;
                }
                BigDecimal share = left.min(installment.getAmount());
                left = left.subtract(share);
                installment.setPaidAmount(share);
                if (share.compareTo(installment.getAmount()) >= 0) {
                    if (installment.getStatus() != PaymentScheduleInstallmentStatus.PAID) {
                        installment.setPaidAt(Instant.now());
                    }
                    installment.setStatus(PaymentScheduleInstallmentStatus.PAID);
                } else {
                    installment.setPaidAt(null);
                    installment.setStatus(share.signum() > 0 ? PaymentScheduleInstallmentStatus.PARTIALLY_PAID
                            : installment.getDueDate() != null && installment.getDueDate().isBefore(java.time.LocalDate.now())
                            ? PaymentScheduleInstallmentStatus.OVERDUE : PaymentScheduleInstallmentStatus.PENDING);
                }
                installmentRepository.save(installment);
            }
            schedule.setPaidAmount(covered);
            schedule.setStatus(covered.compareTo(schedule.getTotalAmount()) >= 0 ? PaymentScheduleStatus.COMPLETED : PaymentScheduleStatus.ACTIVE);
            scheduleRepository.save(schedule);
        });
    }
}
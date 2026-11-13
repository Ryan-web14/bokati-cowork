package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreatePaymentScheduleRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingAgingReportResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.PaymentScheduleInstallmentStatus;
import com.sni.bokaticowork.features.billing.enums.PaymentScheduleStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.PaymentSchedule;
import com.sni.bokaticowork.features.billing.model.PaymentScheduleInstallment;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.PaymentScheduleInstallmentRepository;
import com.sni.bokaticowork.features.billing.repository.PaymentScheduleRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot B9 : echeancier qui suit les paiements dans l'ordre (E12), balance agee par devise (E11). */
class StatementsB9Test {

    @Test
    void shouldSpreadADirectPaymentOverTheInstallmentsInOrder() {
        PaymentScheduleRepository schedules = mock(PaymentScheduleRepository.class);
        PaymentScheduleInstallmentRepository installments = mock(PaymentScheduleInstallmentRepository.class);
        PaymentScheduleServiceImpl service = new PaymentScheduleServiceImpl(schedules, installments, mock(BillingDocumentService.class),
                mock(com.sni.bokaticowork.features.payment.service.interfaces.PaymentService.class),
                mock(com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade.class));
        PaymentSchedule schedule = PaymentSchedule.builder().id(1L).billingDocumentNumber("FAC-1").totalAmount(new BigDecimal("90000"))
                .paidAmount(BigDecimal.ZERO).paidBefore(new BigDecimal("10000")).status(PaymentScheduleStatus.ACTIVE).currency("XAF").build();
        PaymentScheduleInstallment first = installment(1, "30000");
        PaymentScheduleInstallment second = installment(2, "30000");
        PaymentScheduleInstallment third = installment(3, "30000");
        when(schedules.findActiveByDocumentNumber("FAC-1")).thenReturn(Optional.of(schedule));
        when(installments.findByScheduleIdOrdered(1L)).thenReturn(List.of(first, second, third));

        // 10 000 avant l'echeancier, puis 45 000 payes directement sur la facture.
        service.syncWithInvoice(BillingDocument.builder().documentNumber("FAC-1").paidAmount(new BigDecimal("55000")).build());

        assertThat(first.getStatus()).isEqualTo(PaymentScheduleInstallmentStatus.PAID);
        assertThat(second.getStatus()).isEqualTo(PaymentScheduleInstallmentStatus.PARTIALLY_PAID);
        assertThat(second.getPaidAmount()).isEqualByComparingTo("15000");
        assertThat(third.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(schedule.getPaidAmount()).isEqualByComparingTo("45000");

        // Le reste arrive : l'echeancier est complet.
        service.syncWithInvoice(BillingDocument.builder().documentNumber("FAC-1").paidAmount(new BigDecimal("100000")).build());
        assertThat(third.getStatus()).isEqualTo(PaymentScheduleInstallmentStatus.PAID);
        assertThat(schedule.getStatus()).isEqualTo(PaymentScheduleStatus.COMPLETED);
    }

    @Test
    void shouldRefuseAScheduleOnAQuoteOrADraft() {
        BillingDocumentService documents = mock(BillingDocumentService.class);
        PaymentScheduleServiceImpl service = new PaymentScheduleServiceImpl(mock(PaymentScheduleRepository.class),
                mock(PaymentScheduleInstallmentRepository.class), documents,
                mock(com.sni.bokaticowork.features.payment.service.interfaces.PaymentService.class),
                mock(com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade.class));
        when(documents.serviceByNumber("DEV-1")).thenReturn(BillingDocument.builder().documentNumber("DEV-1").documentType(BillingDocumentType.QUOTE)
                .status(BillingDocumentStatus.SENT).balanceDue(BigDecimal.TEN).build());
        when(documents.serviceByNumber("FAC-2")).thenReturn(BillingDocument.builder().documentNumber("FAC-2").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.DRAFT).balanceDue(BigDecimal.TEN).build());
        CreatePaymentScheduleRequest request = mock(CreatePaymentScheduleRequest.class);

        assertThatThrownBy(() -> service.createSchedule("DEV-1", request)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.createSchedule("FAC-2", request)).isInstanceOf(BadRequestException.class).hasMessageContaining("émise");
    }

    @Test
    void shouldKeepOneCurrencyPerAgingReport() {
        BillingDocumentRepository repository = mock(BillingDocumentRepository.class);
        BillingRecoverableServiceImpl service = mock(BillingRecoverableServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "documentRepository", repository);
        when(repository.findForAgingReport(any(), any())).thenReturn(List.of(
                doc("FAC-1", "XAF", "100000"), doc("FAC-2", "EUR", "500"), doc("FAC-3", "XAF", "50000")));

        BillingAgingReportResponse xaf = service.agingReport(null, null, "XAF");
        BillingAgingReportResponse eur = service.agingReport(null, null, "eur");

        assertThat(xaf.summary().grandTotal().totalDue()).isEqualByComparingTo("150000");
        assertThat(eur.summary().grandTotal().totalDue()).isEqualByComparingTo("500");
        assertThat(eur.currency()).isEqualTo("EUR");
    }

    private static PaymentScheduleInstallment installment(int order, String amount) {
        return PaymentScheduleInstallment.builder().installmentOrder(order).amount(new BigDecimal(amount)).paidAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.now().plusDays(30L * order)).status(PaymentScheduleInstallmentStatus.PENDING).build();
    }

    private static BillingDocument doc(String number, String currency, String due) {
        return BillingDocument.builder().documentNumber(number).currency(currency).balanceDue(new BigDecimal(due)).totalAmount(new BigDecimal(due))
                .status(BillingDocumentStatus.ISSUED).dueDate(LocalDate.now().plusDays(5)).build();
    }
}

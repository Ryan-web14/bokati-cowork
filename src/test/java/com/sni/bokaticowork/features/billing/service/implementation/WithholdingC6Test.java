package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.WithholdingDtos;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.Withholding;
import com.sni.bokaticowork.features.billing.model.WithholdingType;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.WithholdingRepository;
import com.sni.bokaticowork.features.billing.repository.WithholdingTypeRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C6 : une facture de 1 180 000 payee 1 000 000 par une administration qui retient la TVA
 * (180 000) est soldee par la retenue ; l'attestation est attendue ; une annulation rouvre le du.
 */
class WithholdingC6Test {

    private final WithholdingRepository withholdings = mock(WithholdingRepository.class);
    private final WithholdingTypeRepository types = mock(WithholdingTypeRepository.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private final BillingDocumentServiceImpl billing = mock(BillingDocumentServiceImpl.class);
    private WithholdingService service;
    private BillingDocument invoice;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<BillingDocumentServiceImpl> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(billing);
        service = new WithholdingService(withholdings, types, documents, sequences, staff, provider);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("caisse@elleaose.com", "Caisse", null));
        when(sequences.next("billing_withholding")).thenReturn("RAS-1");
        when(withholdings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(types.findByCode("TVA_RETENUE")).thenReturn(Optional.of(WithholdingType.builder().code("TVA_RETENUE").label("Retenue de TVA")
                .rate(new BigDecimal("100")).base("TVA").account("4449").active(true).build()));
        when(types.findByCode("PRECOMPTE")).thenReturn(Optional.of(WithholdingType.builder().code("PRECOMPTE").label("Précompte")
                .base("HT").account("4492").active(true).build()));
        invoice = BillingDocument.builder().documentNumber("FAC-31").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.PARTIALLY_PAID).currency("XAF").customerType("CUSTOMER").customerCode("MIN-FIN")
                .totalAmount(new BigDecimal("1180000")).vatAmount(new BigDecimal("180000")).taxAmount(new BigDecimal("180000"))
                .paidAmount(new BigDecimal("1000000")).balanceDue(new BigDecimal("180000")).build();
        when(documents.findByDocumentNumber("FAC-31")).thenReturn(Optional.of(invoice));
    }

    @Test
    void shouldSettleTheInvoiceWithTheVatWithheld() {
        WithholdingDtos.WithholdingView view = service.record("FAC-31", new WithholdingDtos.RecordRequest("TVA_RETENUE", null, null, "ATT-2026-14", null));

        assertThat(view.amount()).isEqualByComparingTo("180000");
        assertThat(view.account()).isEqualTo("4449");
        assertThat(view.certificateMissing()).isTrue();
        verify(billing).applyPayment("FAC-31", new BigDecimal("180000.00"));
    }

    @Test
    void shouldRefuseMoreThanTheBalanceAndAskTheAmountOfATypeWithoutRate() {
        assertThatThrownBy(() -> service.record("FAC-31", new WithholdingDtos.RecordRequest("TVA_RETENUE", new BigDecimal("200000"), null, null, null)))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("RETENUE_MONTANT"));
        assertThatThrownBy(() -> service.record("FAC-31", new WithholdingDtos.RecordRequest("PRECOMPTE", null, null, null, null)))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("RETENUE_MONTANT"));
        verify(billing, never()).applyPayment(any(), any());
    }

    @Test
    void shouldReopenTheBalanceWhenAWithholdingIsCancelled() {
        when(withholdings.findByWithholdingNumber("RAS-1")).thenReturn(Optional.of(Withholding.builder().withholdingNumber("RAS-1")
                .documentNumber("FAC-31").typeCode("TVA_RETENUE").amount(new BigDecimal("180000")).status("RECORDED").build()));

        WithholdingDtos.WithholdingView view = service.cancel("RAS-1", "Saisie en double");

        assertThat(view.status()).isEqualTo("CANCELLED");
        verify(billing).reversePayment(eq("FAC-31"), eq(new BigDecimal("180000")));
    }

    @Test
    void shouldComputeTheBaseFromTheInvoice() {
        assertThat(WithholdingService.base(invoice, "TTC")).isEqualByComparingTo("1180000");
        assertThat(WithholdingService.base(invoice, "HT")).isEqualByComparingTo("1000000");
        assertThat(WithholdingService.base(invoice, "TVA")).isEqualByComparingTo("180000");
    }
}

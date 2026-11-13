package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.ChangeCustomerRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.ChangeCustomerResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingCustomerChange;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingCustomerChangeRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B6 : changement de client, en place avant scellement, sinon avoirs et nouvelle facture (D6). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerChangeB6Test {

    @Mock private BillingDocumentServiceImpl billing;
    @Mock private BillingDocumentRepository documents;
    @Mock private BillingCustomerChangeRepository changes;
    @Mock private BillingEventWriter eventWriter;
    @Mock private StaffProfileService staffProfiles;

    @InjectMocks
    private CustomerChangeService service;

    private BillingDocument invoice;

    @BeforeEach
    void setUp() {
        invoice = BillingDocument.builder().documentNumber("FAC-1").documentType(BillingDocumentType.INVOICE).status(BillingDocumentStatus.ISSUED)
                .customerType("CUSTOMER").customerCode("ORG-A").locked(false).totalAmount(new BigDecimal("118000"))
                .paidAmount(BigDecimal.ZERO).balanceDue(new BigDecimal("118000")).build();
        when(billing.serviceByNumber("FAC-1")).thenReturn(invoice);
        when(staffProfiles.currentAuthor()).thenReturn(new StaffProfileService.Author("awa@elleaose.com", "Awa Mbemba", null));
        when(changes.save(any(BillingCustomerChange.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BillingDocumentResponse created = mock(BillingDocumentResponse.class);
        when(created.documentNumber()).thenReturn("FAC-2");
        when(billing.create(any())).thenReturn(created);
        when(billing.reissueRequest(any(), anyString(), anyString())).thenReturn(mock(CreateBillingDocumentRequest.class));
        when(billing.createCreditNote(eq("FAC-1"), any(CreateCreditNoteRequest.class))).thenAnswer(invocation -> {
            CreateCreditNoteRequest request = invocation.getArgument(1);
            BillingDocumentResponse note = mock(BillingDocumentResponse.class);
            when(note.documentNumber()).thenReturn(request.applyImmediately() ? "AV-RESTE" : "AV-REGL");
            return note;
        });
    }

    @Test
    void shouldChangeAnUnsealedUnpaidInvoiceInPlace() {
        ChangeCustomerResponse response = service.change("FAC-1", new ChangeCustomerRequest("CUSTOMER", "ORG-B", "Erreur de client", null));

        assertThat(response.mode()).isEqualTo("IN_PLACE");
        verify(billing).changeCustomerInPlace(invoice, "CUSTOMER", "ORG-B", "Erreur de client");
        verify(billing, never()).create(any());
    }

    @Test
    void shouldReissueASealedPaidInvoiceAndKeepThePaymentsAsCredit() {
        sealedAndPartlyPaid();

        ChangeCustomerResponse response = service.change("FAC-1", new ChangeCustomerRequest("CUSTOMER", "ORG-B", "Facturer la maison mère", null));

        assertThat(response.mode()).isEqualTo("REISSUE");
        assertThat(response.newInvoice()).isEqualTo("FAC-2");
        assertThat(response.unpaidCreditNote()).isEqualTo("AV-RESTE");
        assertThat(response.paidCreditNote()).isEqualTo("AV-REGL");
        assertThat(response.paymentsMode()).isEqualTo("KEEP_AS_CREDIT");
        verify(billing).validate("FAC-2");
        verify(billing, never()).transferCreditNote(anyString(), anyString(), anyString());
        verify(billing).cancelReplacedInvoice("FAC-1", "FAC-2");
        ArgumentCaptor<CreateCreditNoteRequest> notes = ArgumentCaptor.forClass(CreateCreditNoteRequest.class);
        verify(billing, org.mockito.Mockito.times(2)).createCreditNote(eq("FAC-1"), notes.capture());
        assertThat(notes.getAllValues().get(0).amount()).isEqualByComparingTo("68000");
        assertThat(notes.getAllValues().get(1).amount()).isEqualByComparingTo("50000");
    }

    @Test
    void shouldTransferThePaymentsToTheNewInvoice() {
        sealedAndPartlyPaid();
        when(billing.transferCreditNote("AV-REGL", "FAC-2", "ORG-A")).thenReturn(new BigDecimal("50000"));

        ChangeCustomerResponse response = service.change("FAC-1", new ChangeCustomerRequest("CUSTOMER", "ORG-B", "Facturer la maison mère", "TRANSFER"));

        assertThat(response.paymentsMode()).isEqualTo("TRANSFER");
        assertThat(response.transferredAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void shouldRefuseTheSameCustomer() {
        assertThatThrownBy(() -> service.change("FAC-1", new ChangeCustomerRequest("CUSTOMER", "ORG-A", "x", null)))
                .isInstanceOf(BadRequestException.class);
    }

    private void sealedAndPartlyPaid() {
        invoice.setLocked(true);
        invoice.setStatus(BillingDocumentStatus.PARTIALLY_PAID);
        invoice.setPaidAmount(new BigDecimal("50000"));
        invoice.setBalanceDue(new BigDecimal("68000"));
    }
}

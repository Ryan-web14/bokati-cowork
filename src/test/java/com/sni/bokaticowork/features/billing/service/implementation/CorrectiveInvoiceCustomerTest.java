package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.fiscal.FiscalAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorrectiveInvoiceCustomerTest {

    @Mock
    private BillingDocumentRepository documentRepository;

    @Mock
    private FiscalAuditService fiscalAuditService;

    @Spy
    @InjectMocks
    private BillingDocumentServiceImpl service;

    @Test
    void shouldInheritTheCustomerFromTheInvoiceBeingCorrected() {
        // Regression : la rectificative echouait sur « Customer name is required when customer
        // cannot be resolved » alors que le client est parfaitement determine · c'est celui de
        // la facture corrigee.
        originalExists(invoice());
        ArgumentCaptor<CreateBillingDocumentRequest> captured = captureCreate();

        service.createCorrectiveInvoice("INV-001", manualRequest(null, null));

        CreateBillingDocumentRequest sent = captured.getValue();
        assertThat(sent.customerCode()).isEqualTo("MBR-202607-00000007");
        assertThat(sent.customerName()).isEqualTo("Hope MADHOND");
        assertThat(sent.customerType()).isEqualTo("MEMBER");
        assertThat(sent.customerEmail()).isEqualTo("hope@example.com");
    }

    @Test
    void shouldKeepAnExplicitCustomerOverTheInheritedOne() {
        // Corriger les coordonnees imprimees fait partie des motifs d'emission d'une
        // rectificative · une valeur fournie ne doit pas etre ecrasee.
        originalExists(invoice());
        ArgumentCaptor<CreateBillingDocumentRequest> captured = captureCreate();

        service.createCorrectiveInvoice("INV-001", manualRequest("MBR-AUTRE", "Nom corrige"));

        assertThat(captured.getValue().customerCode()).isEqualTo("MBR-AUTRE");
        assertThat(captured.getValue().customerName()).isEqualTo("Nom corrige");
    }

    @Test
    void shouldStillRefuseAnUnsealedInvoice() {
        BillingDocument draft = invoice();
        draft.setLocked(false);
        originalExists(draft);

        assertThatThrownBy(() -> service.createCorrectiveInvoice("INV-001", manualRequest(null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("validée");
    }

    @Test
    void shouldStillRefuseADocumentThatIsNotAnInvoice() {
        BillingDocument creditNote = invoice();
        creditNote.setDocumentType(BillingDocumentType.CREDIT_NOTE);
        originalExists(creditNote);

        assertThatThrownBy(() -> service.createCorrectiveInvoice("INV-001", manualRequest(null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Seules les factures");
    }

    // =================================================================================

    private ArgumentCaptor<CreateBillingDocumentRequest> captureCreate() {
        ArgumentCaptor<CreateBillingDocumentRequest> captor =
                ArgumentCaptor.forClass(CreateBillingDocumentRequest.class);
        BillingDocumentResponse created = mock(BillingDocumentResponse.class);
        when(created.documentNumber()).thenReturn("REC-001");
        doReturn(created).when(service).create(captor.capture());
        doReturn(created).when(service).get(any());
        return captor;
    }

    private void originalExists(BillingDocument original) {
        when(documentRepository.findByDocumentNumber("INV-001")).thenReturn(Optional.of(original));
        BillingDocument corrective = BillingDocument.builder()
                .documentNumber("REC-001")
                .documentType(BillingDocumentType.CORRECTIVE_INVOICE)
                .build();
        when(documentRepository.findByDocumentNumber("REC-001")).thenReturn(Optional.of(corrective));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(i -> i.getArgument(0));
    }

    private BillingDocument invoice() {
        return BillingDocument.builder()
                .documentNumber("INV-001")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.VALIDATED)
                .locked(true)
                .customerType("MEMBER")
                .customerCode("MBR-202607-00000007")
                .customerName("Hope MADHOND")
                .customerEmail("hope@example.com")
                .customerPhone("065012167")
                .currency("XAF")
                .totalAmount(new BigDecimal("150000"))
                .build();
    }

    /** Charge utile telle que l'interface l'envoie · sans aucune donnee client. */
    private CreateManualBillingDocumentRequest manualRequest(String customerCode, String customerName) {
        return new CreateManualBillingDocumentRequest(
                "MEMBER".equals(customerCode) ? "MEMBER" : (customerCode == null ? null : "MEMBER"),
                customerCode, customerName, null, null, null,
                null, null,
                "Facture rectificative INV-001", null, null,
                "XAF", LocalDate.of(2026, 8, 31), null, null,
                List.of(), List.of(), List.of(),
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}

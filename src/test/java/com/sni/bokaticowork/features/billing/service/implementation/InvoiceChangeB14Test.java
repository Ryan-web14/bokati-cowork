package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.billing.dto.request.RequestInvoiceChangeRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.InvoiceChangeResponse;
import com.sni.bokaticowork.features.billing.enums.BillingChangeRequestStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentChangeRequest;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentChangeRequestRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot B14 : une facture emise non payee se modifie apres approbation ; a l'accord, nouvelle version
 * de la meme piece, ou rectificative si elle est scellee ou comptabilisee (D8). Rien n'est supprime.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceChangeB14Test {

    @Mock private BillingDocumentRepository documents;
    @Mock private BillingDocumentChangeRequestRepository changes;
    @Mock private BillingDocumentServiceImpl billing;
    @Mock private ApprovalEngine approvals;
    @Mock private StaffProfileService staffProfiles;

    @InjectMocks
    private InvoiceChangeService service;

    private BillingDocument invoice;
    private final StaffProfileService.Author awa = new StaffProfileService.Author("awa@elleaose.com", "Awa Mbemba", "Commerciale");

    @BeforeEach
    void setUp() {
        invoice = BillingDocument.builder().id(7L).documentNumber("INV-7").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED).locked(false).paidAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("118000"))
                .currency("XAF").build();
        when(billing.serviceByNumber("INV-7")).thenReturn(invoice);
        when(billing.projectedTotal(any(), any())).thenReturn(new BigDecimal("94400"));
        when(staffProfiles.currentAuthor()).thenReturn(awa);
        when(staffProfiles.authorOf("awa@elleaose.com")).thenReturn(awa);
        when(changes.save(any(BillingDocumentChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(changes.findByDocumentNumberAndStatus(anyString(), any())).thenReturn(List.of());
        when(approvals.submit(any())).thenReturn(ApprovalRequest.builder().number("APR-1").status(ApprovalStatus.PENDING).build());
    }

    @Test
    void shouldWaitForApprovalAndKeepTheCurrentVersion() {
        InvoiceChangeResponse response = service.request("INV-7", new RequestInvoiceChangeRequest("Remise accordée", null, changes()));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.approvalNumber()).isEqualTo("APR-1");
        assertThat(response.totalBefore()).isEqualByComparingTo("118000");
        assertThat(response.totalAfter()).isEqualByComparingTo("94400");
        ArgumentCaptor<ApprovalDtos.Submission> submission = ArgumentCaptor.forClass(ApprovalDtos.Submission.class);
        verify(approvals).submit(submission.capture());
        assertThat(submission.getValue().center()).isEqualTo("BILLING");
        assertThat(submission.getValue().requestType()).isEqualTo("INVOICE_CHANGE");
        assertThat(submission.getValue().amount()).isEqualByComparingTo("94400");
        verify(billing, never()).update(anyString(), any());
        verify(billing, never()).applyInvoiceVersion(any(), any(), any(), any(), any());
    }

    @Test
    void shouldChangeADraftAtOnce() {
        invoice.setStatus(BillingDocumentStatus.DRAFT);

        InvoiceChangeResponse response = service.request("INV-7", new RequestInvoiceChangeRequest("Correction", null, changes()));

        assertThat(response.status()).isEqualTo("APPLIED");
        verify(billing).update(eq("INV-7"), any());
        verify(approvals, never()).submit(any());
    }

    @Test
    void shouldRefuseAPaidInvoice() {
        invoice.setPaidAmount(new BigDecimal("1000"));
        invoice.setStatus(BillingDocumentStatus.PARTIALLY_PAID);

        assertThatThrownBy(() -> service.request("INV-7", new RequestInvoiceChangeRequest("x", null, changes())))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("avoir");
    }

    @Test
    void shouldApplyANewVersionOnceApprovedWhenTheInvoiceIsNeitherSealedNorPosted() {
        BillingDocumentChangeRequest change = pending();
        when(billing.applyInvoiceVersion(eq(invoice), any(), eq("Remise accordée"), any(), eq(awa))).thenReturn(invoice);
        when(billing.currentRevision(invoice)).thenReturn(2);

        service.onApproved(approval(), outcome(true));

        assertThat(change.getStatus()).isEqualTo(BillingChangeRequestStatus.APPLIED);
        assertThat(change.getResult()).isEqualTo("NEW_VERSION");
        assertThat(change.getVersionNumber()).isEqualTo(2);
        assertThat(change.getDecidedBy()).isEqualTo("dg@elleaose.com");
    }

    @Test
    void shouldReplaceASealedInvoiceByACorrectiveOnceApproved() {
        invoice.setLocked(true);
        BillingDocumentChangeRequest change = pending();
        when(billing.replaceWithCorrective(eq(invoice), any(), eq("Remise accordée"), any(), eq(awa)))
                .thenReturn(BillingDocument.builder().documentNumber("COR-1").build());

        service.onApproved(approval(), outcome(true));

        assertThat(change.getResult()).isEqualTo("CORRECTIVE_INVOICE");
        assertThat(change.getResultDocumentNumber()).isEqualTo("COR-1");
    }

    @Test
    void shouldAlsoUseACorrectiveWhenTheInvoiceIsAlreadyPosted() {
        when(documents.salePosted("INV-7")).thenReturn(true);
        BillingDocumentChangeRequest change = pending();
        when(billing.replaceWithCorrective(any(), any(), any(), any(), any())).thenReturn(BillingDocument.builder().documentNumber("COR-2").build());

        service.onApproved(approval(), outcome(true));

        assertThat(change.getResult()).isEqualTo("CORRECTIVE_INVOICE");
    }

    @Test
    void shouldWaitForTheLastLevelAndRecordARejection() {
        BillingDocumentChangeRequest change = pending();

        service.onApproved(approval(), outcome(false));
        assertThat(change.getStatus()).isEqualTo(BillingChangeRequestStatus.PENDING);

        service.onClosed(approval(), outcome(false), BillingChangeRequestStatus.REJECTED);
        assertThat(change.getStatus()).isEqualTo(BillingChangeRequestStatus.REJECTED);
    }

    @Test
    void shouldKeepTheProposedChangesIntactUntilTheDecision() {
        UpdateBillingDocumentRequest proposed = new UpdateBillingDocumentRequest(null, null, "A 30 jours", null, LocalDate.of(2026, 12, 31), null,
                null, null, null, null, null, null, null, null, null, null,
                List.of(new UpdateBillingDocumentLineRequest("UPSERT", 1, BillingLineType.SERVICE, null, "Bureau", null, BigDecimal.ONE,
                        new BigDecimal("80000"), null, null, true, null, null, null, null, null, null, null, null, false, null)), null, null);

        UpdateBillingDocumentRequest read = InvoiceChangeService.read(InvoiceChangeService.write(proposed));

        assertThat(read).isEqualTo(proposed);
    }

    private BillingDocumentChangeRequest pending() {
        BillingDocumentChangeRequest change = BillingDocumentChangeRequest.builder().id(1L).documentId(7L).documentNumber("INV-7")
                .status(BillingChangeRequestStatus.PENDING).reason("Remise accordée").changesJson(InvoiceChangeService.write(changes()))
                .requestedBy("awa@elleaose.com").approvalNumber("APR-1").build();
        when(changes.findFirstByApprovalNumber("APR-1")).thenReturn(Optional.of(change));
        return change;
    }

    private static ApprovalRequest approval() {
        return ApprovalRequest.builder().number("APR-1").subjectRef("INV-7").build();
    }

    private static ApprovalDtos.Outcome outcome(boolean last) {
        return new ApprovalDtos.Outcome("APR-1", "INV-7", last ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING, 1, 1, last,
                "dg@elleaose.com", null, null, null, "D'accord");
    }

    private static UpdateBillingDocumentRequest changes() {
        return new UpdateBillingDocumentRequest(null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }
}

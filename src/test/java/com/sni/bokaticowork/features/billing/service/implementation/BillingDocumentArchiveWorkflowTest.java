package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingDocumentArchiveWorkflowTest {

    @Mock
    private BillingDocumentRepository documentRepository;

    @Mock
    private BillingDocumentMapper mapper;

    @Mock
    private BillingEventWriter eventWriter;

    @Spy
    @InjectMocks
    private BillingDocumentServiceImpl service;

    @Test
    void shouldCancelAndArchiveInvoice() {
        BillingDocument invoice = BillingDocument.builder()
                .documentNumber("INV-ARCH-001")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .customerName("Jean")
                .currency("XAF")
                .totalAmount(new BigDecimal("100.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("100.0000"))
                .issueDate(LocalDate.of(2026, 4, 27))
                .build();

        when(documentRepository.findByDocumentNumber("INV-ARCH-001")).thenReturn(Optional.of(invoice));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillingDocument archived = service.cancelAndArchive("INV-ARCH-001", "Annulation automatique");

        assertEquals(BillingDocumentStatus.CANCELLED, archived.getStatus());
        assertEquals(BigDecimal.ZERO, archived.getBalanceDue());
        assertNotNull(archived.getCancelledAt());
        assertNotNull(archived.getArchivedAt());
        assertNull(archived.getPaidAt());
        verify(documentRepository, times(2)).save(any(BillingDocument.class));
        verify(eventWriter).write(eq(archived), eq("BILLING_DOCUMENT_CANCELLED"), any());
        verify(eventWriter).write(eq(archived), eq("BILLING_DOCUMENT_ARCHIVED"), any());
    }

    @Test
    void shouldLeaveASealedInvoiceCancelledAndNotPaidOnceTheCreditNoteIsApplied() {
        // Regression : l'avoir d'annulation est applique via applyPayment, qui solde la facture
        // et la laisse en PAID. Une facture annulee ressortait donc comme encaissee et restait
        // comptee dans le chiffre d'affaires, les agregats comptables excluant CANCELLED et
        // VOIDED mais pas PAID.
        //
        // La facture est ici partiellement encaissee : depuis la revision des regles
        // d'annulation, c'est l'encaissement qui declenche l'emission d'un avoir. Une facture
        // scellee dont rien n'a ete percu s'annule directement.
        BillingDocument sealed = BillingDocument.builder()
                .documentNumber("INV-SEAL-001")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.PARTIALLY_PAID)
                .locked(true)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .customerName("Jean")
                .currency("XAF")
                .totalAmount(new BigDecimal("100.0000"))
                .paidAmount(new BigDecimal("40.0000"))
                .balanceDue(new BigDecimal("60.0000"))
                .issueDate(LocalDate.of(2026, 4, 27))
                .build();

        when(documentRepository.findByDocumentNumber("INV-SEAL-001")).thenReturn(Optional.of(sealed));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // L'emission de l'avoir a son propre parcours · on simule uniquement son effet de bord,
        // qui est de solder la facture.
        doAnswer(invocation -> {
            sealed.setPaidAmount(sealed.getTotalAmount());
            sealed.setBalanceDue(BigDecimal.ZERO);
            sealed.setStatus(BillingDocumentStatus.PAID);
            sealed.setPaidAt(Instant.now());
            return null;
        }).when(service).createCreditNote(eq("INV-SEAL-001"), any());

        BillingDocument cancelled = service.cancelAndArchive("INV-SEAL-001", "Erreur de saisie");

        assertEquals(BillingDocumentStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
        assertNotNull(cancelled.getArchivedAt());
        assertEquals(0, cancelled.getBalanceDue().compareTo(BigDecimal.ZERO));
        // Les montants fiscaux restent intacts · c'est le statut qui sort le document de la compta.
        assertEquals(0, cancelled.getTotalAmount().compareTo(new BigDecimal("100.0000")));
        verify(eventWriter).write(eq(cancelled), eq("BILLING_DOCUMENT_CANCELLED_VIA_CREDIT_NOTE"), any());
    }

    @Test
    void shouldCancelASealedInvoiceThatNeedsNoCreditNote() {
        // Facture scellee a montant nul : aucun avoir n'est emis, mais elle doit tout de meme
        // ressortir annulee. Auparavant cette branche se contentait d'archiver, en laissant le
        // statut inchange.
        BillingDocument sealed = BillingDocument.builder()
                .documentNumber("INV-SEAL-002")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED)
                .locked(true)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .customerName("Jean")
                .currency("XAF")
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(BigDecimal.ZERO)
                .issueDate(LocalDate.of(2026, 4, 27))
                .build();

        when(documentRepository.findByDocumentNumber("INV-SEAL-002")).thenReturn(Optional.of(sealed));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillingDocument cancelled = service.cancelAndArchive("INV-SEAL-002", null);

        assertEquals(BillingDocumentStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getArchivedAt());
        verify(service, never()).createCreditNote(any(), any());
    }
}

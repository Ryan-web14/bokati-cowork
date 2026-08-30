package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingCancellationRulesTest {

    @Mock
    private BillingDocumentRepository documentRepository;

    @Mock
    private WalletLedgerEntryRepository walletLedgerEntryRepository;

    @Mock
    private BillingEventWriter eventWriter;

    @Mock
    private BillingDocumentMapper mapper;

    @Mock
    private EntityManager entityManager;

    @Spy
    @InjectMocks
    private BillingDocumentServiceImpl service;

    // =================================================================================
    // Un avoir n'est emis que si de l'argent a ete encaisse
    // =================================================================================

    @Test
    void shouldCancelASealedInvoiceDirectlyWhenNothingWasCollected() {
        // Regression : toute facture scellee declenchait un avoir, meme sans un franc encaisse.
        // Un avoir constate une creance en faveur du client · sans encaissement, il n'y a rien
        // a crediter et l'annulation directe suffit.
        BillingDocument invoice = invoice("INV-001", "100000", "0", true);
        exists(invoice);

        BillingDocument cancelled = service.cancelAndArchive("INV-001", "Erreur de saisie");

        assertThat(cancelled.getStatus()).isEqualTo(BillingDocumentStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getArchivedAt()).isNotNull();
        verify(service, never()).createCreditNote(anyString(), any());
    }

    @Test
    void shouldStillEmitACreditNoteWhenMoneyWasCollected() {
        BillingDocument invoice = invoice("INV-002", "100000", "40000", true);
        exists(invoice);
        // L'emission de l'avoir a son propre parcours · seul l'aiguillage est verifie ici.
        doReturn(null).when(service).createCreditNote(anyString(), any());

        service.cancelAndArchive("INV-002", "Prestation annulee");

        verify(service).createCreditNote(eq("INV-002"), any());
    }

    @Test
    void shouldCancelAnUnsealedInvoiceDirectlyEvenWhenPartlyPaid() {
        // Un document non scelle n'a pas de numero fiscal definitif · rien n'impose l'avoir.
        BillingDocument invoice = invoice("INV-003", "100000", "40000", false);
        exists(invoice);

        assertThat(service.cancelAndArchive("INV-003", null).getStatus())
                .isEqualTo(BillingDocumentStatus.CANCELLED);
        verify(service, never()).createCreditNote(anyString(), any());
    }

    @Test
    void shouldCancelAQuoteDirectly() {
        BillingDocument quote = invoice("QUO-001", "100000", "0", true);
        quote.setDocumentType(BillingDocumentType.QUOTE);
        exists(quote);

        assertThat(service.cancelAndArchive("QUO-001", null).getStatus())
                .isEqualTo(BillingDocumentStatus.CANCELLED);
        verify(service, never()).createCreditNote(anyString(), any());
    }

    // =================================================================================
    // Un avoir doit pouvoir etre annule
    // =================================================================================

    @Test
    void shouldCancelAnUnusedCreditNoteWithoutEmittingAnotherOne() {
        // Regression : annuler un avoir tentait d'emettre un avoir d'avoir, et echouait sur
        // « Payments can only be allocated to invoices » venu de ensureCanPay.
        BillingDocument creditNote = creditNote("CRN-001", "50000", BillingDocumentStatus.VALIDATED, null);
        exists(creditNote);

        BillingDocument cancelled = service.cancelAndArchive("CRN-001", "Emis par erreur");

        assertThat(cancelled.getStatus()).isEqualTo(BillingDocumentStatus.CANCELLED);
        verify(service, never()).createCreditNote(anyString(), any());
    }

    @Test
    void shouldReverseTheImputationWhenCancellingAConsumedCreditNote() {
        // L'avoir avait solde une facture · l'annuler sans defaire l'imputation laisserait la
        // facture creditee par un document annule.
        BillingDocument creditNote = creditNote("CRN-002", "50000", BillingDocumentStatus.ISSUED, "INV-010");
        exists(creditNote);
        BillingDocument target = invoice("INV-010", "100000", "50000", true);
        when(documentRepository.findByDocumentNumber("INV-010")).thenReturn(Optional.of(target));
        doReturn(target).when(service).reversePayment(anyString(), any());

        service.cancelAndArchive("CRN-002", "Avoir errone");

        assertThat(creditNote.getStatus()).isEqualTo(BillingDocumentStatus.CANCELLED);
        verify(service).reversePayment("INV-010", new BigDecimal("50000"));
    }

    @Test
    void shouldNotReviveAnInvoiceThatIsItselfCancelled() {
        // C'est le cas rencontre en production : la facture cible avait deja ete annulee, d'ou
        // le refus « cannot be paid in status CANCELLED » a l'imputation.
        BillingDocument creditNote = creditNote("CRN-003", "50000", BillingDocumentStatus.ISSUED, "INV-011");
        exists(creditNote);
        BillingDocument target = invoice("INV-011", "100000", "50000", true);
        target.setStatus(BillingDocumentStatus.CANCELLED);
        when(documentRepository.findByDocumentNumber("INV-011")).thenReturn(Optional.of(target));

        service.cancelAndArchive("CRN-003", null);

        assertThat(creditNote.getStatus()).isEqualTo(BillingDocumentStatus.CANCELLED);
        assertThat(target.getPaidAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void shouldRefuseToCancelACreditNoteAlreadyPaidIntoAWallet() {
        // Le solde a pu etre depense depuis · on ne le reprend pas silencieusement.
        BillingDocument creditNote = creditNote("CRN-004", "50000", BillingDocumentStatus.ISSUED, null);
        exists(creditNote);
        when(walletLedgerEntryRepository.findByIdempotencyKey("CREDIT_NOTE_REFUND:CRN-004"))
                .thenReturn(Optional.of(new WalletLedgerEntry()));

        assertThatThrownBy(() -> service.cancelAndArchive("CRN-004", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reverse au portefeuille");
    }

    // =================================================================================

    private void exists(BillingDocument document) {
        when(documentRepository.findByDocumentNumber(document.getDocumentNumber()))
                .thenReturn(Optional.of(document));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(i -> i.getArgument(0));
    }

    private BillingDocument invoice(String number, String total, String paid, boolean locked) {
        BigDecimal totalAmount = new BigDecimal(total);
        BigDecimal paidAmount = new BigDecimal(paid);
        return BillingDocument.builder()
                .documentNumber(number)
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED)
                .locked(locked)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .currency("XAF")
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .balanceDue(totalAmount.subtract(paidAmount))
                .build();
    }

    private BillingDocument creditNote(String number, String amount, BillingDocumentStatus status,
                                       String sourceCode) {
        BillingDocument creditNote = BillingDocument.builder()
                .documentNumber(number)
                .documentType(BillingDocumentType.CREDIT_NOTE)
                .status(status)
                .locked(true)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .currency("XAF")
                .totalAmount(new BigDecimal(amount))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(BigDecimal.ZERO)
                .build();
        if (sourceCode != null) {
            creditNote.setSourceType("INVOICE");
            creditNote.setSourceCode(sourceCode);
        }
        return creditNote;
    }
}

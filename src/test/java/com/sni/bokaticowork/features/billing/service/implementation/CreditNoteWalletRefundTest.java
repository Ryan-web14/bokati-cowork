package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.fiscal.FiscalAuditService;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditNoteWalletRefundTest {

    @Mock
    private BillingDocumentRepository documentRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private FiscalAuditService fiscalAuditService;

    @Mock
    private BillingEventWriter eventWriter;

    @Mock
    private BillingDocumentMapper mapper;

    @InjectMocks
    private BillingDocumentServiceImpl service;

    @Test
    void shouldCreditTheWalletWithAStableIdempotencyKey() {
        creditNoteExists(creditNote("CRN-001", "50000", true, BillingDocumentStatus.VALIDATED));
        walletResolvesTo("WAL-001");

        service.refundCreditNoteToWallet("CRN-001", "  Client sans facture ouverte  ");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(walletService).credit(any(WalletAccount.class), eq(new BigDecimal("50000")),
                eq(WalletEntryType.REFUND), eq("CREDIT_NOTE"), eq("CRN-001"),
                eq("Client sans facture ouverte"), anyString(), key.capture());

        // La cle derive du numero d'avoir · un second appel retrouvera l'ecriture d'origine
        // au lieu de recrediter, ce qui rend un double clic inoffensif.
        assertThat(key.getValue()).isEqualTo("CREDIT_NOTE_REFUND:CRN-001");
    }

    @Test
    void shouldMarkTheCreditNoteConsumed() {
        BillingDocument creditNote = creditNote("CRN-002", "50000", true, BillingDocumentStatus.VALIDATED);
        creditNoteExists(creditNote);
        walletResolvesTo("WAL-002");

        service.refundCreditNoteToWallet("CRN-002", null);

        assertThat(creditNote.getStatus()).isEqualTo(BillingDocumentStatus.ISSUED);
        verify(documentRepository).save(creditNote);
    }

    @Test
    void shouldRefuseADocumentThatIsNotACreditNote() {
        BillingDocument invoice = creditNote("INV-001", "50000", true, BillingDocumentStatus.VALIDATED);
        invoice.setDocumentType(BillingDocumentType.INVOICE);
        creditNoteExists(invoice);

        assertThatThrownBy(() -> service.refundCreditNoteToWallet("INV-001", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est pas un avoir");
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRefuseAnUnsealedCreditNote() {
        creditNoteExists(creditNote("CRN-003", "50000", false, BillingDocumentStatus.DRAFT));

        assertThatThrownBy(() -> service.refundCreditNoteToWallet("CRN-003", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doit etre valide");
        verify(walletService, never()).credit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRefuseACreditNoteAlreadyConsumed() {
        creditNoteExists(creditNote("CRN-004", "50000", true, BillingDocumentStatus.ISSUED));

        assertThatThrownBy(() -> service.refundCreditNoteToWallet("CRN-004", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deja ete consomme");
    }

    @Test
    void shouldRefuseAZeroAmount() {
        creditNoteExists(creditNote("CRN-005", "0", true, BillingDocumentStatus.VALIDATED));

        assertThatThrownBy(() -> service.refundCreditNoteToWallet("CRN-005", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("montant nul");
    }

    // =================================================================================

    private void creditNoteExists(BillingDocument document) {
        when(documentRepository.findByDocumentNumber(document.getDocumentNumber()))
                .thenReturn(Optional.of(document));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void walletResolvesTo(String walletNumber) {
        WalletResponse response = org.mockito.Mockito.mock(WalletResponse.class);
        when(response.walletNumber()).thenReturn(walletNumber);
        when(walletService.getOrCreate(anyString(), anyString(), anyString())).thenReturn(response);
        when(walletService.serviceWallet(walletNumber))
                .thenReturn(WalletAccount.builder().walletNumber(walletNumber).currency("XAF").build());
    }

    private BillingDocument creditNote(String number, String amount, boolean locked,
                                       BillingDocumentStatus status) {
        return BillingDocument.builder()
                .documentNumber(number)
                .documentType(BillingDocumentType.CREDIT_NOTE)
                .status(status)
                .locked(locked)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .currency("XAF")
                .totalAmount(new BigDecimal(amount))
                .build();
    }
}

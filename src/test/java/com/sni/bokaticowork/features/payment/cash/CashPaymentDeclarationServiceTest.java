package com.sni.bokaticowork.features.payment.cash;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.payment.cash.dto.request.ConfirmCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.request.DeclareCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;
import com.sni.bokaticowork.features.payment.cash.repository.CashPaymentDeclarationRepository;
import com.sni.bokaticowork.features.payment.cash.service.CashDeclarationNotifier;
import com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Annoncer n'est pas payer · c'est toute la question.
 *
 * <p>Le mobile money et le portefeuille aboutissent seuls. Les especes arrivent avec la personne :
 * entre l'annonce et les billets comptes, il n'y a rien d'encaisse, et la facture reste due.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashPaymentDeclarationServiceTest {

    @Mock private CashPaymentDeclarationRepository repository;
    @Mock private BillingDocumentService billingDocumentService;
    @Mock private BillingDocumentRepository billingDocumentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentService paymentService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashDeclarationNotifier notifier;

    private CashPaymentDeclarationService service;

    @BeforeEach
    void setUp() {
        service = new CashPaymentDeclarationService(repository, billingDocumentService, billingDocumentRepository,
                bookingRepository, paymentService, sequenceGenerator, notifier);
        ReflectionTestUtils.setField(service, "validityHours", 24);
        when(sequenceGenerator.next(anyString())).thenReturn("ESP-2026-000001");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Une facture reelle · un enregistrement ne se simule pas, et ce sont justement son type et
     * son etat qui decident de tout ici.
     */
    private BillingDocumentResponse invoice(BillingDocumentStatus status, BigDecimal balanceDue, String currency) {
        return new BillingDocumentResponse(
                "INV-1", // documentNumber
                BillingDocumentType.INVOICE, // documentType
                status, // status
                "MEMBER", // customerType
                "MBR-1", // customerCode
                "Joël Bikindou", // customerName
                null, // customerEmail
                null, // customerPhone
                null, // billingAddressJson
                null, // customerRegistered
                "BILLABLE_ITEM", // sourceType
                "BIL-1", // sourceCode
                null, // resolvedSourceType
                null, // resolvedSourceCode
                null, // resolvedSourceLabel
                null, // resolvedSourceRegistered
                null, // title
                null, // description
                null, // terms
                currency, // currency
                null, // subtotalAmount
                null, // discountAmount
                null, // taxableAmount
                null, // vatAmount
                null, // additionalCentAmount
                null, // taxAmount
                balanceDue, // totalAmount
                null, // paidAmount
                balanceDue, // balanceDue
                null, // optionsTotal
                null, // issueDate
                null, // dueDate
                null, // issuedAt
                null, // sentAt
                null, // paidAt
                null, // customerReference
                null, // poNumber
                null, // projectCode
                null, // salespersonCode
                null, // deliveryAddressJson
                null, // language
                null, // exchangeRate
                null, // paymentReference
                null, // paymentInstructions
                null, // bankDetailsJson
                null, // metadataJson
                List.of(), // lines
                List.of(), // discounts
                List.of(), // taxes
                List.of(), // clauses
                null, // advance
                null, // earlyPaymentDiscount
                null, // signature
                null, // internalNotes
                List.of(), // recoverables
                null, // locked
                null, // fiscalNumber
                null, // fiscalDate
                null, // validatedAt
                null, // sellerName
                null, // sellerNiu
                null, // sellerPhone
                null, // sellerEmail
                null, // customerNiu
                null, // customerCategory
                null, // previousHash
                null, // currentHash
                null, // fiscalSignature
                null, // signedAt
                null, // originalDocumentNumber
                null, // originalDocumentType
                null, // creditNoteReason
                BillingReceivables.receivable(BillingDocumentType.INVOICE, status), // receivable
                BillingReceivables.customerImpact(BillingDocumentType.INVOICE, status, balanceDue, balanceDue) // customerImpact
        );
    }

    private CashPaymentDeclaration declaration(CashDeclarationStatus status) {
        return CashPaymentDeclaration.builder()
                .declarationNumber("ESP-2026-000001")
                .documentNumber("INV-1")
                .customerType("MEMBER")
                .customerCode("MBR-1")
                .customerName("Joël Bikindou")
                .amount(new BigDecimal("25000"))
                .currency("XAF")
                .status(status)
                .declaredAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    // -----------------------------------------------------------------------------------------
    // Annonce
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sans montant, on annonce le solde de la facture")
    void declaresTheWholeBalanceByDefault() {
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "XAF"));

        CashPaymentDeclarationResponse response = service.declare("INV-1", null, "MBR-1");

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(response.status()).isEqualTo(CashDeclarationStatus.AWAITING_CONFIRMATION);
        assertThat(response.declarationNumber()).isEqualTo("ESP-2026-000001");
        verify(notifier).declared(any());
    }

    @Test
    @DisplayName("La reservation derriere la facture est retenue · c'est elle qu'on rendra")
    void remembersTheBookingBehindTheInvoice() {
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "XAF"));
        var booking = new com.sni.bokaticowork.features.booking.model.Booking();
        booking.setBookingNumber("BKG-1");
        when(bookingRepository.findByBillableNumber("BIL-1")).thenReturn(Optional.of(booking));

        assertThat(service.declare("INV-1", null, "MBR-1").bookingNumber()).isEqualTo("BKG-1");
    }

    @Test
    @DisplayName("Une seconde annonce sur la meme facture rend la premiere · sinon le client paie deux fois")
    void neverOpensTwoDeclarationsOnTheSameInvoice() {
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "XAF"));
        CashPaymentDeclaration existing = declaration(CashDeclarationStatus.AWAITING_CONFIRMATION);
        when(repository.findFirstByDocumentNumberAndStatusOrderByDeclaredAtDesc("INV-1",
                CashDeclarationStatus.AWAITING_CONFIRMATION)).thenReturn(Optional.of(existing));

        assertThat(service.declare("INV-1", null, "MBR-1").declarationNumber()).isEqualTo("ESP-2026-000001");

        verify(repository, never()).save(any());
        verify(notifier, never()).declared(any());
    }

    @Test
    @DisplayName("On ne peut pas annoncer plus que le solde · le client serait devant le guichet")
    void refusesMoreThanTheBalance() {
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "XAF"));

        assertThatThrownBy(() -> service.declare("INV-1",
                new DeclareCashPaymentRequest(new BigDecimal("30000"), null), "MBR-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dépasse le solde");
    }

    @Test
    @DisplayName("Une facture en brouillon ne s'annonce pas · elle n'a jamais ete emise")
    void refusesADraftInvoice() {
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.DRAFT, new BigDecimal("25000"), "XAF"));

        assertThatThrownBy(() -> service.declare("INV-1", null, "MBR-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est pas réglable");
    }

    @Test
    @DisplayName("Une facture soldee ne s'annonce pas")
    void refusesASettledInvoice() {
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, BigDecimal.ZERO, "XAF"));

        assertThatThrownBy(() -> service.declare("INV-1", null, "MBR-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà soldée");
    }

    @Test
    @DisplayName("La caisse ne tient que du XAF · annoncer autre chose ne pourrait pas etre encaisse")
    void refusesAForeignCurrency() {
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "EUR"));

        assertThatThrownBy(() -> service.declare("INV-1", null, "MBR-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("XAF");
    }

    // -----------------------------------------------------------------------------------------
    // Confirmation
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Confirmer enregistre un paiement en especes adosse a la session de caisse")
    void confirmationRegistersACashPayment() {
        CashPaymentDeclaration pending = declaration(CashDeclarationStatus.AWAITING_CONFIRMATION);
        when(repository.findByDeclarationNumber("ESP-2026-000001")).thenReturn(Optional.of(pending));
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "XAF"));
        when(paymentService.createIntentFromBillingDocument(any()))
                .thenReturn(org.mockito.Mockito.mock(PaymentIntentResponse.class));
        PaymentTransactionResponse transaction = org.mockito.Mockito.mock(PaymentTransactionResponse.class);
        when(transaction.transactionNumber()).thenReturn("TXN-1");
        when(paymentService.registerCashPayment(any(), any())).thenReturn(transaction);

        CashPaymentDeclarationResponse response = service.confirm("ESP-2026-000001",
                new ConfirmCashPaymentRequest("CSH-1", null, "caissier", null), "caisse@elleaose.com");

        assertThat(response.status()).isEqualTo(CashDeclarationStatus.CONFIRMED);
        assertThat(response.confirmedAmount()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(response.transactionNumber()).isEqualTo("TXN-1");

        ArgumentCaptor<RegisterCashPaymentRequest> captor = ArgumentCaptor.forClass(RegisterCashPaymentRequest.class);
        verify(paymentService).registerCashPayment(any(), captor.capture());
        assertThat(captor.getValue().cashSessionNumber()).isEqualTo("CSH-1");
        assertThat(captor.getValue().receivedBy()).isEqualTo("caissier");
        verify(notifier).confirmed(any());
    }

    @Test
    @DisplayName("La caisse encaisse ce qu'elle compte · pas forcement ce qui etait annonce")
    void confirmsThePartialAmountActuallyCounted() {
        CashPaymentDeclaration pending = declaration(CashDeclarationStatus.AWAITING_CONFIRMATION);
        when(repository.findByDeclarationNumber("ESP-2026-000001")).thenReturn(Optional.of(pending));
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "XAF"));
        when(paymentService.createIntentFromBillingDocument(any()))
                .thenReturn(org.mockito.Mockito.mock(PaymentIntentResponse.class));
        when(paymentService.registerCashPayment(any(), any()))
                .thenReturn(org.mockito.Mockito.mock(PaymentTransactionResponse.class));

        CashPaymentDeclarationResponse response = service.confirm("ESP-2026-000001",
                new ConfirmCashPaymentRequest("CSH-1", new BigDecimal("10000"), null, null), "caisse@elleaose.com");

        assertThat(response.confirmedAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("Une facture reglee entre-temps n'est pas encaissee deux fois · l'annonce est close")
    void neverCollectsTwiceOnASettledInvoice() {
        CashPaymentDeclaration pending = declaration(CashDeclarationStatus.AWAITING_CONFIRMATION);
        when(repository.findByDeclarationNumber("ESP-2026-000001")).thenReturn(Optional.of(pending));
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.PAID, BigDecimal.ZERO, "XAF"));

        assertThatThrownBy(() -> service.confirm("ESP-2026-000001",
                new ConfirmCashPaymentRequest("CSH-1", null, null, null), "caisse@elleaose.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("réglée entre-temps");

        verify(paymentService, never()).registerCashPayment(any(), any());
        assertThat(pending.getStatus()).isEqualTo(CashDeclarationStatus.CANCELLED);
    }

    @Test
    @DisplayName("On ne confirme pas plus que le solde")
    void refusesToCollectMoreThanTheBalance() {
        CashPaymentDeclaration pending = declaration(CashDeclarationStatus.AWAITING_CONFIRMATION);
        when(repository.findByDeclarationNumber("ESP-2026-000001")).thenReturn(Optional.of(pending));
        when(billingDocumentService.get("INV-1")).thenReturn(invoice(BillingDocumentStatus.SENT, new BigDecimal("25000"), "XAF"));

        assertThatThrownBy(() -> service.confirm("ESP-2026-000001",
                new ConfirmCashPaymentRequest("CSH-1", new BigDecimal("40000"), null, null), "caisse@elleaose.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dépasse le solde");
    }

    @Test
    @DisplayName("Une annonce deja close ne se confirme plus")
    void refusesToConfirmAClosedDeclaration() {
        when(repository.findByDeclarationNumber("ESP-2026-000001"))
                .thenReturn(Optional.of(declaration(CashDeclarationStatus.EXPIRED)));

        assertThatThrownBy(() -> service.confirm("ESP-2026-000001",
                new ConfirmCashPaymentRequest("CSH-1", null, null, null), "caisse@elleaose.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expirée");
    }

    // -----------------------------------------------------------------------------------------
    // Abandon et propriete
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Annuler ne touche pas la facture · elle reste due, simplement plus annoncee")
    void cancellingLeavesTheInvoiceDue() {
        CashPaymentDeclaration pending = declaration(CashDeclarationStatus.AWAITING_CONFIRMATION);
        when(repository.findByDeclarationNumber("ESP-2026-000001")).thenReturn(Optional.of(pending));

        CashPaymentDeclarationResponse response = service.cancel("ESP-2026-000001", "Changement d'avis", "MBR-1");

        assertThat(response.status()).isEqualTo(CashDeclarationStatus.CANCELLED);
        verify(paymentService, never()).registerCashPayment(any(), any());
        verify(notifier).cancelled(any());
    }

    @Test
    @DisplayName("L'annonce d'un autre client n'existe pas · on ne dit pas qu'elle existe ailleurs")
    void anotherCustomersDeclarationSimplyDoesNotExist() {
        when(repository.findByDeclarationNumber("ESP-2026-000001"))
                .thenReturn(Optional.of(declaration(CashDeclarationStatus.AWAITING_CONFIRMATION)));

        assertThatThrownBy(() -> service.ownedBy("ESP-2026-000001", "MEMBER", "MBR-9"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    @DisplayName("Expirer close l'annonce sans rien encaisser")
    void expiringSettlesNothing() {
        CashPaymentDeclaration pending = declaration(CashDeclarationStatus.AWAITING_CONFIRMATION);

        CashPaymentDeclaration expired = service.expire(pending, "Délai dépassé sans encaissement");

        assertThat(expired.getStatus()).isEqualTo(CashDeclarationStatus.EXPIRED);
        assertThat(expired.getCloseReason()).isEqualTo("Délai dépassé sans encaissement");
        verify(paymentService, never()).registerCashPayment(any(), any());
    }
}

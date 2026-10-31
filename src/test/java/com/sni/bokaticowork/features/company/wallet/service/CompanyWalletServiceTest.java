package com.sni.bokaticowork.features.company.wallet.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import com.sni.bokaticowork.features.company.wallet.model.SpendOperation;
import com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization;
import com.sni.bokaticowork.features.company.wallet.repository.CompanyWalletSpendRepository;
import com.sni.bokaticowork.features.company.wallet.repository.SpendingAuthorizationRepository;
import com.sni.bokaticowork.features.payment.control.service.WalletAdminActionService;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.security.service.WalletMerchantPaymentGuard;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletTopUpService;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les depenses des collaborateurs sur le portefeuille de leur organisation · autorisation, plafonds,
 * validation du gerant au-dela du seuil, et delai de 48 heures.
 */
class CompanyWalletServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    private final WalletService walletService = mock(WalletService.class);
    private final WalletAccountRepository wallets = mock(WalletAccountRepository.class);
    private final WalletLedgerService ledger = mock(WalletLedgerService.class);
    private final WalletHoldRepository holds = mock(WalletHoldRepository.class);
    private final WalletHoldService holdService = mock(WalletHoldService.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final BillingDocumentService documents = mock(BillingDocumentService.class);
    private final SpendingAuthorizationRepository authorizations = mock(SpendingAuthorizationRepository.class);
    private final CompanyWalletSpendRepository spends = mock(CompanyWalletSpendRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final MemberInAppNotifier notifier = mock(MemberInAppNotifier.class);
    private final CompanyWalletService service = new CompanyWalletService(walletService, wallets, ledger, holds, holdService,
            mock(WalletAdminActionService.class), mock(WalletTopUpService.class), mock(WalletMerchantPaymentGuard.class),
            payments, documents, authorizations, spends, customers, members, links, sequences, notifier,
            Clock.fixed(NOW, ZoneId.of("Africa/Brazzaville")), "XAF", Duration.ofHours(48));

    private Customer acme;
    private Member jean;
    private Member owner;
    private WalletAccount wallet;
    private SpendingAuthorization authorization;

    @BeforeEach
    void setUp() {
        acme = Customer.builder().id(1L).customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        jean = member("MBR-1", "Jean", "Mbemba");
        owner = member("MBR-9", "Awa", "Gérante");
        when(customers.findByCustomerId("CUS-COM-1")).thenReturn(Optional.of(acme));
        when(members.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(jean));
        when(links.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE))
                .thenReturn(Optional.of(CustomerMemberLink.builder().customer(acme).member(jean).role(MemberLinkRole.EMPLOYEE).build()));
        when(links.findAllByCustomerAndStatusOrderByStartedAtDesc(acme, MemberLinkStatus.ACTIVE)).thenReturn(List.of(
                CustomerMemberLink.builder().customer(acme).member(owner).role(MemberLinkRole.OWNER).build()));

        wallet = WalletAccount.builder().id(50L).walletNumber("WAL-ACME").ownerType("CUSTOMER").ownerCode("CUS-COM-1")
                .currency("XAF").status(WalletStatus.ACTIVE).availableBalance(new BigDecimal("500000")).build();
        when(wallets.findByOwnerAndCurrency("CUSTOMER", "CUS-COM-1", "XAF")).thenReturn(Optional.of(wallet));
        when(wallets.findById(50L)).thenReturn(Optional.of(wallet));
        when(walletService.serviceWallet("WAL-ACME")).thenReturn(wallet);

        authorization = SpendingAuthorization.builder().id(70L).shortId(7L).walletId(50L).companyCode("CUS-COM-1")
                .memberCode("MBR-1").status(SpendingAuthorization.Status.ACTIVE).build();
        authorization.setOperations(EnumSet.of(SpendOperation.BOOKING, SpendOperation.INVOICE_PAYMENT));
        when(authorizations.findOpen(50L, "MBR-1")).thenReturn(Optional.of(authorization));

        when(spends.sumEngagedSince(any(), anyString(), any())).thenReturn(BigDecimal.ZERO);
        when(spends.saveAndFlush(any(CompanyWalletSpend.class))).thenAnswer(call -> call.getArgument(0));
        when(sequences.next("wallet_hold")).thenReturn("HLD-1");
        PaymentIntentResponse intent = intent("INT-1");
        PaymentTransactionResponse transaction = transaction("TXN-1");
        when(payments.createIntent(any())).thenReturn(intent);
        when(payments.payWithWallet(eq("INT-1"), any())).thenReturn(transaction);
    }

    private static Member member(String id, String first, String last) {
        Member member = new Member();
        member.setMemberId(id);
        member.setFirstname(first);
        member.setLastname(last);
        member.setEmail(id.toLowerCase() + "@acme.cg");
        return member;
    }

    private static PaymentIntentResponse intent(String number) {
        PaymentIntentResponse intent = mock(PaymentIntentResponse.class);
        when(intent.intentNumber()).thenReturn(number);
        return intent;
    }

    private static PaymentTransactionResponse transaction(String number) {
        PaymentTransactionResponse transaction = mock(PaymentTransactionResponse.class);
        when(transaction.transactionNumber()).thenReturn(number);
        return transaction;
    }

    /** Une facture de reservation du membre, reliee a l'organisation. */
    private void invoice(String number, String due, String lineSource) {
        BillingDocumentResponse invoice = mock(BillingDocumentResponse.class);
        when(invoice.documentNumber()).thenReturn(number);
        when(invoice.customerType()).thenReturn("MEMBER");
        when(invoice.customerCode()).thenReturn("MBR-1");
        when(invoice.companyCustomerCode()).thenReturn("CUS-COM-1");
        when(invoice.status()).thenReturn(BillingDocumentStatus.SENT);
        when(invoice.currency()).thenReturn("XAF");
        when(invoice.balanceDue()).thenReturn(new BigDecimal(due));
        BillingDocumentLineResponse line = mock(BillingDocumentLineResponse.class);
        when(line.sourceType()).thenReturn(lineSource);
        List<BillingDocumentLineResponse> lines = List.of(line);
        when(invoice.lines()).thenReturn(lines);
        when(documents.get(number)).thenReturn(invoice);
    }

    @Test
    @DisplayName("Sans autorisation, ou pour une operation qu'elle ne couvre pas : refus")
    void refusesWithoutAuthorization() {
        invoice("INV-1", "10000", "SUBSCRIPTION_RENEWAL");
        assertThatThrownBy(() -> service.payAsCollaborator(jean, "CUS-COM-1", "INV-1"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("ne couvre pas");

        when(authorizations.findOpen(50L, "MBR-1")).thenReturn(Optional.empty());
        invoice("INV-2", "10000", "BOOKING");
        assertThatThrownBy(() -> service.payAsCollaborator(jean, "CUS-COM-1", "INV-2"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("pas autorisé");
        verify(payments, never()).payWithWallet(any(), any());
    }

    @Test
    @DisplayName("Le refus dit quel plafond bloque · par paiement, puis du jour")
    void capsSayWhichOneBlocks() {
        invoice("INV-1", "30000", "BOOKING");
        authorization.setMaxPerTransaction(new BigDecimal("25000"));
        assertThatThrownBy(() -> service.payAsCollaborator(jean, "CUS-COM-1", "INV-1"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Plafond par paiement");

        authorization.setMaxPerTransaction(null);
        authorization.setMaxPerDay(new BigDecimal("50000"));
        when(spends.sumEngagedSince(eq(50L), eq("MBR-1"), any())).thenReturn(new BigDecimal("25000"));
        assertThatThrownBy(() -> service.payAsCollaborator(jean, "CUS-COM-1", "INV-1"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Plafond du jour");
    }

    @Test
    @DisplayName("Sous le seuil : reglee aussitot, intention au nom de l'organisation, le collaborateur note")
    void paysAtOnceUnderThreshold() {
        invoice("INV-1", "10000", "BOOKING");

        var result = service.payAsCollaborator(jean, "CUS-COM-1", "INV-1");

        assertThat(result.spend().status()).isEqualTo(CompanyWalletSpend.Status.EXECUTED);
        ArgumentCaptor<CreatePaymentIntentRequest> intent = ArgumentCaptor.forClass(CreatePaymentIntentRequest.class);
        verify(payments).createIntent(intent.capture());
        assertThat(intent.getValue().customerType()).isEqualTo("CUSTOMER");
        assertThat(intent.getValue().customerCode()).isEqualTo("CUS-COM-1");
        assertThat(intent.getValue().sourceType()).isEqualTo("BILLING_DOCUMENT");
        assertThat(intent.getValue().sourceCode()).isEqualTo("INV-1");
        assertThat(intent.getValue().metadataJson()).contains("MBR-1");
        ArgumentCaptor<WalletPaymentRequest> payment = ArgumentCaptor.forClass(WalletPaymentRequest.class);
        verify(payments).payWithWallet(eq("INT-1"), payment.capture());
        assertThat(payment.getValue().walletNumber()).isEqualTo("WAL-ACME");
        assertThat(payment.getValue().createdBy()).isEqualTo("MBR-1");
        assertThat(result.spend().transactionNumber()).isEqualTo("TXN-1");
    }

    @Test
    @DisplayName("Au-dela du seuil : en attente, fonds retenus, gerant prevenu, rien de preleve")
    void waitsForOwnerAboveThreshold() {
        invoice("INV-1", "80000", "BOOKING");
        authorization.setApprovalAbove(new BigDecimal("50000"));

        var result = service.payAsCollaborator(jean, "CUS-COM-1", "INV-1");

        assertThat(result.spend().status()).isEqualTo(CompanyWalletSpend.Status.PENDING_APPROVAL);
        assertThat(result.spend().expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(48)));
        verify(ledger).placeHold(eq(wallet), eq(new BigDecimal("80000")), eq("COMPANY_SPEND"), eq("INV-1"), eq("HLD-1"), eq("MBR-1"), any());
        verify(holds).save(any(WalletHold.class));
        verify(notifier).notify(eq("COMPANY_SPEND_APPROVAL_REQUESTED"), any(), any(), eq("mbr-9@acme.cg"), any(), eq("MBR-9"), any(), any());
        verify(payments, never()).payWithWallet(any(), any());
    }

    private CompanyWalletSpend pending(Instant expiresAt) {
        CompanyWalletSpend spend = CompanyWalletSpend.builder().id(80L).shortId(8L).walletId(50L).companyCode("CUS-COM-1")
                .spentByMember("MBR-1").operation(SpendOperation.BOOKING).amount(new BigDecimal("80000")).currency("XAF")
                .documentNumber("INV-1").status(CompanyWalletSpend.Status.PENDING_APPROVAL).holdNumber("HLD-1")
                .expiresAt(expiresAt).build();
        when(spends.lockByShortId(8L)).thenReturn(Optional.of(spend));
        when(holds.findByHoldNumber("HLD-1")).thenReturn(Optional.of(WalletHold.builder().holdNumber("HLD-1")
                .status(WalletHoldStatus.ACTIVE).build()));
        return spend;
    }

    @Test
    @DisplayName("Le gerant accepte : retenue levee, facture reglee")
    void approveReleasesAndPays() {
        CompanyWalletSpend spend = pending(NOW.plus(Duration.ofHours(10)));
        invoice("INV-1", "80000", "BOOKING");

        service.approve(acme, owner, 8L);

        verify(holdService).release("HLD-1", "MBR-9");
        verify(payments).payWithWallet(eq("INT-1"), any());
        assertThat(spend.getStatus()).isEqualTo(CompanyWalletSpend.Status.EXECUTED);
        assertThat(spend.getDecidedBy()).isEqualTo("MBR-9");
    }

    @Test
    @DisplayName("Sans reponse sous 48 heures : annulee, fonds rendus, collaborateur prevenu")
    void expiresAfterTheWindow() {
        CompanyWalletSpend spend = pending(NOW.minusSeconds(1));

        assertThat(service.expire(8L)).isTrue();

        assertThat(spend.getStatus()).isEqualTo(CompanyWalletSpend.Status.EXPIRED);
        verify(holdService).release("HLD-1", "SYSTEM");
        verify(notifier).notify(eq("COMPANY_SPEND_DECIDED"), any(), any(), eq("mbr-1@acme.cg"), any(), eq("MBR-1"), any(), any());
        assertThatThrownBy(() -> service.approve(acme, owner, 8L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Fin du lien : autorisations suspendues, demandes en attente annulees")
    void linkEndSuspends() {
        CompanyWalletSpend spend = pending(NOW.plus(Duration.ofHours(10)));
        when(authorizations.findActiveOfMemberInCompany("MBR-1", "CUS-COM-1")).thenReturn(List.of(authorization));
        when(spends.findPendingOfMemberInCompany("CUS-COM-1", "MBR-1")).thenReturn(List.of(spend));

        service.suspendOnLinkEnd(acme, jean, "Fin du rattachement : départ", "admin");

        assertThat(authorization.getStatus()).isEqualTo(SpendingAuthorization.Status.SUSPENDED);
        assertThat(spend.getStatus()).isEqualTo(CompanyWalletSpend.Status.CANCELLED);
        verify(holdService).release("HLD-1", "admin");
    }

    @Test
    @DisplayName("La facture d'un autre membre, non reliee a l'organisation, n'existe pas")
    void foreignInvoiceIsHidden() {
        BillingDocumentResponse foreign = mock(BillingDocumentResponse.class);
        when(foreign.customerType()).thenReturn("MEMBER");
        when(foreign.customerCode()).thenReturn("MBR-2");
        when(foreign.status()).thenReturn(BillingDocumentStatus.SENT);
        when(documents.get("INV-X")).thenReturn(foreign);

        assertThatThrownBy(() -> service.payAsCollaborator(jean, "CUS-COM-1", "INV-X"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("L'operation se lit sur la facture · reservation, abonnement, pass, sinon facture")
    void operationFromInvoice() {
        invoice("A", "1", "BOOKING");
        assertThat(CompanyWalletService.operationOf(documents.get("A"))).isEqualTo(SpendOperation.BOOKING);
        invoice("B", "1", "SUBSCRIPTION_RENEWAL");
        assertThat(CompanyWalletService.operationOf(documents.get("B"))).isEqualTo(SpendOperation.SUBSCRIPTION);
        invoice("C", "1", "PASS_SETUP");
        assertThat(CompanyWalletService.operationOf(documents.get("C"))).isEqualTo(SpendOperation.PASS);
        invoice("D", "1", "MANUAL");
        assertThat(CompanyWalletService.operationOf(documents.get("D"))).isEqualTo(SpendOperation.INVOICE_PAYMENT);
    }
}

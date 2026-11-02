package com.sni.bokaticowork.features.portal.company.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.company.statement.service.AccountStatementService;
import com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import com.sni.bokaticowork.features.portal.billing.service.ClientBillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le gerant regle les factures de son organisation · et seulement celles-la.
 */
class ClientCompanyBillingServiceTest {

    private final CompanyMembershipService membership = mock(CompanyMembershipService.class);
    private final BillingDocumentService documents = mock(BillingDocumentService.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final ClientCompanyBillingService service = new ClientCompanyBillingService(membership,
            mock(AccountStatementService.class), documents, mock(BillingDocumentPdfService.class),
            mock(ClientBillingService.class), payments, mock(PaymentReceiptService.class),
            mock(PawapayDepositRepository.class), mock(PawapayDepositService.class), mock(CashPaymentDeclarationService.class));

    private Member owner;
    private Customer acme;

    @BeforeEach
    void setUp() {
        owner = new Member();
        owner.setMemberId("MBR-OWNER");
        acme = Customer.builder().id(1L).customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        when(membership.ownedCompany(owner, "CUS-COM-1")).thenReturn(acme);
    }

    private BillingDocumentResponse invoice(String number, String customerType, String customerCode, String companyCode,
                                            BillingDocumentStatus status) {
        BillingDocumentResponse invoice = mock(BillingDocumentResponse.class);
        when(invoice.documentNumber()).thenReturn(number);
        when(invoice.customerType()).thenReturn(customerType);
        when(invoice.customerCode()).thenReturn(customerCode);
        when(invoice.companyCustomerCode()).thenReturn(companyCode);
        when(invoice.status()).thenReturn(status);
        when(documents.get(number)).thenReturn(invoice);
        return invoice;
    }

    private static ClientInitiateMobileMoneyPaymentRequest phone() {
        return mock(ClientInitiateMobileMoneyPaymentRequest.class);
    }

    @Test
    @DisplayName("Facture de membre reliee : le gerant la paie, le paiement porte son identifiant")
    void ownerPaysLinkedMemberInvoice() {
        invoice("INV-3", "MEMBER", "MBR-1", "CUS-COM-1", BillingDocumentStatus.SENT);
        PaymentIntentResponse intent = mock(PaymentIntentResponse.class);
        when(intent.intentNumber()).thenReturn("PI-1");
        when(payments.createIntentFromBillingDocument(any())).thenReturn(intent);

        service.payWithMobileMoney(owner, "CUS-COM-1", "INV-3", phone());

        ArgumentCaptor<InitiateMobileMoneyDepositRequest> request = ArgumentCaptor.forClass(InitiateMobileMoneyDepositRequest.class);
        verify(payments).initiateMobileMoneyDeposit(eq("PI-1"), request.capture());
        assertThat(request.getValue().createdBy()).isEqualTo("MBR-OWNER");
    }

    @Test
    @DisplayName("Facture d'une autre organisation, ou d'un membre non relie : 404, rien n'est lance")
    void foreignInvoiceIsHidden() {
        invoice("INV-X", "CUSTOMER", "CUS-COM-2", null, BillingDocumentStatus.SENT);
        invoice("INV-Y", "MEMBER", "MBR-1", null, BillingDocumentStatus.SENT);

        assertThatThrownBy(() -> service.payWithMobileMoney(owner, "CUS-COM-1", "INV-X", phone()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.invoice(owner, "CUS-COM-1", "INV-Y"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(payments, never()).createIntentFromBillingDocument(any());
    }

    @Test
    @DisplayName("Un brouillon de l'organisation n'existe pas pour le gerant")
    void draftIsHidden() {
        invoice("INV-D", "CUSTOMER", "CUS-COM-1", null, BillingDocumentStatus.DRAFT);
        assertThatThrownBy(() -> service.invoice(owner, "CUS-COM-1", "INV-D")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Un membre qui n'est pas gerant n'atteint pas l'organisation")
    void nonOwnerIsRefused() {
        Member employee = new Member();
        employee.setMemberId("MBR-1");
        when(membership.ownedCompany(employee, "CUS-COM-1")).thenThrow(new ResourceNotFoundException("Organisation introuvable"));
        assertThatThrownBy(() -> service.statement(employee, "CUS-COM-1", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

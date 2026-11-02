package com.sni.bokaticowork.features.company;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.membership.service.CompanyInvoiceLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Relier d'anciennes factures de membres · le document emis ne change pas.
 */
class CompanyInvoiceLinkServiceTest {

    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final CompanyInvoiceLinkService service = new CompanyInvoiceLinkService(documents, customers, members, links);

    private Customer acme;
    private Member jean;

    @BeforeEach
    void setUp() {
        acme = Customer.builder().id(1L).customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        jean = new Member();
        jean.setMemberId("MBR-1");
        when(customers.findByCustomerId("CUS-COM-1")).thenReturn(Optional.of(acme));
        when(members.findByMemberIdIncludingArchived("MBR-1")).thenReturn(Optional.of(jean));
        when(links.existsByCustomerAndMember(acme, jean)).thenReturn(true);
    }

    private BillingDocument invoice(String number, BillingDocumentStatus status) {
        BillingDocument document = BillingDocument.builder().documentNumber(number).documentType(BillingDocumentType.INVOICE)
                .status(status).customerType("MEMBER").customerCode("MBR-1").customerName("Jean Mbemba")
                .issueDate(LocalDate.of(2026, 9, 1)).totalAmount(new BigDecimal("50000")).balanceDue(new BigDecimal("50000"))
                .build();
        when(documents.findByDocumentNumber(number)).thenReturn(Optional.of(document));
        return document;
    }

    private static CompanyInvoiceLinkService.LinkRequest byNumbers(String... numbers) {
        return new CompanyInvoiceLinkService.LinkRequest("CUS-COM-1", List.of(numbers), null, null, null);
    }

    @Test
    @DisplayName("Une facture emise est reliee, le document imprime ne change pas")
    void linksAnIssuedInvoice() {
        BillingDocument issued = invoice("INV-1", BillingDocumentStatus.SENT);

        var result = service.link(byNumbers("INV-1"), "admin");

        assertThat(result.linked()).isEqualTo(1);
        assertThat(issued.getCompanyCustomerCode()).isEqualTo("CUS-COM-1");
        assertThat(issued.getCompanyLinkSource()).isEqualTo("ATTACHED");
        assertThat(issued.getCompanyName()).isNull();
        assertThat(issued.getCustomerName()).isEqualTo("Jean Mbemba");
    }

    @Test
    @DisplayName("La previsualisation dit pourquoi une facture ne se relie pas, et n'ecrit rien")
    void previewExplainsRefusals() {
        invoice("INV-1", BillingDocumentStatus.DRAFT);
        BillingDocument other = invoice("INV-2", BillingDocumentStatus.SENT);
        other.setCompanyCustomerCode("CUS-COM-2");

        var result = service.preview(byNumbers("INV-1", "INV-2"));

        assertThat(result.eligible()).isZero();
        assertThat(result.lines()).extracting(CompanyInvoiceLinkService.Line::reason)
                .anyMatch(r -> r.contains("Brouillon"))
                .anyMatch(r -> r.contains("autre organisation"));
        verify(documents, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Un membre jamais rattache a l'organisation : refus")
    void unlinkedMemberIsNotEligible() {
        when(links.existsByCustomerAndMember(acme, jean)).thenReturn(false);
        invoice("INV-1", BillingDocumentStatus.SENT);

        assertThat(service.preview(byNumbers("INV-1")).lines().get(0).eligible()).isFalse();
    }

    @Test
    @DisplayName("Un lien pose a l'emission ne se retire pas, un lien pose apres coup oui")
    void unlinkRules() {
        BillingDocument atIssue = invoice("INV-1", BillingDocumentStatus.SENT);
        atIssue.setCompanyCustomerCode("CUS-COM-1");
        atIssue.setCompanyName("SARL Acme");
        atIssue.setCompanyLinkSource("AT_ISSUE");
        assertThatThrownBy(() -> service.unlink("INV-1")).isInstanceOf(BadRequestException.class);

        BillingDocument attached = invoice("INV-2", BillingDocumentStatus.SENT);
        attached.setCompanyCustomerCode("CUS-COM-1");
        attached.setCompanyLinkSource("ATTACHED");
        service.unlink("INV-2");
        assertThat(attached.getCompanyCustomerCode()).isNull();
    }
}

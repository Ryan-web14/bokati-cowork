package com.sni.bokaticowork.features.company;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A quelle organisation relier une facture de membre · selon le mode, ou le choix explicite.
 */
class MemberBillingCompanyResolverTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final CustomerMemberLinkRepository linkRepository = mock(CustomerMemberLinkRepository.class);
    private final MemberBillingCompanyResolver resolver = new MemberBillingCompanyResolver(memberRepository, linkRepository);

    private Member jean;
    private Customer acme;
    private Customer mbote;

    @BeforeEach
    void setUp() {
        jean = new Member();
        jean.setMemberId("MBR-1");
        jean.setFirstname("Jean");
        jean.setLastname("Mbemba");
        acme = Customer.builder().id(1L).customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY)
                .defaultInvoiceLinkMode(InvoiceLinkMode.AUTOMATIC).build();
        mbote = Customer.builder().id(2L).customerId("CUS-COM-2").companyName("Ets Mboté").type(CustomerType.COMPANY)
                .defaultInvoiceLinkMode(InvoiceLinkMode.ON_CHOICE).build();
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(jean));
        when(linkRepository.findAllByMemberAndStatusOrderByStartedAtDesc(jean, MemberLinkStatus.ACTIVE))
                .thenReturn(List.of(link(acme), link(mbote)));
    }

    private CustomerMemberLink link(Customer company) {
        return CustomerMemberLink.builder().customer(company).member(jean).role(MemberLinkRole.EMPLOYEE)
                .status(MemberLinkStatus.ACTIVE).startedAt(Instant.now()).createdBy("t").build();
    }

    @Test
    @DisplayName("Sans choix : l'organisation en mode automatique")
    void automaticCompanyByDefault() {
        assertThat(resolver.resolve("MEMBER", "MBR-1", null)).contains(acme);
    }

    @Test
    @DisplayName("NONE : le membre seul, meme en mode automatique")
    void noneMeansMemberAlone() {
        assertThat(resolver.resolve("MEMBER", "MBR-1", "NONE")).isEmpty();
    }

    @Test
    @DisplayName("Choix explicite d'une organisation au choix")
    void explicitChoice() {
        assertThat(resolver.resolve("MEMBER", "MBR-1", "CUS-COM-2")).contains(mbote);
    }

    @Test
    @DisplayName("Choix d'une organisation ou le membre n'est pas rattache : refus")
    void unlinkedCompanyIsRefused() {
        assertThatThrownBy(() -> resolver.resolve("MEMBER", "MBR-1", "CUS-COM-9"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est pas rattaché");
    }

    @Test
    @DisplayName("Sans organisation automatique : le membre seul")
    void noAutomaticNoCompany() {
        acme.setDefaultInvoiceLinkMode(InvoiceLinkMode.ON_CHOICE);
        assertThat(resolver.resolve("MEMBER", "MBR-1", null)).isEmpty();
    }

    @Test
    @DisplayName("Une facture d'organisation ne se relie pas a une autre organisation")
    void companyInvoiceCannotLinkACompany() {
        assertThatThrownBy(() -> resolver.resolve("CUSTOMER", "CUS-COM-1", "CUS-COM-2"))
                .isInstanceOf(BadRequestException.class);
        assertThat(resolver.resolve("CUSTOMER", "CUS-COM-1", null)).isEmpty();
    }
}

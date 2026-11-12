package com.sni.bokaticowork.core.outbox.service.implementation;

import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L'adresse de facturation choisie par le membre passe avant celle du compte et de la fiche client. */
class OutboxRecipientResolverBillingTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final BusinessRepository businessRepository = mock(BusinessRepository.class);
    private final MemberPreferences preferences = mock(MemberPreferences.class);
    private final OutboxRecipientResolver resolver =
            new OutboxRecipientResolver(customerRepository, memberRepository, businessRepository);

    private final Member member = Member.builder().id(7L).memberId("MBR-7").email("awa@example.com").build();

    @BeforeEach
    void setUp() {
        resolver.setMemberPreferences(preferences);
        when(preferences.billingRecipient(any(Member.class), any())).thenAnswer(call -> call.getArgument(1));
        when(preferences.billingEmail(any(Member.class))).thenReturn(Optional.empty());
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-7")).thenReturn(Optional.of(member));
    }

    @Test
    @DisplayName("Membre : l'adresse de facturation des preferences, liste comprise, passe avant celle du compte")
    void memberBillingEmailWins() {
        when(preferences.billingRecipient(member, "awa@example.com")).thenReturn("compta@awa.cg, awa@example.com");

        OutboxRecipientResolver.Recipient recipient = resolver.resolveBillingByOwnerCode(DocumentOwnerType.MEMBER, "MBR-7");

        assertThat(recipient.email()).isEqualTo("compta@awa.cg, awa@example.com");
    }

    @Test
    @DisplayName("Membre sans preference : le courriel du compte, comme avant")
    void memberWithoutPreferenceKeepsAccountEmail() {
        OutboxRecipientResolver.Recipient recipient = resolver.resolveBillingByOwnerCode(DocumentOwnerType.MEMBER, "MBR-7");

        assertThat(recipient.email()).isEqualTo("awa@example.com");
    }

    @Test
    @DisplayName("Client personne relie a un membre : l'adresse du membre passe avant customer.billingEmail")
    void personCustomerFollowsLinkedMember() {
        Customer customer = Customer.builder().customerId("CUS-1").type(CustomerType.PERSON)
                .email("awa@example.com").billingEmail("ancienne@awa.cg").build();
        when(customerRepository.findByCustomerId("CUS-1")).thenReturn(Optional.of(customer));
        when(memberRepository.findAllByCustomerAndDeletedFalse(eq(customer), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member)));
        when(preferences.billingEmail(member)).thenReturn(Optional.of("compta@awa.cg"));

        OutboxRecipientResolver.Recipient recipient = resolver.resolveBillingByOwnerCode(DocumentOwnerType.CUSTOMER, "CUS-1");

        assertThat(recipient.email()).isEqualTo("compta@awa.cg");
    }

    @Test
    @DisplayName("Client entreprise : garde son propre billingEmail")
    void companyCustomerKeepsItsBillingEmail() {
        Customer customer = Customer.builder().customerId("CUS-2").type(CustomerType.COMPANY)
                .companyName("SARL Mbote").email("contact@mbote.cg").billingEmail("factures@mbote.cg").build();
        when(customerRepository.findByCustomerId("CUS-2")).thenReturn(Optional.of(customer));

        OutboxRecipientResolver.Recipient recipient = resolver.resolveBillingByOwnerCode(DocumentOwnerType.CUSTOMER, "CUS-2");

        assertThat(recipient.email()).isEqualTo("factures@mbote.cg");
        verify(preferences, never()).billingEmail(any(Member.class));
    }

    @Test
    @DisplayName("Organisation : son adresse, sans preference de membre")
    void businessUnchanged() {
        BusinessEntity business = new BusinessEntity();
        business.setName("SARL Mbote");
        business.setEmail("contact@mbote.cg");
        when(businessRepository.findByCode("BIZ-1")).thenReturn(Optional.of(business));

        OutboxRecipientResolver.Recipient recipient = resolver.resolveBillingByOwnerCode(DocumentOwnerType.BUSINESS, "BIZ-1");

        assertThat(recipient.email()).isEqualTo("contact@mbote.cg");
    }

    @Test
    @DisplayName("Contrats et conformite : la resolution ordinaire ignore l'adresse de facturation")
    void ordinaryResolutionIgnoresBillingPreference() {
        when(preferences.billingRecipient(member, "awa@example.com")).thenReturn("compta@awa.cg");

        OutboxRecipientResolver.Recipient recipient = resolver.resolveByOwnerCode(DocumentOwnerType.MEMBER, "MBR-7");

        assertThat(recipient.email()).isEqualTo("awa@example.com");
    }
}

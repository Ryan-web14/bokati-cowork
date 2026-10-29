package com.sni.bokaticowork.features.company;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.dto.MembershipDtos;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rattachement des membres · un lien actif par couple, une seule organisation automatique.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyMembershipServiceTest {

    @Mock private CustomerMemberLinkRepository linkRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CustomerService customerService;
    @InjectMocks private CompanyMembershipService service;

    private Customer acme;
    private Customer mbote;
    private Member jean;

    @BeforeEach
    void setUp() {
        acme = company(1L, "CUS-COM-1", "SARL Acme", InvoiceLinkMode.ON_CHOICE);
        mbote = company(2L, "CUS-COM-2", "Ets Mboté", InvoiceLinkMode.ON_CHOICE);
        jean = new Member();
        jean.setId(10L);
        jean.setMemberId("MBR-1");
        jean.setFirstname("Jean");
        jean.setLastname("Mbemba");
        jean.setEmail("jean@mbote.cg");
        jean.setCustomer(acme);
        when(customerRepository.findByCustomerId("CUS-COM-1")).thenReturn(Optional.of(acme));
        when(customerRepository.findByCustomerId("CUS-COM-2")).thenReturn(Optional.of(mbote));
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(jean));
        when(linkRepository.findByCustomerAndMemberAndStatus(any(), any(), any())).thenReturn(Optional.empty());
        when(linkRepository.findAllByMemberAndStatusOrderByStartedAtDesc(jean, MemberLinkStatus.ACTIVE)).thenReturn(List.of());
    }

    private static Customer company(Long id, String code, String name, InvoiceLinkMode mode) {
        return Customer.builder().id(id).customerId(code).companyName(name).type(CustomerType.COMPANY)
                .defaultInvoiceLinkMode(mode).build();
    }

    private CustomerMemberLink link(Customer company, MemberLinkRole role, InvoiceLinkMode mode) {
        return CustomerMemberLink.builder().id(company.getId() * 100).shortId(company.getId()).customer(company).member(jean)
                .role(role).status(MemberLinkStatus.ACTIVE).startedAt(Instant.now()).invoiceLinkMode(mode).createdBy("t").build();
    }

    @Test
    @DisplayName("Rattache un membre comme employe")
    void linksAnEmployee() {
        var response = service.link("CUS-COM-1",
                new MembershipDtos.LinkMemberRequest("MBR-1", MemberLinkRole.EMPLOYEE, null, null, null, null), "admin");

        assertThat(response.role()).isEqualTo(MemberLinkRole.EMPLOYEE);
        assertThat(response.effectiveInvoiceLinkMode()).isEqualTo(InvoiceLinkMode.ON_CHOICE);
        verify(linkRepository).save(any());
    }

    @Test
    @DisplayName("Un deuxieme lien actif avec la meme organisation est refuse")
    void refusesADuplicateActiveLink() {
        when(linkRepository.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE))
                .thenReturn(Optional.of(link(acme, MemberLinkRole.OWNER, null)));

        assertThatThrownBy(() -> service.link("CUS-COM-1",
                new MembershipDtos.LinkMemberRequest("MBR-1", MemberLinkRole.EMPLOYEE, null, null, null, null), "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà rattaché");
    }

    @Test
    @DisplayName("Deux organisations en mode automatique pour un membre : refus, rien n'est ecrit")
    void refusesTwoAutomaticCompanies() {
        when(linkRepository.findAllByMemberAndStatusOrderByStartedAtDesc(jean, MemberLinkStatus.ACTIVE))
                .thenReturn(List.of(link(acme, MemberLinkRole.EMPLOYEE, InvoiceLinkMode.AUTOMATIC)));

        assertThatThrownBy(() -> service.link("CUS-COM-2",
                new MembershipDtos.LinkMemberRequest("MBR-1", MemberLinkRole.OWNER, null, InvoiceLinkMode.AUTOMATIC, null, null), "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("une organisation en mode automatique");
        verify(linkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une personne ne recoit pas de membres")
    void refusesAPersonCustomer() {
        Customer person = Customer.builder().id(3L).customerId("CUS-PER-1").type(CustomerType.PERSON).build();
        when(customerRepository.findByCustomerId("CUS-PER-1")).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> service.link("CUS-PER-1",
                new MembershipDtos.LinkMemberRequest("MBR-1", MemberLinkRole.EMPLOYEE, null, null, null, null), "admin"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Fin du lien avec l'organisation payeuse : le membre repasse sur sa fiche personnelle")
    void endingThePayerLinkSwitchesToPersonalCustomer() {
        CustomerMemberLink link = link(acme, MemberLinkRole.EMPLOYEE, null);
        when(linkRepository.findByShortId(1L)).thenReturn(Optional.of(link));
        Customer personal = Customer.builder().id(9L).customerId("CUS-PER-9").type(CustomerType.PERSON).build();
        when(customerRepository.findByEmailAndDeletedFalse("jean@mbote.cg")).thenReturn(Optional.of(personal));

        var response = service.end("CUS-COM-1", 1L, "Départ de l'entreprise", "admin");

        assertThat(response.status()).isEqualTo(MemberLinkStatus.ENDED);
        assertThat(response.payerChanged()).isTrue();
        assertThat(jean.getCustomer()).isSameAs(personal);
    }

    @Test
    @DisplayName("Un employe ne voit pas les membres de l'organisation : 404")
    void employeeCannotListMembers() {
        when(linkRepository.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE))
                .thenReturn(Optional.of(link(acme, MemberLinkRole.EMPLOYEE, null)));

        assertThatThrownBy(() -> service.membersForOwner(jean, "CUS-COM-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Le gerant voit les membres de son organisation")
    void ownerListsMembers() {
        CustomerMemberLink own = link(acme, MemberLinkRole.OWNER, null);
        when(linkRepository.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE)).thenReturn(Optional.of(own));
        when(linkRepository.findAllByCustomerAndStatusOrderByStartedAtDesc(acme, MemberLinkStatus.ACTIVE)).thenReturn(List.of(own));

        assertThat(service.membersForOwner(jean, "CUS-COM-1")).hasSize(1);
    }
}

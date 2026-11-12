package com.sni.bokaticowork.features.company.service.support;

import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La fiche legale d'une organisation et ceux qui signent pour elle.
 */
class CompanyLegalCardTest {

    private final OrganizationBillingProfile profile = mock(OrganizationBillingProfile.class);
    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final CompanyLegalCard card = new CompanyLegalCard(profile, links);

    private Customer acme;

    @BeforeEach
    void setUp() {
        BusinessEntity legal = BusinessEntity.builder().name("SARL Acme").legalForm("SARL").rccmNumber("CG-PNR-01-2020-B12-00001")
                .niuNumber("M2020000123456X").legalRepresentativeName("Awa Mabiala").legalRepresentativeTitle("Gérante").build();
        acme = Customer.builder().customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).businessEntity(legal).build();
        when(profile.identifiers(any())).thenReturn(List.of());
    }

    private static Member member(String id, String first, String last, String email) {
        Member member = new Member();
        member.setMemberId(id);
        member.setFirstname(first);
        member.setLastname(last);
        member.setEmail(email);
        return member;
    }

    private void links(CustomerMemberLink... active) {
        when(links.findAllByCustomerAndStatusOrderByStartedAtDesc(acme, MemberLinkStatus.ACTIVE)).thenReturn(List.of(active));
    }

    @Test
    @DisplayName("La fiche se lit · denomination, forme, RCCM, NIU, representant")
    void readsCard() {
        var c = card.of(acme).orElseThrow();
        assertThat(c.name()).isEqualTo("SARL Acme");
        assertThat(c.legalForm()).isEqualTo("SARL");
        assertThat(c.rccm()).isEqualTo("CG-PNR-01-2020-B12-00001");
        assertThat(c.niu()).isEqualTo("M2020000123456X");
        assertThat(c.representativeName()).isEqualTo("Awa Mabiala");
        assertThat(card.of(Customer.builder().type(CustomerType.PERSON).build())).isEmpty();
    }

    @Test
    @DisplayName("Signataires : le representant, puis les membres autorises a signer · pas les autres")
    void signatories() {
        links(CustomerMemberLink.builder().member(member("MBR-1", "Jean", "Mbemba", "jean@acme.cg")).role(MemberLinkRole.EMPLOYEE).canSign(true).build(),
                CustomerMemberLink.builder().member(member("MBR-2", "Paul", "Nkounkou", "paul@acme.cg")).role(MemberLinkRole.EMPLOYEE).canSign(false).build());

        var signatories = card.signatories(acme);

        assertThat(signatories).extracting(CompanyLegalCard.Signatory::name).containsExactly("Awa Mabiala", "Jean Mbemba");
        assertThat(signatories.getFirst().legalRepresentative()).isTrue();
        assertThat(card.designated(acme, "jean@acme.cg", null)).isPresent();
        assertThat(card.designated(acme, "paul@acme.cg", "Paul Nkounkou")).isEmpty();
        // Le representant sans compte se designe par son nom, accents et casse mis a part.
        assertThat(card.designated(acme, "awa@gmail.com", "awa MABIALA")).isPresent();
    }

    @Test
    @DisplayName("Le representant qui a un compte autorise a signer · une seule ligne, avec son adresse")
    void representativeWithAccount() {
        links(CustomerMemberLink.builder().member(member("MBR-3", "Awa", "Mabiala", "awa@acme.cg")).role(MemberLinkRole.OWNER).canSign(true).build());

        var signatories = card.signatories(acme);

        assertThat(signatories).hasSize(1);
        assertThat(signatories.getFirst().email()).isEqualTo("awa@acme.cg");
        assertThat(signatories.getFirst().memberId()).isEqualTo("MBR-3");
        assertThat(card.designated(acme, "autre@gmail.com", "Awa Mabiala")).isEmpty();
    }
}

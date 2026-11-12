package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.service.support.CompanyLegalCard;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.MailItem;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemEventRepository;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La domiciliation d'une organisation · lue sur sa fiche, vue et retiree par ceux qu'elle autorise.
 */
class CompanyDomiciliationTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final DomiciliationCompanyAccess access = new DomiciliationCompanyAccess(customers, links);

    private Customer acme;
    private Member owner;
    private Member mailClerk;
    private Member employee;
    private DomiciliationContract contract;

    private static Member member(String id) {
        Member member = new Member();
        member.setMemberId(id);
        member.setFirstname("Prenom");
        member.setLastname(id);
        return member;
    }

    @BeforeEach
    void setUp() {
        acme = Customer.builder().customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        when(customers.findByCustomerId("CUS-COM-1")).thenReturn(Optional.of(acme));
        owner = member("MBR-OWN");
        mailClerk = member("MBR-MAIL");
        employee = member("MBR-EMP");
        when(links.findByCustomerAndMemberAndStatus(acme, owner, MemberLinkStatus.ACTIVE)).thenReturn(Optional.of(
                CustomerMemberLink.builder().customer(acme).member(owner).role(MemberLinkRole.OWNER).build()));
        when(links.findByCustomerAndMemberAndStatus(acme, mailClerk, MemberLinkStatus.ACTIVE)).thenReturn(Optional.of(
                CustomerMemberLink.builder().customer(acme).member(mailClerk).role(MemberLinkRole.EMPLOYEE).canCollectMail(true).build()));
        when(links.findByCustomerAndMemberAndStatus(acme, employee, MemberLinkStatus.ACTIVE)).thenReturn(Optional.of(
                CustomerMemberLink.builder().customer(acme).member(employee).role(MemberLinkRole.EMPLOYEE).build()));
        Subscription subscription = new Subscription();
        subscription.setSubscriberType(SubscriberType.CUSTOMER);
        subscription.setSubscriberCode("CUS-COM-1");
        contract = DomiciliationContract.builder().id(9L).contractNumber("DOM-1").subscription(subscription).legalName("Ancien nom").build();
    }

    @Test
    @DisplayName("Le gerant et le membre autorise voient la domiciliation · pas un employe sans le drapeau")
    void accessFollowsTheLink() {
        assertThat(access.mayAccess(owner, contract)).isTrue();
        assertThat(access.mayAccess(mailClerk, contract)).isTrue();
        assertThat(access.mayAccess(employee, contract)).isFalse();
    }

    @Test
    @DisplayName("Retrait d'un pli : par un membre autorise, son nom retenu ; refuse a un employe sans le drapeau")
    void collectByAuthorisedMember() {
        MailItemRepository mails = mock(MailItemRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        MailItem item = MailItem.builder().itemNumber("PLI-1").contract(contract).mailType(MailItem.Type.LETTER)
                .status(MailItem.Status.RECEIVED).build();
        when(mails.findByItemNumber("PLI-1")).thenReturn(Optional.of(item));
        when(mails.save(any(MailItem.class))).thenAnswer(call -> call.getArgument(0));
        when(members.findByMemberIdAndDeletedFalse("MBR-MAIL")).thenReturn(Optional.of(mailClerk));
        when(members.findByMemberIdAndDeletedFalse("MBR-EMP")).thenReturn(Optional.of(employee));
        MailItemService service = new MailItemService(mails, mock(MailItemEventRepository.class), null, null, null, null, null, null,
                access, members);

        assertThatThrownBy(() -> service.collect("PLI-1", new MailItemService.Collection(null, null, null, "MBR-EMP"), "accueil"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("autorisé");

        MailItem collected = service.collect("PLI-1", new MailItemService.Collection(null, null, null, "MBR-MAIL"), "accueil");

        assertThat(collected.getStatus()).isEqualTo(MailItem.Status.COLLECTED);
        assertThat(collected.getCollectedByMember()).isEqualTo("MBR-MAIL");
        assertThat(collected.getCollectedBy()).isEqualTo("Prenom MBR-MAIL");
    }

    @Test
    @DisplayName("La fiche se reporte sur le contrat en cours · denomination, RCCM, NIU, representant")
    void cardIsAppliedToContract() {
        CompanyLegalCard legalCard = mock(CompanyLegalCard.class);
        when(legalCard.of(acme)).thenReturn(Optional.of(new CompanyLegalCard.Card("CUS-COM-1", "SARL Acme Congo", "SARL",
                "CG-PNR-01-2020-B12-00001", "M2020000123456X", "Awa Mabiala", "Gérante", null, List.of())));
        DomiciliationContractRepository contracts = mock(DomiciliationContractRepository.class);
        when(contracts.findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(SubscriberType.CUSTOMER, "CUS-COM-1"))
                .thenReturn(List.of(contract));
        DomiciliationService service = new DomiciliationService(contracts, null, null, null, null, null, null, null, null, null,
                access, legalCard, null);
        ReflectionTestUtils.setField(contract, "status", DomiciliationContract.Status.ACTIVE);

        assertThat(service.refreshCompany("CUS-COM-1")).isEqualTo(1);

        assertThat(contract.getLegalName()).isEqualTo("SARL Acme Congo");
        assertThat(contract.getRegistrationNumber()).isEqualTo("CG-PNR-01-2020-B12-00001");
        assertThat(contract.getTaxNumber()).isEqualTo("M2020000123456X");
        assertThat(contract.getLegalRepresentativeName()).isEqualTo("Awa Mabiala");
    }
}

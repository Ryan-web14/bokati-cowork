package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.sharing.dto.PassSharingDtos;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ouvrir et retirer l'acces des collaborateurs aux passes de leur organisation.
 */
class CompanyPassSharingServiceTest {

    private final PassRepository passes = mock(PassRepository.class);
    private final PassBeneficiaryRepository beneficiaries = mock(PassBeneficiaryRepository.class);
    private final EntitlementGrantRepository grants = mock(EntitlementGrantRepository.class);
    private final EntitlementLedgerRepository ledger = mock(EntitlementLedgerRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final CompanyPassSharingService service = new CompanyPassSharingService(passes, beneficiaries, grants, ledger,
            members, customers, links, bookings);

    private Customer acme;
    private Pass pass;
    private Member jean;
    private final List<PassBeneficiary> rows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        acme = Customer.builder().id(1L).customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        pass = new Pass();
        pass.setId(30L);
        pass.setPassNumber("PAS-ACME");
        pass.setOwnerType(SubscriberType.CUSTOMER);
        pass.setOwnerCode("CUS-COM-1");
        pass.setStatus(PassStatus.ACTIVE);
        when(passes.findByPassNumber("PAS-ACME")).thenReturn(Optional.of(pass));
        jean = new Member();
        jean.setMemberId("MBR-1");
        jean.setFirstname("Jean");
        jean.setLastname("Mbemba");
        when(members.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(jean));
        when(links.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE))
                .thenReturn(Optional.of(CustomerMemberLink.builder().customer(acme).member(jean).build()));
        when(beneficiaries.findAllByPassId(30L)).thenReturn(rows);
        when(beneficiaries.save(any(PassBeneficiary.class))).thenAnswer(call -> call.getArgument(0));
        when(grants.findAllByPassId(30L)).thenReturn(List.of());
        when(ledger.sumConsumedByBeneficiaryOnPass(30L)).thenReturn(List.of());
    }

    private static PassSharingDtos.AddBeneficiaryRequest add() {
        return new PassSharingDtos.AddBeneficiaryRequest("MBR-1", 10, null, null);
    }

    @Test
    @DisplayName("Un collaborateur rattache recoit l'acces au pass")
    void opensAccess() {
        service.addBeneficiary(acme, "PAS-ACME", add(), "MBR-OWNER");

        ArgumentCaptor<PassBeneficiary> saved = ArgumentCaptor.forClass(PassBeneficiary.class);
        verify(beneficiaries).save(saved.capture());
        assertThat(saved.getValue().getBeneficiaryCode()).isEqualTo("MBR-1");
        assertThat(saved.getValue().getMaxUsesForBeneficiary()).isEqualTo(10);
        assertThat(saved.getValue().getAddedBy()).isEqualTo("MBR-OWNER");
    }

    @Test
    @DisplayName("Un membre non rattache, ou deja beneficiaire, est refuse")
    void refusesUnlinkedOrDuplicate() {
        when(links.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addBeneficiary(acme, "PAS-ACME", add(), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("rattaché");

        when(links.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE))
                .thenReturn(Optional.of(CustomerMemberLink.builder().customer(acme).member(jean).build()));
        rows.add(PassBeneficiary.builder().pass(pass).beneficiaryType("MEMBER").beneficiaryCode("MBR-1").build());
        assertThatThrownBy(() -> service.addBeneficiary(acme, "PAS-ACME", add(), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("déjà accès");
        verify(beneficiaries, never()).save(any());
    }

    @Test
    @DisplayName("Un acces revoque est rouvert, pas double · une ligne par personne et par pass")
    void reopensRevokedAccess() {
        PassBeneficiary revoked = PassBeneficiary.builder().pass(pass).beneficiaryType("MEMBER").beneficiaryCode("MBR-1")
                .usedCount(4).revokedAt(Instant.now()).revokedReason("Départ").build();
        rows.add(revoked);

        service.addBeneficiary(acme, "PAS-ACME", add(), "admin");

        verify(beneficiaries).save(revoked);
        assertThat(revoked.getRevokedAt()).isNull();
        assertThat(revoked.getUsedCount()).isZero();
    }

    @Test
    @DisplayName("Le pass d'une autre organisation n'est pas visible")
    void foreignPassIsHidden() {
        pass.setOwnerCode("CUS-COM-2");
        assertThatThrownBy(() -> service.view(acme, "PAS-ACME")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Retirer l'acces signale ses reservations a venir sans les annuler")
    void revokeWarnsAboutFutureBookings() {
        PassBeneficiary access = PassBeneficiary.builder().pass(pass).shortId(7L).beneficiaryType("MEMBER")
                .beneficiaryCode("MBR-1").build();
        when(beneficiaries.findByShortId(7L)).thenReturn(Optional.of(access));
        when(bookings.findFutureBookingNumbersOnPass(eq("PAS-ACME"), eq("MBR-1"), any())).thenReturn(List.of("BKG-9"));

        PassSharingDtos.RevokeResult result = service.revokeBeneficiary(acme, "PAS-ACME", 7L, "Départ", "admin");

        assertThat(access.getRevokedAt()).isNotNull();
        assertThat(access.getRevokedReason()).isEqualTo("Départ");
        assertThat(result.futureBookings()).containsExactly("BKG-9");
        assertThat(result.warning()).contains("ne sont pas annulées");
    }

    @Test
    @DisplayName("Un acces ne sert que sur un pass actif, dans sa periode et ses passages")
    void usableAccess() {
        Instant now = Instant.now();
        PassBeneficiary access = PassBeneficiary.builder().pass(pass).beneficiaryType("MEMBER").beneficiaryCode("MBR-1")
                .usedCount(0).build();
        assertThat(CompanyPassLocator.usable(access, now)).isTrue();

        access.setMaxUsesForBeneficiary(2);
        access.setUsedCount(2);
        assertThat(CompanyPassLocator.usable(access, now)).isFalse();

        access.setUsedCount(0);
        pass.setStatus(PassStatus.SUSPENDED);
        assertThat(CompanyPassLocator.usable(access, now)).isFalse();

        pass.setStatus(PassStatus.ACTIVE);
        access.setValidUntil(now.minusSeconds(60));
        assertThat(CompanyPassLocator.usable(access, now)).isFalse();
    }
}

package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.enums.ReceiptPreference;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le membre derriere un destinataire de facturation, et ce que ses preferences changent.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberBillingContactTest {

    @Mock private MemberRepository memberRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MemberPreferences preferences;
    @InjectMocks private MemberBillingContact contact;

    private Member member(String email) {
        return Member.builder().id(1L).memberId("MBR-1").email(email).build();
    }

    @Test
    @DisplayName("L'adresse de facturation du membre passe avant l'adresse du compte")
    void billingEmailWinsOverAccountEmail() {
        Member member = member("joel@example.com");
        when(preferences.billingEmail(member)).thenReturn(Optional.of("compta@example.com, joel@example.com"));

        assertThat(contact.recipient(member, "joel@example.com")).isEqualTo("compta@example.com, joel@example.com");
        assertThat(contact.recipient(member, null)).isEqualTo("compta@example.com, joel@example.com");
    }

    @Test
    @DisplayName("Sans preference, l'adresse connue reste ; une adresse saisie a part est gardee")
    void noPreferenceOrTypedAddressKeepsCurrent() {
        Member member = member("joel@example.com");
        when(preferences.billingEmail(member)).thenReturn(Optional.empty());
        assertThat(contact.recipient(member, "joel@example.com")).isEqualTo("joel@example.com");

        when(preferences.billingEmail(member)).thenReturn(Optional.of("compta@example.com"));
        assertThat(contact.recipient(member, "autre@example.com")).isEqualTo("autre@example.com");
    }

    @Test
    @DisplayName("Le client personne d'un membre suit la preference, avant son propre billingEmail")
    void personCustomerFollowsMemberPreference() {
        Customer customer = Customer.builder().id(9L).customerId("CUS-1").type(CustomerType.PERSON)
                .email("joel@example.com").billingEmail("ancien@example.com").build();
        Member member = member("joel@example.com");
        member.setCustomer(customer);
        when(customerRepository.findByCustomerId("CUS-1")).thenReturn(Optional.of(customer));
        when(memberRepository.findAllByCustomerAndDeletedFalse(eq(customer), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member)));
        when(preferences.billingEmail(member)).thenReturn(Optional.of("compta@example.com"));

        assertThat(contact.recipient("CUSTOMER", "CUS-1", "ancien@example.com")).isEqualTo("compta@example.com");
    }

    @Test
    @DisplayName("Une organisation garde sa propre adresse et ses envois ne se coupent pas")
    void organisationIsNeverAMember() {
        Customer company = Customer.builder().id(10L).customerId("CUS-COM").type(CustomerType.COMPANY)
                .billingEmail("factures@sarl.cg").build();
        when(customerRepository.findByCustomerId("CUS-COM")).thenReturn(Optional.of(company));

        assertThat(contact.recipient("CUSTOMER", "CUS-COM", "factures@sarl.cg")).isEqualTo("factures@sarl.cg");
        assertThat(contact.allowsEmail("CUSTOMER", "CUS-COM", EventType.INVOICE_ISSUED)).isTrue();
        assertThat(contact.recipient("BUSINESS_ENTITY", "BIZ-1", "contact@biz.cg")).isEqualTo("contact@biz.cg");
        verify(memberRepository, never()).findAllByCustomerAndDeletedFalse(any(), any());
    }

    @Test
    @DisplayName("Envoi coupe seulement si le membre l'a coupe ; maintenu par defaut")
    void allowsEmailFollowsPreference() {
        Member member = member("joel@example.com");
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(member));
        when(preferences.allows(member, EventType.INVOICE_ISSUED, NotificationChannel.EMAIL)).thenReturn(false);
        when(preferences.allows(member, EventType.PAYMENT_RECEIVED, NotificationChannel.EMAIL)).thenReturn(true);

        assertThat(contact.allowsEmail("MEMBER", "MBR-1", EventType.INVOICE_ISSUED)).isFalse();
        assertThat(contact.allowsEmail("MEMBER", "MBR-1", EventType.PAYMENT_RECEIVED)).isTrue();
        assertThat(contact.allowsEmail("MEMBER", "MBR-INCONNU", EventType.INVOICE_ISSUED)).isTrue();
    }

    @Test
    @DisplayName("Recu de portefeuille, plafonds et fuseau lus dans les preferences du membre")
    void walletPreferences() {
        Member member = member("joel@example.com");
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(member));
        when(preferences.walletReceipt(member)).thenReturn(ReceiptPreference.INVOICE_ONLY);
        when(preferences.dailySpendingCap(member)).thenReturn(Optional.of(20000L));
        when(preferences.weeklySpendingCap(member)).thenReturn(Optional.empty());
        when(preferences.zone(member)).thenReturn(ZoneId.of("Europe/Paris"));

        assertThat(contact.wantsEveryWalletReceipt("MEMBER", "MBR-1")).isFalse();
        assertThat(contact.wantsEveryWalletReceipt("BUSINESS_ENTITY", "BIZ-1")).isTrue();
        MemberBillingContact.SpendingCaps caps = contact.spendingCaps("MEMBER", "MBR-1");
        assertThat(caps.daily()).isEqualTo(20000L);
        assertThat(caps.weekly()).isNull();
        assertThat(caps.zone()).isEqualTo(ZoneId.of("Europe/Paris"));
        assertThat(contact.spendingCaps("BUSINESS_ENTITY", "BIZ-1").isEmpty()).isTrue();
    }
}

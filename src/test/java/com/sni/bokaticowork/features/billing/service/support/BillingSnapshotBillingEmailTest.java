package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.company.service.support.OrganizationBillingProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L'adresse figee sur une facture adressee a un membre suit son adresse de facturation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingSnapshotBillingEmailTest {

    @Mock private MemberRepository memberRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private OrganizationBillingProfile organizationProfile;
    @Mock private MemberBillingContact memberContact;
    @InjectMocks private BillingCustomerSnapshotResolver resolver;

    private CreateBillingDocumentRequest request(String type, String code, String email) {
        CreateBillingDocumentRequest request = mock(CreateBillingDocumentRequest.class);
        when(request.customerType()).thenReturn(type);
        when(request.customerCode()).thenReturn(code);
        when(request.customerEmail()).thenReturn(email);
        return request;
    }

    @Test
    @DisplayName("Facture a un membre : l'adresse de facturation des preferences remplace celle du compte")
    void memberBillingEmail() {
        Member member = Member.builder().id(1L).memberId("MBR-1").firstname("Joel").email("joel@example.com").build();
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(member));
        when(memberContact.memberOf("MEMBER", "MBR-1")).thenReturn(Optional.of(member));
        when(memberContact.recipient(member, "joel@example.com")).thenReturn("compta@example.com");

        assertThat(resolver.resolve(request("MEMBER", "MBR-1", null)).customerEmail()).isEqualTo("compta@example.com");
    }

    @Test
    @DisplayName("Facture a une organisation : son adresse reste")
    void organisationKeepsItsAddress() {
        BusinessEntity business = BusinessEntity.builder().code("BIZ-1").name("SARL").email("factures@sarl.cg").build();
        when(businessRepository.findByCode("BIZ-1")).thenReturn(Optional.of(business));
        when(memberContact.memberOf(any(), any())).thenReturn(Optional.empty());

        assertThat(resolver.resolve(request("BUSINESS_ENTITY", "BIZ-1", null)).customerEmail()).isEqualTo("factures@sarl.cg");
    }
}

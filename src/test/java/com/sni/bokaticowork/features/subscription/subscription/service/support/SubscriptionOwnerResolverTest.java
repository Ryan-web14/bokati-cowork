package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionOwnerResolverTest {

    @Mock private MemberRepository memberRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BusinessRepository businessRepository;
    @InjectMocks private SubscriptionOwnerResolver resolver;

    @Test
    @DisplayName("Entreprise cliente (CUS-COM) envoyee en BUSINESS_ENTITY : reconnue comme CUSTOMER")
    void companyCustomerSentAsBusinessEntityIsResolvedAsCustomer() {
        Customer company = new Customer();
        company.setCustomerId("CUS-COM-202611-00000012");
        company.setType(CustomerType.COMPANY);
        when(businessRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByCustomerId("CUS-COM-202611-00000012")).thenReturn(Optional.of(company));

        var owner = resolver.resolve(SubscriberType.BUSINESS_ENTITY, "CUS-COM-202611-00000012");

        assertThat(owner.type()).isEqualTo(SubscriberType.CUSTOMER);
        assertThat(owner.customer()).isSameAs(company);
        assertThat(owner.code()).isEqualTo("CUS-COM-202611-00000012");
        assertThat(owner.typeOr(SubscriberType.BUSINESS_ENTITY)).isEqualTo(SubscriberType.CUSTOMER);
    }

    @Test
    @DisplayName("Une fiche /businesses (BIZ-...) reste BUSINESS_ENTITY")
    void realBusinessEntityStaysBusinessEntity() {
        BusinessEntity business = new BusinessEntity();
        business.setCode("BIZ-1");
        when(businessRepository.findByCode("BIZ-1")).thenReturn(Optional.of(business));

        assertThat(resolver.resolve(SubscriberType.BUSINESS_ENTITY, "BIZ-1").type()).isEqualTo(SubscriberType.BUSINESS_ENTITY);
    }

    @Test
    @DisplayName("Un client particulier envoye en BUSINESS_ENTITY reste refuse, avec l'indication du bon type")
    void personCustomerSentAsBusinessEntityIsRefused() {
        Customer person = new Customer();
        person.setCustomerId("CUS-PER-1");
        person.setType(CustomerType.PERSON);
        when(businessRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByCustomerId("CUS-PER-1")).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> resolver.resolve(SubscriberType.BUSINESS_ENTITY, "CUS-PER-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CUSTOMER");
    }
}

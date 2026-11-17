package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Une entreprise se retrouve par son profil client comme par son entite, dans les deux sens. */
class SubscriberIdentitiesTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final BusinessRepository businesses = mock(BusinessRepository.class);
    private final SubscriberIdentities identities = new SubscriberIdentities(customers, businesses);

    private Customer companyProfile() {
        BusinessEntity business = new BusinessEntity();
        business.setId(5L);
        business.setCode("BUS-0005");
        Customer customer = new Customer();
        customer.setCustomerId("CUS-COM-202611-00000012");
        customer.setBusinessEntity(business);
        return customer;
    }

    @Test
    void theCustomerCodeAsBusinessEntityFindsTheCustomerProfile() {
        when(customers.findByCustomerId("CUS-COM-202611-00000012")).thenReturn(Optional.of(companyProfile()));
        when(businesses.findByCode(anyString())).thenReturn(Optional.empty());

        assertThat(identities.of(SubscriberType.BUSINESS_ENTITY, "CUS-COM-202611-00000012")).containsExactlyInAnyOrder(
                new SubscriberIdentities.Identity(SubscriberType.BUSINESS_ENTITY, "CUS-COM-202611-00000012"),
                new SubscriberIdentities.Identity(SubscriberType.CUSTOMER, "CUS-COM-202611-00000012"),
                new SubscriberIdentities.Identity(SubscriberType.BUSINESS_ENTITY, "BUS-0005"));
    }

    @Test
    void theBusinessCodeFindsItsCustomerProfiles() {
        BusinessEntity business = companyProfile().getBusinessEntity();
        when(customers.findByCustomerId(anyString())).thenReturn(Optional.empty());
        when(businesses.findByCode("BUS-0005")).thenReturn(Optional.of(business));
        when(customers.findAllByBusinessEntityId(5L)).thenReturn(List.of(companyProfile()));

        assertThat(identities.of(SubscriberType.BUSINESS_ENTITY, "BUS-0005")).containsExactlyInAnyOrder(
                new SubscriberIdentities.Identity(SubscriberType.BUSINESS_ENTITY, "BUS-0005"),
                new SubscriberIdentities.Identity(SubscriberType.CUSTOMER, "CUS-COM-202611-00000012"));
    }

    @Test
    void aMemberKeepsItsOnlyIdentity() {
        assertThat(identities.of(SubscriberType.MEMBER, "MBR-1")).containsExactly(new SubscriberIdentities.Identity(SubscriberType.MEMBER, "MBR-1"));
    }
}

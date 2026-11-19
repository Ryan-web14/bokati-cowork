package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Une fiche client entreprise (CUS-COM-...) et son entreprise ne font qu'un. */
class CompanyCustomerTypeTest {

    @Test
    void aCompanyCustomerCodeSentAsBusinessEntityIsACustomer() {
        BusinessRepository businesses = mock(BusinessRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        BillingCustomerSnapshotResolver resolver = new BillingCustomerSnapshotResolver(mock(MemberRepository.class), customers, businesses,
                null, null, null);
        when(businesses.findByCode("CUS-COM-202611-00000012")).thenReturn(Optional.empty());
        com.sni.bokaticowork.features.company.model.BusinessEntity entity = new com.sni.bokaticowork.features.company.model.BusinessEntity();
        entity.setCode("ENT-0042");
        when(customers.findByCustomerId("CUS-COM-202611-00000012")).thenReturn(Optional.of(Customer.builder()
                .customerId("CUS-COM-202611-00000012").businessEntity(entity).build()));
        when(businesses.findByCode("CUS-COM-202611-00000013")).thenReturn(Optional.empty());
        when(customers.findByCustomerId("CUS-COM-202611-00000013")).thenReturn(Optional.of(Customer.builder().customerId("CUS-COM-202611-00000013").build()));
        when(businesses.findByCode("ENT-1")).thenReturn(Optional.of(new com.sni.bokaticowork.features.company.model.BusinessEntity()));

        // La fiche client entreprise designe son entreprise.
        assertThat(resolver.canonical("BUSINESS_ENTITY", "CUS-COM-202611-00000012"))
                .isEqualTo(new BillingCustomerSnapshotResolver.Party("BUSINESS_ENTITY", "ENT-0042"));
        // Sans entreprise rattachee : la fiche client.
        assertThat(resolver.canonical("BUSINESS_ENTITY", "CUS-COM-202611-00000013"))
                .isEqualTo(new BillingCustomerSnapshotResolver.Party("CUSTOMER", "CUS-COM-202611-00000013"));
        assertThat(resolver.canonical("BUSINESS_ENTITY", "ENT-1").code()).isEqualTo("ENT-1");
        assertThat(resolver.canonical("MEMBER", "MBR-1").type()).isEqualTo("MEMBER");
    }
}

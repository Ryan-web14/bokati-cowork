package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 20/11/2026 : l'avoir d'un membre ou d'un client rattache a une entreprise s'impute sur une facture de l'entreprise. */
class CreditNoteToCompanyInvoiceTest {

    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final BillingDocumentServiceImpl service = mock(BillingDocumentServiceImpl.class, CALLS_REAL_METHODS);

    private static BillingDocument doc(String type, String code) {
        BillingDocument document = new BillingDocument();
        document.setCustomerType(type);
        document.setCustomerCode(code);
        return document;
    }

    private Customer company() {
        BusinessEntity entity = new BusinessEntity();
        entity.setCode("ENT-7");
        return Customer.builder().customerId("CUS-COM-7").type(CustomerType.COMPANY).businessEntity(entity).build();
    }

    @Test
    void aLinkedMembersCreditNoteGoesToTheCompanyInvoiceOnly() {
        ReflectionTestUtils.setField(service, "companyLinks", links);
        ReflectionTestUtils.setField(service, "customersForCompanies", customers);
        when(customers.findByCustomerId("CUS-COM-7")).thenReturn(Optional.of(company()));
        when(customers.findFirstByBusinessEntity_CodeIgnoreCaseAndTypeAndDeletedFalse("ENT-7", CustomerType.COMPANY)).thenReturn(Optional.of(company()));
        when(links.findActiveCompanyCodes("MBR-1")).thenReturn(List.of("CUS-COM-7"));
        when(links.findActiveCompanyCodes("MBR-2")).thenReturn(List.of("CUS-COM-9"));

        assertThat(service.holderBelongsToCompany(doc("MEMBER", "MBR-1"), doc("CUSTOMER", "CUS-COM-7"))).isTrue();
        assertThat(service.holderBelongsToCompany(doc("MEMBER", "MBR-1"), doc("BUSINESS_ENTITY", "ENT-7"))).isTrue();
        assertThat(service.holderBelongsToCompany(doc("MEMBER", "MBR-2"), doc("CUSTOMER", "CUS-COM-7"))).isFalse();
    }

    @Test
    void anIndividualCustomerOfTheSameEntityQualifiesAnOtherDoesNot() {
        ReflectionTestUtils.setField(service, "companyLinks", links);
        ReflectionTestUtils.setField(service, "customersForCompanies", customers);
        when(customers.findByCustomerId("CUS-COM-7")).thenReturn(Optional.of(company()));
        BusinessEntity same = new BusinessEntity();
        same.setCode("ENT-7");
        when(customers.findByCustomerId("CUS-IND-1")).thenReturn(Optional.of(Customer.builder().customerId("CUS-IND-1").businessEntity(same).build()));
        when(customers.findByCustomerId("CUS-IND-2")).thenReturn(Optional.of(Customer.builder().customerId("CUS-IND-2").build()));
        when(customers.findByCustomerId("CUS-IND-3")).thenReturn(Optional.of(Customer.builder().customerId("CUS-IND-3").type(CustomerType.PERSON).build()));

        assertThat(service.holderBelongsToCompany(doc("CUSTOMER", "CUS-IND-1"), doc("CUSTOMER", "CUS-COM-7"))).isTrue();
        assertThat(service.holderBelongsToCompany(doc("CUSTOMER", "CUS-IND-2"), doc("CUSTOMER", "CUS-COM-7"))).isFalse();
        // Une facture d'un particulier n'est pas une facture d'entreprise.
        assertThat(service.holderBelongsToCompany(doc("MEMBER", "MBR-1"), doc("CUSTOMER", "CUS-IND-3"))).isFalse();
    }
}

package com.sni.bokaticowork.features.client.customer.service.implementation;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerSummaryresponse;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.mapper.interfaces.CustomerMapper;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepo;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AddressService addressService;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Mock
    private KycAutomationService kycAutomationService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void shouldPersistAddressBeforeSavingCustomer() {
        AddressRequest addressRequest = AddressRequest.builder()
                .streetNumber("12")
                .streetName("Main")
                .district("Central")
                .city("Douala")
                .countryCode("CMR")
                .build();

        CustomerRequest request = CustomerRequest.builder()
                .type("PERSON")
                .firstname("Jane")
                .lastname("Doe")
                .email("jane@example.com")
                .phone("060000000")
                .address(addressRequest)
                .build();

        Address transientAddress = Address.builder().city("Douala").build();
        Address persistedAddress = Address.builder().id(42L).city("Douala").build();
        Customer mappedCustomer = Customer.builder().address(transientAddress).build();

        when(customerMapper.toEntity(request)).thenReturn(mappedCustomer);
        when(addressService.createAddress(addressRequest)).thenReturn(persistedAddress);
        when(sequenceGenerator.next("customer", LocalDate.now())).thenReturn("CUS-0001");

        customerService.createCustomer(request);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).save(customerCaptor.capture());
        assertSame(persistedAddress, customerCaptor.getValue().getAddress());
    }

    @Test
    void shouldSearchCustomersByQueryAndType() {
        Customer customer = Customer.builder()
                .id(10L)
                .customerId("CUS-0010")
                .type(CustomerType.COMPANY)
                .companyName("ACME")
                .email("contact@acme.com")
                .phone("060000000")
                .build();

        CustomerSummaryresponse summary = CustomerSummaryresponse.builder()
                .customerId("CUS-0010")
                .displayName("ACME")
                .build();

        when(customerRepo.basicSearch("acme", CustomerType.COMPANY)).thenReturn(java.util.List.of(customer));
        when(customerMapper.toSummary(customer)).thenReturn(summary);

        var results = customerService.basicSearch("acme", CustomerType.COMPANY);

        assertEquals(1, results.size());
        assertEquals("CUS-0010", results.getFirst().getCustomerId());
    }

    @Test
    void shouldResolveCustomerForServiceByCustomerId() {
        Customer customer = Customer.builder()
                .id(10L)
                .customerId("CUS-PER-2604-0001")
                .build();

        when(customerRepo.findByCustomerId("CUS-PER-2604-0001")).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerForService(" CUS-PER-2604-0001 ");

        assertSame(customer, result);
    }

    @Test
    void shouldResolveCustomerForServiceByNumericIdWhenCustomerIdIsMissing() {
        long numericId = 7242491337790099000L;
        Customer customer = Customer.builder()
                .id(numericId)
                .customerId("CUS-PER-2604-0001")
                .build();

        when(customerRepo.findByCustomerId(String.valueOf(numericId))).thenReturn(Optional.empty());
        when(customerRepo.findById(numericId)).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerForService(String.valueOf(numericId));

        assertSame(customer, result);
    }
}

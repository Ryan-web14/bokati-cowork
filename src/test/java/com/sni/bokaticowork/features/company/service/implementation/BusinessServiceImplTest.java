package com.sni.bokaticowork.features.company.service.implementation;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CurrencyService;
import com.sni.bokaticowork.features.company.dto.request.BusinessEntityRequest;
import com.sni.bokaticowork.features.company.dto.response.BusinessEntityResponse;
import com.sni.bokaticowork.features.company.mapper.interfaces.BusinessMapper;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @Mock
    private BusinessMapper businessMapper;

    @Mock
    private BusinessRepository businessRepo;

    @Mock
    private AddressService addressService;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private BusinessServiceImpl businessService;

    @Test
    void shouldPersistAddressAndCurrencyWhenCreatingBusiness() {
        AddressRequest addressRequest = AddressRequest.builder()
                .streetNumber("12")
                .streetName("Main")
                .district("Central")
                .city("Douala")
                .countryCode("CMR")
                .build();

        BusinessEntityRequest request = BusinessEntityRequest.builder()
                .name("Acme")
                .legalForm("SARL")
                .niuNumber("P123456789012345")
                .rccmNumber("RC-123")
                .taxId("TX-123")
                .activity("Cowork")
                .address(addressRequest)
                .phone("060000000")
                .email("contact@acme.com")
                .baseCurrencyCode("XAF")
                .build();

        BusinessEntity business = BusinessEntity.builder().name("Acme").build();
        BusinessEntityResponse response = BusinessEntityResponse.builder().name("Acme").build();
        Currency currency = Currency.builder().currencyCode("XAF").build();
        Address address = Address.builder().id(42L).city("Douala").build();

        when(businessRepo.existsByNameIgnoreCase("Acme")).thenReturn(false);
        when(businessRepo.existsByNiuNumber("P123456789012345")).thenReturn(false);
        when(businessRepo.existsByRccmNumber("RC-123")).thenReturn(false);
        when(currencyService.serviceCurrencyByCode("XAF")).thenReturn(currency);
        when(addressService.createAddress(addressRequest)).thenReturn(address);
        when(businessMapper.toEntity(request)).thenReturn(business);
        when(businessMapper.toDTO(business)).thenReturn(response);

        BusinessEntityResponse created = businessService.createBusiness(request);

        ArgumentCaptor<BusinessEntity> captor = ArgumentCaptor.forClass(BusinessEntity.class);
        verify(businessRepo).save(captor.capture());
        assertSame(currency, captor.getValue().getBaseCurrency());
        assertSame(address, captor.getValue().getAddress());
        assertEquals("Acme", created.getName());
    }

    @Test
    void shouldSearchBusinesses() {
        BusinessEntity business = BusinessEntity.builder().name("Acme").build();
        BusinessEntityResponse response = BusinessEntityResponse.builder().name("Acme").build();

        when(businessRepo.basicSearch("acme")).thenReturn(List.of(business));
        when(businessMapper.toDTO(business)).thenReturn(response);

        List<BusinessEntityResponse> results = businessService.basicSearch("acme");

        assertEquals(1, results.size());
        assertEquals("Acme", results.getFirst().getName());
    }
}

package com.sni.bokaticowork.features.company.account;

import com.sni.bokaticowork.core.baseClasses.model.Currency;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CurrencyService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.account.dto.CompanyDtos;
import com.sni.bokaticowork.features.company.account.service.CompanyAccountService;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La devise se change avec les conditions de facturation, comme a la creation de l'organisation. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyBillingCurrencyTest {

    @Mock CustomerRepository customerRepository;
    @Mock BusinessRepository businessRepository;
    @Mock CurrencyService currencyService;
    @Mock BillingDocumentRepository documentRepository;
    @Mock com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService customerService;
    @Mock com.sni.bokaticowork.features.company.repository.BusinessEntityIdentifierRepository identifierRepository;
    @Mock com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService addressService;
    @Mock com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository subscriptionRepository;
    @Mock com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository linkRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @InjectMocks CompanyAccountService service;

    private final Currency xaf = Currency.builder().currencyCode("XAF").build();
    private final Currency eur = Currency.builder().currencyCode("EUR").build();
    private BusinessEntity legal;

    @BeforeEach
    void setUp() {
        legal = BusinessEntity.builder().code("BUS-1").baseCurrency(xaf).build();
        Customer customer = Customer.builder().customerId("CUS-COM-1").type(CustomerType.COMPANY).businessEntity(legal).build();
        when(customerRepository.findByCustomerId("CUS-COM-1")).thenReturn(Optional.of(customer));
        when(currencyService.serviceCurrencyByCode("EUR")).thenReturn(eur);
        when(currencyService.serviceCurrencyByCode("ZZZ")).thenThrow(new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException("absente"));
    }

    @Test
    @DisplayName("currencyCode change la devise de l'organisation et revient dans la reponse")
    void currencyIsUpdated() {
        CompanyDtos.CompanyResponse response = service.updateBilling("CUS-COM-1",
                new CompanyDtos.UpdateBillingRequest("compta@exemple.cg", null, 30, new BigDecimal("500000"), null, "eur"), "admin@sni");

        assertThat(legal.getBaseCurrency()).isSameAs(eur);
        assertThat(response.currencyCode()).isEqualTo("EUR");
        assertThat(response.paymentTermsDays()).isEqualTo(30);
        verify(businessRepository).save(legal);
    }

    @Test
    @DisplayName("Sans currencyCode, la devise ne bouge pas")
    void currencyIsKept() {
        CompanyDtos.CompanyResponse response = service.updateBilling("CUS-COM-1",
                new CompanyDtos.UpdateBillingRequest(null, null, 15, null, null, null), "admin@sni");

        assertThat(response.currencyCode()).isEqualTo("XAF");
        verify(currencyService, never()).serviceCurrencyByCode(anyString());
        verify(businessRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une devise inconnue est refusee avec un message clair")
    void unknownCurrencyIsRefused() {
        assertThatThrownBy(() -> service.updateBilling("CUS-COM-1",
                new CompanyDtos.UpdateBillingRequest(null, null, null, null, null, "ZZZ"), "admin@sni"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ZZZ");
    }
}

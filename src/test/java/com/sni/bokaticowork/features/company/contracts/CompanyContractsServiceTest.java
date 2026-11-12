package com.sni.bokaticowork.features.company.contracts;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.service.support.CompanyLegalCard;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La fiche contrats d'une organisation relit l'organisation dans sa transaction : celle que le
 * controleur lui passe est detachee, et lire son entite juridique levait LazyInitializationException (500).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyContractsServiceTest {

    @Mock private ContractRepository contractRepository;
    @Mock private DomiciliationContractRepository domiciliationRepository;
    @Mock private CompanyLegalCard legalCard;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private CompanyContractsService service;

    @Test
    @DisplayName("Les signataires se lisent sur l'organisation relue dans la transaction, pas sur celle recue")
    void signatoriesAreReadOnTheAttachedCompany() {
        Customer detached = mock(Customer.class);
        when(detached.getId()).thenReturn(12L);
        Customer attached = mock(Customer.class);
        when(attached.getId()).thenReturn(12L);
        when(attached.getCustomerId()).thenReturn("CUS-COM-202611-00000012");
        when(customerRepository.findById(12L)).thenReturn(Optional.of(attached));
        when(contractRepository.findAllByOwnerTypeAndOwnerCode(any(), any(), any())).thenReturn(Page.empty());
        when(domiciliationRepository.findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(legalCard.signatories(attached)).thenReturn(List.of());

        CompanyContractsService.CompanyContracts result = service.list(detached);

        assertThat(result.companyCustomerCode()).isEqualTo("CUS-COM-202611-00000012");
        verify(legalCard).signatories(eq(attached));
        verify(legalCard, never()).signatories(eq(detached));
    }
}

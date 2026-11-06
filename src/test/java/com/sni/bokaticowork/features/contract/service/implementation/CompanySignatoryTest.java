package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.service.support.CompanyLegalCard;
import com.sni.bokaticowork.features.contract.dto.request.RequestContractSigningRequest;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractSigningToken;
import com.sni.bokaticowork.features.contract.repository.ContractSigningTokenRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le contrat d'une organisation se signe par une personne designee, plus devinee.
 */
class CompanySignatoryTest {

    private final ContractSigningTokenRepository tokens = mock(ContractSigningTokenRepository.class);
    private final ContractService contracts = mock(ContractService.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CompanyLegalCard legalCard = mock(CompanyLegalCard.class);
    private final ContractSigningServiceImpl service = new ContractSigningServiceImpl(tokens, contracts, mock(OutboxService.class),
            mock(MemberInAppNotifier.class), customers, legalCard);

    private Customer acme;

    @BeforeEach
    void setUp() {
        acme = Customer.builder().customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        when(customers.findByCustomerId("CUS-COM-1")).thenReturn(Optional.of(acme));
        Contract contract = Contract.builder().contractCode("CTR-1").title("Contrat-cadre").ownerType(DocumentOwnerType.CUSTOMER)
                .ownerCode("CUS-COM-1").status(ContractStatus.AWAITING_SIGNATURE).build();
        when(contracts.serviceByCode("CTR-1")).thenReturn(contract);
        when(tokens.findByContractCodeAndRevokedFalseAndSignedAtIsNull("CTR-1")).thenReturn(Optional.empty());
        when(tokens.save(any(ContractSigningToken.class))).thenAnswer(call -> {
            ContractSigningToken token = call.getArgument(0);
            if (token.getToken() == null) {
                token.setToken(UUID.randomUUID());
            }
            return token;
        });
        CompanyLegalCard.Signatory jean = new CompanyLegalCard.Signatory("Jean Mbemba", "Mandataire", "jean@acme.cg", "MBR-1", false);
        when(legalCard.signatories(acme)).thenReturn(List.of(
                new CompanyLegalCard.Signatory("Awa Mabiala", "Gérante", null, null, true), jean));
        when(legalCard.designated(acme, "jean@acme.cg", null)).thenReturn(Optional.of(jean));
        when(legalCard.designated(acme, "paul@acme.cg", "Paul")).thenReturn(Optional.empty());
    }

    private static RequestContractSigningRequest request(String email, String name) {
        RequestContractSigningRequest request = new RequestContractSigningRequest();
        request.setSignerEmail(email);
        request.setSignerName(name);
        return request;
    }

    @Test
    @DisplayName("Sans signataire dans la demande : le premier designe ayant une adresse")
    void defaultsToDesignated() {
        service.requestSigning("CTR-1", request(null, null));

        ArgumentCaptor<ContractSigningToken> token = ArgumentCaptor.forClass(ContractSigningToken.class);
        verify(tokens).save(token.capture());
        assertThat(token.getValue().getSignerEmail()).isEqualTo("jean@acme.cg");
        assertThat(token.getValue().getSignerName()).isEqualTo("Jean Mbemba");
    }

    @Test
    @DisplayName("Une personne non designee ne signe pas pour l'organisation")
    void refusesUndesignated() {
        assertThatThrownBy(() -> service.requestSigning("CTR-1", request("paul@acme.cg", "Paul")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("pas désigné");
    }
}

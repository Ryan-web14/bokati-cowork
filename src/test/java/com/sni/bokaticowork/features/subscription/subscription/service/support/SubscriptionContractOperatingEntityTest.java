package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractGenerationService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentSignatureRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * C'est toujours l'espace qui contracte, jamais le souscripteur.
 *
 * <p>{@code businessCode} designe l'entite exploitante · la partie qui signe face au client. On y
 * passait le code du souscripteur des qu'il s'agissait d'une entreprise, et comme
 * {@code business_entity} porte aussi les clients entreprises, la generation retrouvait bien une
 * ligne : <b>celle du client</b>. Le contrat nommait donc le client des deux cotes, l'espace n'y
 * figurait pas, et le lieu de signature comme la juridiction competente venaient de l'adresse du
 * client · exactement ce que ce champ existe pour garantir.</p>
 */
class SubscriptionContractOperatingEntityTest {

    private ContractService contractService;
    private ContractGenerationService contractGenerationService;
    private SubscriptionContractSupport support;

    @BeforeEach
    void setUp() {
        contractService = mock(ContractService.class);
        contractGenerationService = mock(ContractGenerationService.class);
        SubscriptionContractTemplatePolicyResolver policyResolver =
                mock(SubscriptionContractTemplatePolicyResolver.class);

        when(policyResolver.nonRefundablePolicy()).thenReturn(
                new SubscriptionContractTemplatePolicyResolver.ContractTemplatePolicy(
                        "SUBSCRIPTION_PASS_NON_REFUNDABLE",
                        "subscription-pass-non-refundable",
                        "Contrat abonnement non remboursable",
                        "Politique systeme",
                        List.of("Clause de non remboursement.")));

        when(contractService.create(any()))
                .thenReturn(ContractResponse.builder().contractCode("CTR-1").build());
        when(contractGenerationService.generatePdf(any()))
                .thenReturn(DocumentResponse.builder().code("DOC-1").build());

        support = new SubscriptionContractSupport(
                contractService,
                contractGenerationService,
                mock(SubscriptionOwnerResolver.class),
                policyResolver,
                mock(DocumentRepository.class),
                mock(DocumentSignatureRepository.class));
    }

    /** Un abonnement souscrit par une societe cliente · le cas ou le defaut se voyait. */
    private Subscription businessSubscription() {
        BusinessEntity client = BusinessEntity.builder()
                .code("BIZ-CLIENT-1")
                .name("SARL Mbote")
                .email("contact@mbote.cg")
                .phone("+242061234567")
                .rccmNumber("CG-BZV-01-2026-B12-00001")
                .build();

        Subscription subscription = new Subscription();
        subscription.setSubscriptionNumber("SUB-1");
        subscription.setSubscriberType(SubscriberType.BUSINESS_ENTITY);
        subscription.setSubscriberCode("BIZ-CLIENT-1");
        subscription.setBusinessEntity(client);
        subscription.setStartDate(LocalDate.of(2026, 10, 1));
        subscription.setCurrentPeriodEnd(LocalDate.of(2026, 10, 31));
        subscription.setAutoRenew(Boolean.TRUE);
        subscription.setCurrency("XAF");
        return subscription;
    }

    @Test
    @DisplayName("Le code du souscripteur n'est jamais envoye comme entite contractante")
    void theSubscriberCodeIsNeverSentAsTheContractingEntity() {
        support.createAndSignForSubscription(businessSubscription());

        ArgumentCaptor<CreateContractRequest> created = ArgumentCaptor.forClass(CreateContractRequest.class);
        ArgumentCaptor<GenerateContractRequest> generated = ArgumentCaptor.forClass(GenerateContractRequest.class);
        org.mockito.Mockito.verify(contractService).create(created.capture());
        org.mockito.Mockito.verify(contractGenerationService).generatePdf(generated.capture());

        // Null laisse la generation prendre l'entite designee comme exploitante · c'etait
        // « BIZ-CLIENT-1 », donc le client lui-meme.
        assertThat(created.getValue().getBusinessCode()).isNull();
        assertThat(generated.getValue().getBusinessCode()).isNull();
    }

    @Test
    @DisplayName("Le souscripteur reste bien le cocontractant, lui")
    void theSubscriberIsStillTheCounterparty() {
        support.createAndSignForSubscription(businessSubscription());

        ArgumentCaptor<GenerateContractRequest> generated = ArgumentCaptor.forClass(GenerateContractRequest.class);
        org.mockito.Mockito.verify(contractGenerationService).generatePdf(generated.capture());

        // Le correctif ne doit pas avoir efface le client du contrat · il change de cote, pas
        // d'existence.
        assertThat(generated.getValue().getOwnerCode()).isEqualTo("BIZ-CLIENT-1");
    }

    @Test
    @DisplayName("Le contrat est marque genere puis signe")
    void theContractIsMarkedGeneratedThenSigned() {
        String contractCode = support.createAndSignForSubscription(businessSubscription());

        assertThat(contractCode).isEqualTo("CTR-1");
        org.mockito.Mockito.verify(contractService).markGenerated("CTR-1", "DOC-1");
        org.mockito.Mockito.verify(contractService).markSigned("CTR-1", "DOC-1");
    }
}

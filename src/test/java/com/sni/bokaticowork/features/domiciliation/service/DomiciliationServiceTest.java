package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationAddress;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationRegistration;
import com.sni.bokaticowork.features.domiciliation.model.ServiceDefinition;
import com.sni.bokaticowork.features.domiciliation.model.SubscriptionService;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationAddressRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationRegistrationRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La domiciliation · trois verrous et une règle qui se déduit.
 *
 * <p>Pas d'activation sans pièces vérifiées. Pas d'activation sans enregistrement. Pas d'attestation
 * fiscale sans engagement annuel. Et la qualité fiscale ne se saisit pas : elle se recalcule à chaque
 * changement d'engagement, et la perdre révoque l'attestation en cours.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomiciliationServiceTest {

    @Mock private DomiciliationContractRepository contractRepository;
    @Mock private DomiciliationRegistrationRepository registrationRepository;
    @Mock private DomiciliationAddressRepository addressRepository;
    @Mock private SubscribedServiceService subscribedServiceService;
    @Mock private SubscriptionOwnerResolver ownerResolver;
    @Mock private SubscriberKycLevelGuard kycGuard;
    @Mock private DomiciliationDocuments documents;
    @Mock private TransactionContextResolver contextResolver;
    @Mock private OutboxService outboxService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private DomiciliationService service;

    private Subscription subscription;
    private SubscriptionService domiciliationService;
    private DomiciliationAddress address;

    @BeforeEach
    void setUp() {
        service.configure(365, 2, false, "");
        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").subscriberType(SubscriberType.MEMBER)
                .subscriberCode("MBR-1").currency("XAF").autoRenew(false).build();
        ServiceDefinition definition = ServiceDefinition.builder().code("SVC-DOMICILIATION").name("Domiciliation")
                .serviceCategory(ServiceDefinition.Category.DOMICILIATION).requiresContract(true).build();
        domiciliationService = SubscriptionService.builder().id(10L).serviceNumber("SSV-1").subscription(subscription)
                .serviceDefinition(definition).status(SubscriptionService.Status.PENDING).build();
        address = DomiciliationAddress.builder().id(5L).code("DAD-0001").label("Siège").fiscalCapable(true).active(true)
                .address(Address.builder().streetNumber("12").streetName("Avenue X").city("Brazzaville").build()).build();

        when(addressRepository.findByCode("DAD-0001")).thenReturn(Optional.of(address));
        when(contractRepository.countByAssignedAddress_IdAndStatusNotIn(anyLong(), any())).thenReturn(0L);
        when(contractRepository.findFirstBySubscription_IdAndStatusNotInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.empty());
        when(subscribedServiceService.ofSubscription("SUB-1")).thenReturn(List.of(domiciliationService));
        when(subscribedServiceService.activate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscribedServiceService.terminate(any(SubscriptionService.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next("domiciliation_contract")).thenReturn("DOM-2026-00001");
        when(sequenceGenerator.next("domiciliation_registration")).thenReturn("DRG-2026-00001");
        when(contractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ownerResolver.resolve(any(), anyString())).thenReturn(new SubscriptionOwnerResolver.Owner("MBR-1", null, null, null));
        when(contextResolver.resolveParty(anyString(), anyString()))
                .thenReturn(new TransactionContextResolver.PartyView("MEMBER", "MBR-1", "Jane", "jane@example.test", null, null, true));
        DocumentResponse generated = DocumentResponse.builder().code("DOC-1").build();
        when(documents.contract(any())).thenReturn(generated);
        when(documents.certificate(any(), any(), any(), any())).thenReturn(generated);
    }

    private DomiciliationService.OpenOrder order(BillingCycle cycle, Integer months) {
        return new DomiciliationService.OpenOrder("SUB-1", "Société Exemple", "SARL", "CG-BZV-01-2026-B12-00001", "P123",
                "Jane Doe", "DAD-0001", "B12", cycle, months, 30, LocalDate.of(2026, 10, 1), null);
    }

    private DomiciliationContract openedAndRegistered(BillingCycle cycle, int months) {
        DomiciliationContract contract = service.open(order(cycle, months), "alice");
        contract.setStatus(DomiciliationContract.Status.ACTIVE);
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));
        return contract;
    }

    // ---- La regle fiscale se deduit ---------------------------------------------------------

    @Test
    void douzeMoisDonnentLaQualiteFiscaleMoinsNonQuelQueSoitLeRythme() {
        assertTrue(service.open(order(BillingCycle.YEARLY, 12), "alice").getFiscalAddressEligible());
        assertFalse(service.open(order(BillingCycle.MONTHLY, 1), "alice").getFiscalAddressEligible());
        assertFalse(service.open(order(BillingCycle.QUARTERLY, 3), "alice").getFiscalAddressEligible());
        assertTrue(service.open(order(BillingCycle.MONTHLY, 12), "alice").getFiscalAddressEligible(),
                "Mensuel sur douze mois d'engagement · c'est l'engagement qui compte, pas le rythme");
    }

    @Test
    void uneAdresseNonFiscaleNeLaDonneAPersonne() {
        address.setFiscalCapable(false);

        assertFalse(service.open(order(BillingCycle.YEARLY, 12), "alice").getFiscalAddressEligible());
    }

    @Test
    void seulsLesTroisRythmesSontAdmis() {
        assertThrows(BadRequestException.class, () -> service.open(order(BillingCycle.WEEKLY, 1), "alice"));
    }

    // ---- Verrou 1 · pieces verifiees ---------------------------------------------------------

    @Test
    void sansPiecesVerifieesLeContratAttendLesPiecesEtLeDit() {
        DomiciliationContract contract = service.open(order(BillingCycle.YEARLY, 12), "alice");
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));
        Mockito.doThrow(new BadRequestException("KYC level 2 required")).when(kycGuard).require(anyInt(), any(), anyString());

        ConflictException thrown = assertThrows(ConflictException.class, () -> service.prepareForSignature("DOM-2026-00001", "alice"));

        assertEquals(DomiciliationContract.Status.PENDING_DOCUMENTS, contract.getStatus());
        assertTrue(thrown.getMessage().contains("représentant légal"), "Le message dit quoi fournir, pas « KYC insuffisant »");
        verify(documents, never()).contract(any());
    }

    @Test
    void avecLesPiecesLeContratEstGenereEtAttendLaSignature() {
        DomiciliationContract contract = service.open(order(BillingCycle.YEARLY, 12), "alice");
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));

        service.prepareForSignature("DOM-2026-00001", "alice");

        assertEquals(DomiciliationContract.Status.PENDING_SIGNATURE, contract.getStatus());
        assertEquals("DOC-1", contract.getContractDocumentCode());
    }

    // ---- Verrou 2 · enregistrement -------------------------------------------------------------

    @Test
    void laSignatureOuvreLaDemarcheExterneEtLActivationAttendSonIssue() {
        DomiciliationContract contract = service.open(order(BillingCycle.YEARLY, 12), "alice");
        contract.setStatus(DomiciliationContract.Status.PENDING_SIGNATURE);
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));

        service.markSigned("DOM-2026-00001", "DOC-SIGNED", "alice");

        assertEquals(DomiciliationContract.Status.PENDING_REGISTRATION, contract.getStatus());
        verify(registrationRepository).save(any(DomiciliationRegistration.class));
        verify(subscribedServiceService, never()).activate(any());
    }

    @Test
    void lEnregistrementConfirmeActiveLeContratEtRendLeService() {
        DomiciliationContract contract = service.open(order(BillingCycle.YEARLY, 12), "alice");
        contract.setStatus(DomiciliationContract.Status.PENDING_REGISTRATION);
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));
        DomiciliationRegistration registration = DomiciliationRegistration.builder().registrationNumber("DRG-2026-00001")
                .contract(contract).status(DomiciliationRegistration.Status.SUBMITTED).build();
        when(registrationRepository.findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.of(registration));

        service.confirmRegistration("DOM-2026-00001",
                new DomiciliationService.RegistrationOutcome("ENR-2026-4477", LocalDate.of(2026, 10, 3), null, "DOC-RECU", "DOC-ENR"), "alice");

        assertEquals(DomiciliationContract.Status.ACTIVE, contract.getStatus());
        assertEquals(DomiciliationRegistration.Status.REGISTERED, registration.getStatus());
        assertNotNull(contract.getFiscalAddressGrantedAt());
        verify(subscribedServiceService).activate(domiciliationService);
        verify(outboxService).publish(eq("DOMICILIATION_ACTIVATED"), eq("DOMICILIATION"), eq("DOM-2026-00001"), any());
    }

    @Test
    void lEnregistrementSeConfirmeAvecLaReferenceEtLExemplaireTimbre() {
        DomiciliationContract contract = service.open(order(BillingCycle.YEARLY, 12), "alice");
        contract.setStatus(DomiciliationContract.Status.PENDING_REGISTRATION);
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));

        assertThrows(BadRequestException.class, () -> service.confirmRegistration("DOM-2026-00001",
                new DomiciliationService.RegistrationOutcome(null, null, null, null, "DOC-ENR"), "alice"));
        assertThrows(BadRequestException.class, () -> service.confirmRegistration("DOM-2026-00001",
                new DomiciliationService.RegistrationOutcome("ENR-1", null, null, null, null), "alice"));
    }

    @Test
    void unRejetRouvreAussitotUneDemarche() {
        DomiciliationContract contract = service.open(order(BillingCycle.YEARLY, 12), "alice");
        contract.setStatus(DomiciliationContract.Status.PENDING_REGISTRATION);
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));
        DomiciliationRegistration registration = DomiciliationRegistration.builder().contract(contract)
                .status(DomiciliationRegistration.Status.SUBMITTED).build();
        when(registrationRepository.findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.of(registration));

        service.rejectRegistration("DOM-2026-00001", "Statuts non conformes", "alice");

        assertEquals(DomiciliationRegistration.Status.REJECTED, registration.getStatus());
        assertEquals(DomiciliationContract.Status.PENDING_REGISTRATION, contract.getStatus(), "En attente, pas en échec");
        verify(registrationRepository, Mockito.times(2)).save(any());
    }

    // ---- Verrou 3 · attestation ----------------------------------------------------------------

    @Test
    void pasDAttestationAvantLEnregistrement() {
        DomiciliationContract contract = service.open(order(BillingCycle.YEARLY, 12), "alice");
        contract.setStatus(DomiciliationContract.Status.PENDING_REGISTRATION);
        when(contractRepository.findByContractNumber("DOM-2026-00001")).thenReturn(Optional.of(contract));

        assertThrows(ConflictException.class, () -> service.issueCertificate("DOM-2026-00001", DomiciliationContract.CertificateScope.COMMERCIAL, "alice"));
        verify(documents, never()).certificate(any(), any(), any(), any());
    }

    @Test
    void lAttestationFiscaleEstRefuseeSousDouzeMoisAvecLaRegleExpliquee() {
        openedAndRegistered(BillingCycle.MONTHLY, 3);

        ConflictException thrown = assertThrows(ConflictException.class,
                () -> service.issueCertificate("DOM-2026-00001", DomiciliationContract.CertificateScope.FISCAL, "alice"));

        assertTrue(thrown.getMessage().contains("un an"), "Le refus explique la règle, il ne renvoie pas une erreur technique");
        assertTrue(thrown.getMessage().contains("commerciale"), "…et dit ce qui reste possible");
    }

    @Test
    void lAttestationCommercialeEstDelivreeSurUnContratActif() {
        DomiciliationContract contract = openedAndRegistered(BillingCycle.MONTHLY, 3);

        service.issueCertificate("DOM-2026-00001", DomiciliationContract.CertificateScope.COMMERCIAL, "alice");

        assertEquals(DomiciliationContract.CertificateScope.COMMERCIAL, contract.getCertificateScope());
        assertTrue(contract.certificateInForce());
        assertEquals(contract.getEndDate(), contract.getCertificateValidUntil(),
                "Une attestation ne survit pas au contrat qu'elle atteste · trois mois, pas un an");
    }

    @Test
    void lAttestationFiscaleEstDelivreeSurUnEngagementAnnuel() {
        DomiciliationContract contract = openedAndRegistered(BillingCycle.YEARLY, 12);

        service.issueCertificate("DOM-2026-00001", DomiciliationContract.CertificateScope.FISCAL, "alice");

        assertEquals(DomiciliationContract.CertificateScope.FISCAL, contract.getCertificateScope());
    }

    // ---- Changement d'engagement --------------------------------------------------------------

    @Test
    void passerDAnnuelAMensuelRetireLaQualiteFiscaleEtRevoqueLAttestation() {
        DomiciliationContract contract = openedAndRegistered(BillingCycle.YEARLY, 12);
        service.issueCertificate("DOM-2026-00001", DomiciliationContract.CertificateScope.FISCAL, "alice");

        service.changeCommitment("DOM-2026-00001", BillingCycle.MONTHLY, 1, "alice");

        assertFalse(contract.getFiscalAddressEligible());
        assertNotNull(contract.getFiscalAddressRevokedAt());
        assertNotNull(contract.getCertificateRevokedAt(), "L'attestation fiscale en cours est révoquée");
        assertFalse(contract.certificateInForce());
        verify(outboxService).publish(eq("DOMICILIATION_FISCAL_LOST"), any(), any(), any());
    }

    @Test
    void passerDeMensuelAAnnuelRendLaQualiteFiscaleSansRienDelivrer() {
        DomiciliationContract contract = openedAndRegistered(BillingCycle.MONTHLY, 1);

        service.changeCommitment("DOM-2026-00001", BillingCycle.YEARLY, 12, "alice");

        assertTrue(contract.getFiscalAddressEligible());
        assertNull(contract.getCertificateDocumentCode(), "Gagner l'éligibilité ne délivre rien · l'attestation se demande");
    }

    @Test
    void uneAttestationCommercialeSurvitAuChangementDEngagement() {
        DomiciliationContract contract = openedAndRegistered(BillingCycle.YEARLY, 12);
        service.issueCertificate("DOM-2026-00001", DomiciliationContract.CertificateScope.COMMERCIAL, "alice");

        service.changeCommitment("DOM-2026-00001", BillingCycle.MONTHLY, 1, "alice");

        assertNull(contract.getCertificateRevokedAt(), "Seule l'attestation fiscale dépend de l'engagement");
    }

    // ---- Fin ------------------------------------------------------------------------------------

    @Test
    void laFinRevoqueLAttestationPrevientEtPoseLaConservation() {
        DomiciliationContract contract = openedAndRegistered(BillingCycle.YEARLY, 12);
        service.issueCertificate("DOM-2026-00001", DomiciliationContract.CertificateScope.FISCAL, "alice");

        service.terminate("DOM-2026-00001", "Départ du domicilié", LocalDate.of(2027, 1, 31), "alice");

        assertEquals(DomiciliationContract.Status.TERMINATED, contract.getStatus());
        assertNotNull(contract.getCertificateRevokedAt());
        assertEquals(LocalDate.of(2037, 1, 31), contract.getRetainDocumentsUntil(), "Dix ans · posés à la fin, jamais raccourcis");
        assertNotNull(contract.getTerminationNotifiedAt());
        assertNull(contract.getAdministrationNotifiedAt(), "L'administration n'est prévenue que si la configuration le demande");
        verify(subscribedServiceService).terminate(domiciliationService);
    }

    @Test
    void lAdministrationEstPrevenueDeLaFinSiLaConfigurationLeDemande() {
        service.configure(365, 2, true, "impots@example.test");
        DomiciliationContract contract = openedAndRegistered(BillingCycle.YEARLY, 12);
        when(registrationRepository.findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());

        service.terminate("DOM-2026-00001", "Départ", null, "alice");

        assertNotNull(contract.getAdministrationNotifiedAt());
        verify(outboxService).publish(eq("DOMICILIATION_TERMINATED_ADMIN"), any(), any(), any());
    }

    @Test
    void uneAdressePleineNAccueillePlus() {
        address.setMaxOccupants(3);
        when(contractRepository.countByAssignedAddress_IdAndStatusNotIn(eq(5L), any())).thenReturn(3L);

        assertThrows(ConflictException.class, () -> service.open(order(BillingCycle.YEARLY, 12), "alice"));
    }
}

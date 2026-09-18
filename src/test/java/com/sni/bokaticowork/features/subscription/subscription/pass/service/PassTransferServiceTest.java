package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassTransferStatus;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassCredential;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassTransfer;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassCredentialRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassTransferRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le transfert de titulaire.
 *
 * <p>{@code transferable} existait comme booléen sans rien derrière. Trois règles viennent du
 * document — pass transférable, pass actif et non consommé, acceptation du destinataire vérifié —
 * et deux conséquences s'y ajoutent, que le transfert entraîne et qu'il valait mieux trancher
 * explicitement que découvrir.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassTransferServiceTest {

    @Mock private PassRepository passRepository;
    @Mock private PassTransferRepository transferRepository;
    @Mock private PassBeneficiaryRepository beneficiaryRepository;
    @Mock private PassCredentialRepository credentialRepository;
    @Mock private SubscriptionOwnerResolver ownerResolver;
    @Mock private SubscriberKycLevelGuard kycGuard;
    @Mock private PassEventWriter eventWriter;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private PassTransferService service;

    private Pass pass;

    @BeforeEach
    void setUp() {
        pass = Pass.builder()
                .passNumber("PASS-0001")
                .passType(PassType.DAY_PASS)
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MEM-ANCIEN")
                .status(PassStatus.ACTIVE)
                .transferable(Boolean.TRUE)
                .maxUses(10)
                .usedCount(2)
                .build();
        pass.setId(1L);

        when(passRepository.findByPassNumber(anyString())).thenReturn(Optional.of(pass));
        when(passRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(transferRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(transferRepository.findPending(anyLong())).thenReturn(Optional.empty());
        when(sequenceGenerator.next(anyString())).thenReturn("PTR-0001");
        when(ownerResolver.resolve(any(), anyString())).thenReturn(
                new SubscriptionOwnerResolver.Owner("MEM-NOUVEAU", null, null, null));
        when(beneficiaryRepository.findAllByPassId(anyLong())).thenReturn(List.of());
        when(credentialRepository.findActiveByPassId(anyLong())).thenReturn(List.of());
    }

    // -------------------------------------------------------------------------------------
    // Demande
    // -------------------------------------------------------------------------------------

    @Test
    void requestsATransferWithoutChangingTheOwnerYet() {
        PassTransfer transfer = request();

        assertEquals(PassTransferStatus.PENDING_ACCEPTANCE, transfer.getStatus());
        assertEquals("MEM-ANCIEN", transfer.getFromOwnerCode());
        // Le pass reste ou il est jusqu'a l'acceptation.
        assertEquals("MEM-ANCIEN", pass.getOwnerCode());
    }

    @Test
    void refusesANonTransferablePass() {
        pass.setTransferable(Boolean.FALSE);

        assertEquals("Ce pass n'est pas transférable",
                assertThrows(BadRequestException.class, this::request).getMessage());
    }

    @Test
    void refusesAConsumedOrSuspendedPass() {
        pass.setStatus(PassStatus.CONSUMED);
        assertEquals("Ce pass est entièrement consommé",
                assertThrows(BadRequestException.class, this::request).getMessage());

        pass.setStatus(PassStatus.SUSPENDED);
        assertEquals("Ce pass est suspendu",
                assertThrows(BadRequestException.class, this::request).getMessage());
    }

    @Test
    void refusesAnExpiredPass() {
        pass.setValidUntil(Instant.now().minusSeconds(60));

        assertEquals("Ce pass a expiré",
                assertThrows(BadRequestException.class, this::request).getMessage());
    }

    @Test
    void refusesATransferToTheCurrentHolder() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.request("PASS-0001", SubscriberType.MEMBER, "MEM-ANCIEN", null, null, "admin"));

        assertTrue(ex.getMessage().contains("appartient déjà"));
    }

    /**
     * Deux demandes simultanées vers deux destinataires donneraient le pass à celui qui accepte le
     * second, ce que l'émetteur n'a jamais voulu.
     */
    @Test
    void refusesASecondPendingTransfer() {
        when(transferRepository.findPending(anyLong())).thenReturn(Optional.of(new PassTransfer()));

        assertTrue(assertThrows(BadRequestException.class, this::request)
                .getMessage().contains("déjà en attente"));
    }

    /** Proposer un pass à quelqu'un qui n'existe pas doit échouer tout de suite, pas après une notification. */
    @Test
    void resolvesTheRecipientAtRequestTimeNotAtAcceptance() {
        service.request("PASS-0001", SubscriberType.MEMBER, "MEM-NOUVEAU", null, null, "admin");

        verify(ownerResolver).resolve(SubscriberType.MEMBER, "MEM-NOUVEAU");
    }

    // -------------------------------------------------------------------------------------
    // Acceptation
    // -------------------------------------------------------------------------------------

    @Test
    void movesThePassOnAcceptance() {
        givenPending();

        PassTransfer completed = service.accept("PTR-0001", "MEM-NOUVEAU");

        assertEquals(PassTransferStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals("MEM-NOUVEAU", pass.getOwnerCode());
        verify(eventWriter).writeEvent(eq(pass), eq(PassEventType.PASS_TRANSFERRED), any());
    }

    /** Un pass transféré ne doit pas servir à contourner ce qu'une vente directe exigerait. */
    @Test
    void checksTheRecipientMeetsThePlanKycLevel() {
        PassPlanVersion version = new PassPlanVersion();
        version.setRequiredKycLevel(3);
        pass.setPassVersion(version);
        givenPending();
        doThrow(new BadRequestException("KYC level 3 required for this pass"))
                .when(kycGuard).require(eq(3), any(), anyString());

        assertThrows(BadRequestException.class, () -> service.accept("PTR-0001", "MEM-NOUVEAU"));

        assertEquals("MEM-ANCIEN", pass.getOwnerCode());
    }

    /**
     * Le nouveau titulaire n'a jamais choisi ces personnes. Les laisser reviendrait à lui imposer
     * des gens qui consomment son pass, ce qu'il découvrirait quand son solde baisse sans qu'il
     * soit venu.
     */
    @Test
    void revokesTheBeneficiariesDesignatedByThePreviousHolder() {
        PassBeneficiary beneficiary = PassBeneficiary.builder()
                .pass(pass).beneficiaryType("MEMBER").beneficiaryCode("MEM-COLLEGUE")
                .addedAt(Instant.now().minusSeconds(86400)).build();
        when(beneficiaryRepository.findAllByPassId(1L)).thenReturn(List.of(beneficiary));
        givenPending();

        service.accept("PTR-0001", "MEM-NOUVEAU");

        assertNotNull(beneficiary.getRevokedAt());
        assertTrue(beneficiary.getRevokedReason().contains("PTR-0001"));
    }

    /**
     * Un code QR reste sur le téléphone de l'ancien titulaire après le transfert. Ne pas le révoquer
     * lui laisserait la porte ouverte sur un pass qui ne lui appartient plus.
     */
    @Test
    void revokesTheCredentialsLeftOnThePreviousHolderPhone() {
        PassCredential credential = PassCredential.builder()
                .pass(pass).value("QR-ABC").issuedAt(Instant.now().minusSeconds(86400)).build();
        when(credentialRepository.findActiveByPassId(1L)).thenReturn(List.of(credential));
        givenPending();

        service.accept("PTR-0001", "MEM-NOUVEAU");

        assertNotNull(credential.getRevokedAt());
        assertTrue(credential.getRevokedReason().contains("PTR-0001"));
    }

    /** Le pass a pu être consommé entre la demande et l'acceptation. */
    @Test
    void refusesAcceptanceIfThePassChangedStateMeanwhile() {
        givenPending();
        pass.setStatus(PassStatus.CONSUMED);

        assertThrows(BadRequestException.class, () -> service.accept("PTR-0001", "MEM-NOUVEAU"));
        assertEquals("MEM-ANCIEN", pass.getOwnerCode());
    }

    @Test
    void refusesToActOnATransferThatIsNoLongerPending() {
        PassTransfer done = transfer();
        done.setStatus(PassTransferStatus.COMPLETED);
        when(transferRepository.findByNumber("PTR-0001")).thenReturn(Optional.of(done));

        assertTrue(assertThrows(BadRequestException.class,
                () -> service.accept("PTR-0001", "MEM-NOUVEAU")).getMessage().contains("plus en attente"));
    }

    @Test
    void leavesThePassWhereItIsOnRejection() {
        givenPending();

        PassTransfer rejected = service.reject("PTR-0001", "Pas intéressé");

        assertEquals(PassTransferStatus.REJECTED, rejected.getStatus());
        assertEquals("MEM-ANCIEN", pass.getOwnerCode());
        verify(passRepository, never()).save(any());
    }

    @Test
    void letsTheSenderCancelBeforeAcceptance() {
        givenPending();

        PassTransfer cancelled = service.cancel("PTR-0001", "Erreur");

        assertEquals(PassTransferStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
    }

    // -------------------------------------------------------------------------------------

    private PassTransfer request() {
        return service.request("PASS-0001", SubscriberType.MEMBER, "MEM-NOUVEAU", "Cession", null, "admin");
    }

    private void givenPending() {
        when(transferRepository.findByNumber("PTR-0001")).thenReturn(Optional.of(transfer()));
    }

    private PassTransfer transfer() {
        return PassTransfer.builder()
                .transferNumber("PTR-0001")
                .pass(pass)
                .fromOwnerType("MEMBER")
                .fromOwnerCode("MEM-ANCIEN")
                .toOwnerType("MEMBER")
                .toOwnerCode("MEM-NOUVEAU")
                .status(PassTransferStatus.PENDING_ACCEPTANCE)
                .requestedAt(Instant.now().minusSeconds(3600))
                .build();
    }
}

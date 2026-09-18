package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.limit.service.WalletKycLevelResolver;
import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.security.enums.WalletChallengeType;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import com.sni.bokaticowork.features.payment.security.model.WalletTransactionConfirmation;
import com.sni.bokaticowork.features.payment.security.service.WalletConfirmationService;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletBeneficiaryRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletPaymentRequestRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Le transfert entre abonnés.
 *
 * <p>Ce qui est protégé ici : l'ordre des trois temps (simuler, initier, confirmer), le fait que
 * rien ne sort avant la confirmation, l'ordre fixe des verrous, les clés d'idempotence dérivées du
 * numéro, et le refus dès que le portefeuille émetteur est verrouillé par son titulaire.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletTransferServiceTest {

    @Mock private WalletTransferRepository transferRepository;
    @Mock private WalletBeneficiaryRepository beneficiaryRepository;
    @Mock private WalletPaymentRequestRepository paymentRequestRepository;
    @Mock private WalletAccountRepository walletRepository;
    @Mock private WalletLedgerService ledgerService;
    @Mock private WalletLimitService limitService;
    @Mock private WalletKycLevelResolver kycLevelResolver;
    @Mock private WalletSecurityService securityService;
    @Mock private WalletConfirmationService confirmationService;
    @Mock private WalletCounterpartyResolver counterpartyResolver;
    @Mock private WalletDeviceService deviceService;
    @Mock private WalletNotifier notifier;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private WalletTransferService service;

    private WalletAccount alice;
    private WalletAccount bob;

    @BeforeEach
    void setUp() {
        alice = wallet(1L, "WAL-ALICE", "MBR-A", "50000");
        bob = wallet(2L, "WAL-BOB", "MBR-B", "1000");

        when(counterpartyResolver.resolve(eq("WAL-BOB"), anyString()))
                .thenReturn(new WalletCounterpartyResolver.Counterparty(bob, "Bob"));
        when(kycLevelResolver.levelOf(any())).thenReturn(1);
        when(limitService.checkTransfer(any(), anyInt(), any()))
                .thenReturn(new WalletLimitService.LimitVerdict(true, null, null));
        when(securityService.evaluate(any(), eq(WalletOperationType.TRANSFER), any()))
                .thenReturn(new WalletSecurityService.SecurityVerdict(true, false, false, null));
        when(sequenceGenerator.next("wallet_transfer")).thenReturn("WTR-202609-000001");
        when(transferRepository.save(any(WalletTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(confirmationService.request(any(), any(), any(), any()))
                .thenReturn(WalletTransactionConfirmation.builder()
                        .confirmationCode("WCF-1").challengeType(WalletChallengeType.PIN)
                        .expiresAt(Instant.now().plusSeconds(300)).maxAttempts(3).build());
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(alice));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(bob));
        when(walletRepository.findById(anyLong())).thenAnswer(invocation ->
                Optional.of(((Long) invocation.getArgument(0)) == 1L ? alice : bob));
        when(ledgerService.debit(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> WalletLedgerEntry.builder().entryNumber("WLE-D").build());
        when(ledgerService.credit(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> WalletLedgerEntry.builder().entryNumber("WLE-C").build());
        when(deviceService.list(any())).thenReturn(List.of());
        service.configureFees(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "");
    }

    private WalletAccount wallet(Long id, String number, String owner, String balance) {
        return WalletAccount.builder()
                .id(id).walletNumber(number).ownerType("MEMBER").ownerCode(owner).currency("XAF")
                .status(WalletStatus.ACTIVE)
                .availableBalance(new BigDecimal(balance)).ledgerBalance(new BigDecimal(balance))
                .heldBalance(BigDecimal.ZERO)
                .notifyOnCredit(true).notifyOnDebit(true)
                .build();
    }

    private WalletTransferService.TransferOrder order(String amount) {
        return new WalletTransferService.TransferOrder("WAL-BOB", new BigDecimal(amount), "merci", null);
    }

    // ---------------------------------------------------------------------------------------
    // Simuler
    // ---------------------------------------------------------------------------------------

    @Test
    void laSimulationDitCeQuiSePasseraitSansRienEcrire() {
        WalletTransferService.TransferPreview preview = service.simulate(alice, order("10000"), null);

        assertTrue(preview.allowed());
        assertEquals(0, preview.balanceAfter().compareTo(new BigDecimal("40000")));
        assertTrue(preview.pinRequired(), "Le transfert exige toujours le code");
        verify(ledgerService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
        verify(transferRepository, never()).save(any());
        verify(confirmationService, never()).request(any(), any(), any(), any());
    }

    @Test
    void laSimulationExpliqueUnSoldeInsuffisant() {
        WalletTransferService.TransferPreview preview = service.simulate(alice, order("60000"), null);

        assertFalse(preview.allowed());
        assertTrue(preview.blockingReason().contains("insuffisant"));
        assertTrue(preview.blockingReason().contains("10000"), "Le refus dit ce qu'il manque");
    }

    @Test
    void laSimulationRelaieLeCheminDeSortieDuPlafond() {
        when(limitService.checkTransfer(any(), anyInt(), any()))
                .thenReturn(new WalletLimitService.LimitVerdict(false, "Plafond par transfert atteint", "Complétez votre dossier"));

        WalletTransferService.TransferPreview preview = service.simulate(alice, order("10000"), null);

        assertFalse(preview.allowed());
        assertEquals("Complétez votre dossier", preview.upgradePath());
    }

    @Test
    void laSimulationNeNotePasLAppareil() {
        // Une simulation n'est pas une opération · un titulaire qui hésite sur un montant ne doit pas
        // voir arriver un courriel « nouvel appareil ».
        service.simulate(alice, order("10000"), "phone-1");

        verify(deviceService, never()).touch(any(), any(), any());
    }

    // ---------------------------------------------------------------------------------------
    // Initier
    // ---------------------------------------------------------------------------------------

    @Test
    void initierPoseLeTransfertEtDemandeLeCodeSansRienDebiter() {
        WalletTransferService.InitiatedTransfer initiated = service.initiate(alice, order("10000"), "MBR-A", "1.2.3.4", "phone-1");

        assertEquals(WalletTransferStatus.PENDING_CONFIRMATION, initiated.transfer().getStatus());
        assertEquals("WCF-1", initiated.transfer().getConfirmationCode());
        assertNotNull(initiated.transfer().getTransferUuid());
        verify(ledgerService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
        verify(deviceService).touch(alice, "phone-1", "1.2.3.4");
    }

    @Test
    void initierRefuseUnPortefeuilleVerrouilleParSonTitulaire() {
        alice.setLockedByOwnerAt(Instant.now());

        ConflictException thrown = assertThrows(ConflictException.class,
                () -> service.initiate(alice, order("10000"), "MBR-A", null, null));

        assertTrue(thrown.getMessage().contains("verrouillé"));
        verify(confirmationService, never()).request(any(), any(), any(), any());
    }

    @Test
    void unDestinataireVerrouilleParSonTitulaireRecoitEncore() {
        // Le verrou du titulaire ferme la sortie, pas l'entrée : c'est lui qui s'est protégé, pas
        // l'établissement qui l'a sanctionné.
        bob.setLockedByOwnerAt(Instant.now());

        WalletTransferService.InitiatedTransfer initiated = service.initiate(alice, order("10000"), "MBR-A", null, null);

        assertEquals(WalletTransferStatus.PENDING_CONFIRMATION, initiated.transfer().getStatus());
    }

    @Test
    void unDestinataireGeleNeRecoitPas() {
        bob.setFrozenAt(Instant.now());

        assertThrows(ConflictException.class, () -> service.initiate(alice, order("10000"), "MBR-A", null, null));
    }

    @Test
    void initierRefuseLeTransfertVersSoiMeme() {
        when(counterpartyResolver.resolve(eq("WAL-ALICE"), anyString()))
                .thenReturn(new WalletCounterpartyResolver.Counterparty(alice, "Alice"));

        assertThrows(BadRequestException.class, () -> service.initiate(alice,
                new WalletTransferService.TransferOrder("WAL-ALICE", new BigDecimal("10"), null, null), "MBR-A", null, null));
    }

    @Test
    void initierRefuseAuDelaDuPlafondAvecLeMessageDuPlafond() {
        when(limitService.checkTransfer(any(), anyInt(), any()))
                .thenReturn(new WalletLimitService.LimitVerdict(false, "Plafond par transfert atteint · 50000 XAF", null));
        Mockito.doThrow(new ConflictException("wallet", "Plafond par transfert atteint · 50000 XAF"))
                .when(limitService).assertAllowed(any());

        ConflictException thrown = assertThrows(ConflictException.class,
                () -> service.initiate(alice, order("10000"), "MBR-A", null, null));

        assertTrue(thrown.getMessage().contains("50000"));
    }

    // ---------------------------------------------------------------------------------------
    // Confirmer
    // ---------------------------------------------------------------------------------------

    private WalletTransfer pending() {
        return WalletTransfer.builder()
                .id(10L).transferNumber("WTR-202609-000001").sourceWallet(alice).targetWallet(bob)
                .amount(new BigDecimal("10000.0000")).feeAmount(BigDecimal.ZERO).currency("XAF")
                .status(WalletTransferStatus.PENDING_CONFIRMATION).confirmationCode("WCF-1")
                .initiatedBy("MBR-A").expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void confirmerExecuteLesDeuxEcrituresAvecDesClesDeriveesDuNumero() {
        when(transferRepository.findByTransferNumber("WTR-202609-000001")).thenReturn(Optional.of(pending()));

        WalletTransfer done = service.confirm(alice, "WTR-202609-000001", "1357");

        assertEquals(WalletTransferStatus.COMPLETED, done.getStatus());
        assertNotNull(done.getCompletedAt());
        assertEquals("WLE-D", done.getDebitEntryNumber());
        assertEquals("WLE-C", done.getCreditEntryNumber());

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(ledgerService).debit(eq(alice), eq(new BigDecimal("10000.0000")), eq(WalletEntryType.TRANSFER_OUT),
                any(), any(), any(), any(), keys.capture());
        assertEquals("WALLET_TRANSFER_OUT:WTR-202609-000001", keys.getValue(),
                "Un rejeu doit retrouver l'écriture d'origine par sa clé, pas la refaire");
        verify(ledgerService).credit(eq(bob), eq(new BigDecimal("10000.0000")), eq(WalletEntryType.TRANSFER_IN),
                any(), any(), any(), any(), eq("WALLET_TRANSFER_IN:WTR-202609-000001"));
        verify(notifier).transferSent(any(), any(), any(), eq("WTR-202609-000001"));
        verify(notifier).transferReceived(any(), any(), any(), eq("WTR-202609-000001"));
    }

    @Test
    void lesVerrousSontPrisDansLOrdreDesIdentifiantsQuelQueSoitLeSens() {
        // Bob envoie à Alice · l'identifiant d'Alice est le plus petit, elle est verrouillée en premier.
        WalletTransfer reverse = pending();
        reverse.setSourceWallet(bob);
        reverse.setTargetWallet(alice);
        bob.setAvailableBalance(new BigDecimal("50000"));
        when(transferRepository.findByTransferNumber("WTR-202609-000001")).thenReturn(Optional.of(reverse));

        service.confirm(bob, "WTR-202609-000001", "1357");

        InOrder order = Mockito.inOrder(walletRepository);
        order.verify(walletRepository).findByIdForUpdate(1L);
        order.verify(walletRepository).findByIdForUpdate(2L);
    }

    @Test
    void lEmpreinteEstVerifieeAvantLeCode() {
        // L'ordre appartient au service de confirmation ; ici on vérifie seulement qu'il est appelé
        // avec l'opération réellement enregistrée · montant, devise, destinataire.
        when(transferRepository.findByTransferNumber("WTR-202609-000001")).thenReturn(Optional.of(pending()));

        service.confirm(alice, "WTR-202609-000001", "1357");

        ArgumentCaptor<WalletConfirmationService.OperationToConfirm> operation =
                ArgumentCaptor.forClass(WalletConfirmationService.OperationToConfirm.class);
        verify(confirmationService).confirm(eq("WCF-1"), operation.capture(), eq("1357"));
        assertEquals("WAL-BOB", operation.getValue().counterpartyCode());
        assertEquals(0, operation.getValue().amount().compareTo(new BigDecimal("10000")));
    }

    @Test
    void unCodeFauxNeDebiteRien() {
        when(transferRepository.findByTransferNumber("WTR-202609-000001")).thenReturn(Optional.of(pending()));
        Mockito.doThrow(new BadRequestException("Code secret incorrect"))
                .when(confirmationService).confirm(any(), any(), any());

        assertThrows(BadRequestException.class, () -> service.confirm(alice, "WTR-202609-000001", "0000"));

        verify(ledgerService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void unEchecALExecutionEstGardeAvecSaRaison() {
        WalletTransfer transfer = pending();
        when(transferRepository.findByTransferNumber("WTR-202609-000001")).thenReturn(Optional.of(transfer));
        when(ledgerService.debit(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException("Insufficient wallet balance"));

        assertThrows(BadRequestException.class, () -> service.confirm(alice, "WTR-202609-000001", "1357"));

        assertEquals(WalletTransferStatus.FAILED, transfer.getStatus());
        assertEquals("Insufficient wallet balance", transfer.getFailureReason(),
                "Le titulaire doit retrouver ce transfert dans son historique avec la raison");
    }

    @Test
    void unTransfertEchuNeSeConfirmePlus() {
        WalletTransfer stale = pending();
        stale.setExpiresAt(Instant.now().minusSeconds(1));
        when(transferRepository.findByTransferNumber("WTR-202609-000001")).thenReturn(Optional.of(stale));

        assertThrows(BadRequestException.class, () -> service.confirm(alice, "WTR-202609-000001", "1357"));

        assertEquals(WalletTransferStatus.CANCELLED, stale.getStatus());
        verify(confirmationService, never()).confirm(any(), any(), any());
    }

    @Test
    void confirmerLeTransfertDUnAutreEstIntrouvablePasInterdit() {
        when(transferRepository.findByTransferNumber("WTR-202609-000001")).thenReturn(Optional.of(pending()));

        assertThrows(com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException.class,
                () -> service.confirm(bob, "WTR-202609-000001", "1357"));
    }

    // ---------------------------------------------------------------------------------------
    // Frais
    // ---------------------------------------------------------------------------------------

    @Test
    void sansPortefeuilleDeFraisAucunFraisNEstPreleve() {
        // Prélever de l'argent qui n'atterrit nulle part creuserait un écart au rapprochement.
        service.configureFees(new BigDecimal("0.01"), BigDecimal.ZERO, BigDecimal.ZERO, "");

        WalletTransferService.TransferPreview preview = service.simulate(alice, order("10000"), null);

        assertEquals(0, preview.fee().compareTo(BigDecimal.ZERO));
    }

    @Test
    void lesFraisSontBornesEtVersesAuPortefeuilleDeFrais() {
        WalletAccount house = wallet(99L, "WAL-HOUSE", "HOUSE", "0");
        when(walletRepository.findByWalletNumber("WAL-HOUSE")).thenReturn(Optional.of(house));
        service.configureFees(new BigDecimal("0.01"), new BigDecimal("200"), new BigDecimal("500"), "WAL-HOUSE");

        assertEquals(0, service.simulate(alice, order("10000"), null).fee().compareTo(new BigDecimal("200")),
                "1 % de 10 000 vaut 100, relevé au minimum de 200");
        assertEquals(0, service.simulate(alice, order("40000"), null).fee().compareTo(new BigDecimal("400")));
        alice.setAvailableBalance(new BigDecimal("100000"));
        assertEquals(0, service.simulate(alice, order("90000"), null).fee().compareTo(new BigDecimal("500")),
                "1 % de 90 000 vaut 900, ramené au maximum de 500");
    }
}

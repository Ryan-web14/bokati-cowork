package com.sni.bokaticowork.features.payment.control.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.control.model.WalletAdminAction;
import com.sni.bokaticowork.features.payment.control.repository.WalletAdminActionRepository;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.limit.repository.WalletLimitPolicyRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les actions administratives.
 *
 * <p>Deux principes, et les tests les prennent au mot : aucune action sans motif, et un second visa
 * sur les opérations sensibles · pendant lequel rien n'est exécuté, et que le demandeur ne peut pas
 * se donner à lui-même.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletAdminActionServiceTest {

    @Mock private WalletAdminActionRepository actionRepository;
    @Mock private WalletAccountRepository walletRepository;
    @Mock private WalletLedgerEntryRepository ledgerRepository;
    @Mock private WalletLedgerService ledgerService;
    @Mock private WalletSecurityService securityService;
    @Mock private WalletLimitPolicyRepository limitPolicyRepository;
    @Mock private WalletNotifier notifier;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private WalletAdminActionService service;

    private WalletAccount wallet;

    @BeforeEach
    void setUp() {
        wallet = WalletAccount.builder()
                .id(1L).walletNumber("WAL-1").ownerType("MEMBER").ownerCode("MBR-1").currency("XAF")
                .status(WalletStatus.ACTIVE)
                .availableBalance(new BigDecimal("50000")).ledgerBalance(new BigDecimal("50000")).heldBalance(BigDecimal.ZERO)
                .build();
        when(walletRepository.findByWalletNumber("WAL-1")).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next("wallet_admin_action")).thenReturn("WAA-202609-000001");
        when(actionRepository.save(any(WalletAdminAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerService.credit(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> WalletLedgerEntry.builder().entryNumber("WLE-C").build());
        when(ledgerService.debit(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> WalletLedgerEntry.builder().entryNumber("WLE-D").build());
        service.configureThreshold(new BigDecimal("100000"));
    }

    private WalletAdminActionService.ActionRequest request(WalletAdminAction.Type type, String amount, String reference) {
        return new WalletAdminActionService.ActionRequest(type,
                amount == null ? null : new BigDecimal(amount), reference, "Régularisation demandée par le client", null);
    }

    // ---------------------------------------------------------------------------------------

    @Test
    void aucuneActionSansMotif() {
        WalletAdminActionService.ActionRequest sansMotif = new WalletAdminActionService.ActionRequest(
                WalletAdminAction.Type.SUSPEND, null, null, "  ", null);

        assertThrows(BadRequestException.class, () -> service.request("WAL-1", sansMotif, "alice"));
        verify(actionRepository, never()).save(any());
    }

    @Test
    void unPetitCreditSExecuteSeul() {
        WalletAdminAction action = service.request("WAL-1", request(WalletAdminAction.Type.TOPUP, "5000", null), "alice");

        assertEquals(WalletAdminAction.Status.EXECUTED, action.getStatus());
        assertEquals("WLE-C", action.getResultReference());
        verify(ledgerService).credit(eq(wallet), eq(new BigDecimal("5000.0000")), eq(WalletEntryType.ADMIN_TOPUP),
                any(), eq("WAA-202609-000001"), any(), eq("alice"), eq("ADMIN_ACTION:WAA-202609-000001"));
    }

    @Test
    void unGrosDebitAttendLeSecondVisaSansRienExecuter() {
        WalletAdminAction action = service.request("WAL-1", request(WalletAdminAction.Type.DEBIT, "150000", null), "alice");

        assertEquals(WalletAdminAction.Status.PENDING_APPROVAL, action.getStatus());
        assertNull(action.getExecutedAt());
        verify(ledgerService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void leDemandeurNePeutPasSeDonnerLeSecondVisa() {
        WalletAdminAction pending = service.request("WAL-1", request(WalletAdminAction.Type.DEBIT, "150000", null), "alice");
        when(actionRepository.findByActionNumber("WAA-202609-000001")).thenReturn(Optional.of(pending));

        assertThrows(ConflictException.class, () -> service.approve("WAA-202609-000001", "alice"));
        assertThrows(ConflictException.class, () -> service.approve("WAA-202609-000001", "ALICE"),
                "La casse ne fait pas une autre personne");
        verify(ledgerService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void leSecondVisaDUnAutreExecuteAuNomDeCelUiQuiVise() {
        WalletAdminAction pending = service.request("WAL-1", request(WalletAdminAction.Type.DEBIT, "150000", null), "alice");
        when(actionRepository.findByActionNumber("WAA-202609-000001")).thenReturn(Optional.of(pending));
        wallet.setAvailableBalance(new BigDecimal("200000"));
        wallet.setLedgerBalance(new BigDecimal("200000"));

        WalletAdminAction approved = service.approve("WAA-202609-000001", "bob");

        assertEquals(WalletAdminAction.Status.EXECUTED, approved.getStatus());
        assertEquals("bob", approved.getApprovedBy());
        assertNotNull(approved.getExecutedAt());
        verify(ledgerService).debit(eq(wallet), eq(new BigDecimal("150000.0000")), eq(WalletEntryType.ADMIN_DEBIT),
                any(), any(), any(), eq("bob"), any());
    }

    @Test
    void unRefusGardeLaTraceEtNExecuteRien() {
        WalletAdminAction pending = service.request("WAL-1", request(WalletAdminAction.Type.DEBIT, "150000", null), "alice");
        when(actionRepository.findByActionNumber("WAA-202609-000001")).thenReturn(Optional.of(pending));

        WalletAdminAction rejected = service.reject("WAA-202609-000001", "bob", "Montant non justifié");

        assertEquals(WalletAdminAction.Status.REJECTED, rejected.getStatus());
        assertEquals("Montant non justifié", rejected.getRejectionReason());
        verify(ledgerService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void uneContrePassationExigeToujoursLeSecondVisa() {
        WalletAdminAction action = service.request("WAL-1", request(WalletAdminAction.Type.REVERSE, null, "WTX-1"), "alice");

        assertEquals(WalletAdminAction.Status.PENDING_APPROVAL, action.getStatus(),
                "Annuler une écriture, même petite, n'est jamais un geste solitaire");
    }

    @Test
    void laContrePassationEstOpposeeEtCleeSurLEcritureDOrigine() {
        WalletLedgerEntry original = WalletLedgerEntry.builder()
                .transactionNumber("WTX-1").wallet(wallet).direction(WalletEntryDirection.CREDIT)
                .amount(new BigDecimal("7000")).entryType(WalletEntryType.ADMIN_TOPUP).build();
        when(ledgerRepository.findByTransactionNumber("WTX-1")).thenReturn(Optional.of(original));
        WalletAdminAction pending = service.request("WAL-1", request(WalletAdminAction.Type.REVERSE, null, "WTX-1"), "alice");
        when(actionRepository.findByActionNumber("WAA-202609-000001")).thenReturn(Optional.of(pending));

        service.approve("WAA-202609-000001", "bob");

        // Un crédit se contre-passe par un débit, et la clé vient de l'écriture d'origine : deux
        // actions qui voudraient annuler la même écriture retombent sur la même contre-passation.
        verify(ledgerService).debit(eq(wallet), eq(new BigDecimal("7000")), eq(WalletEntryType.REVERSAL),
                any(), any(), any(), eq("bob"), eq("REVERSAL:WTX-1"));
    }

    @Test
    void uneRetenueNeSeContrePassePas() {
        WalletLedgerEntry hold = WalletLedgerEntry.builder()
                .transactionNumber("WTX-H").wallet(wallet).direction(WalletEntryDirection.DEBIT)
                .amount(new BigDecimal("7000")).entryType(WalletEntryType.HOLD).build();
        when(ledgerRepository.findByTransactionNumber("WTX-H")).thenReturn(Optional.of(hold));
        WalletAdminAction pending = service.request("WAL-1", request(WalletAdminAction.Type.REVERSE, null, "WTX-H"), "alice");
        when(actionRepository.findByActionNumber("WAA-202609-000001")).thenReturn(Optional.of(pending));

        assertThrows(BadRequestException.class, () -> service.approve("WAA-202609-000001", "bob"));
    }

    @Test
    void suspendreGeleEtPrevient() {
        WalletAdminAction action = service.request("WAL-1", request(WalletAdminAction.Type.SUSPEND, null, null), "alice");

        assertEquals(WalletAdminAction.Status.EXECUTED, action.getStatus());
        assertNotNull(wallet.getFrozenAt());
        assertEquals(WalletStatus.SUSPENDED, wallet.getStatus());
        verify(notifier).securityEvent(eq(wallet), eq("WALLET_SUSPENDED"), any(), any());
    }

    @Test
    void uneDerogationDePlafondEstBorneeDansLeTemps() {
        when(limitPolicyRepository.findByCode("WLP-VIP")).thenReturn(Optional.of(
                com.sni.bokaticowork.features.payment.limit.model.WalletLimitPolicy.builder().code("WLP-VIP").build()));
        WalletAdminActionService.ActionRequest sansTerme = new WalletAdminActionService.ActionRequest(
                WalletAdminAction.Type.RAISE_LIMIT, null, "WLP-VIP", "Client grand compte", null);

        assertThrows(BadRequestException.class, () -> service.request("WAL-1", sansTerme, "alice"));

        Instant until = Instant.now().plusSeconds(86400L * 30);
        service.request("WAL-1", new WalletAdminActionService.ActionRequest(
                WalletAdminAction.Type.RAISE_LIMIT, null, "WLP-VIP", "Client grand compte", until), "alice");
        assertEquals("WLP-VIP", wallet.getLimitPolicyCode());
        assertEquals(until, wallet.getLimitPolicyUntil());
    }

    @Test
    void cloturerAvecSoldeExigeUneReferenceDeRestitutionEtUnSecondVisa() {
        WalletAdminAction pending = service.request("WAL-1", request(WalletAdminAction.Type.CLOSE, null, null), "alice");
        assertEquals(WalletAdminAction.Status.PENDING_APPROVAL, pending.getStatus());
        when(actionRepository.findByActionNumber("WAA-202609-000001")).thenReturn(Optional.of(pending));

        ConflictException thrown = assertThrows(ConflictException.class, () -> service.approve("WAA-202609-000001", "bob"));
        assertTrue(thrown.getMessage().contains("restitution"), "Un solde ne disparaît pas · il faut dire par où il est reparti");
    }

    @Test
    void cloturerAvecSoldeEtReferenceRestitueThenFerme() {
        WalletAdminAction pending = service.request("WAL-1", request(WalletAdminAction.Type.CLOSE, null, "MM-REFUND-42"), "alice");
        when(actionRepository.findByActionNumber("WAA-202609-000001")).thenReturn(Optional.of(pending));

        service.approve("WAA-202609-000001", "bob");

        verify(ledgerService).debit(eq(wallet), eq(new BigDecimal("50000")), eq(WalletEntryType.ADMIN_DEBIT),
                any(), any(), org.mockito.ArgumentMatchers.contains("MM-REFUND-42"), eq("bob"), any());
        assertEquals(WalletStatus.CLOSED, wallet.getStatus());
        assertNotNull(wallet.getClosedAt());
    }

    @Test
    void cloturerAVideSExecuteSeul() {
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setLedgerBalance(BigDecimal.ZERO);

        WalletAdminAction action = service.request("WAL-1", request(WalletAdminAction.Type.CLOSE, null, null), "alice");

        assertEquals(WalletAdminAction.Status.EXECUTED, action.getStatus());
        assertEquals(WalletStatus.CLOSED, wallet.getStatus());
        verify(ledgerService, never()).debit(any(), any(), any(), any(), any(), any(), any(), any());
    }
}

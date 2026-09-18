package com.sni.bokaticowork.features.payment.limit.service;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.limit.model.WalletLimitPolicy;
import com.sni.bokaticowork.features.payment.limit.repository.WalletLimitPolicyRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Les plafonds du portefeuille.
 *
 * <p>Ce que ces tests protègent tient en deux phrases. Un plafond absent n'est pas un plafond à
 * zéro : confondre les deux bloquerait tout au niveau le plus élevé, précisément celui où l'on veut
 * laisser passer. Et un refus doit dire ce qu'il faudrait pour passer, sans quoi le titulaire ne
 * sait pas s'il doit attendre demain, fournir une pièce, ou renoncer.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletLimitServiceTest {

    @Mock
    private WalletLimitPolicyRepository policyRepository;

    @Mock
    private WalletLedgerEntryRepository ledgerRepository;

    @InjectMocks
    private WalletLimitService limitService;

    private WalletAccount wallet;

    @BeforeEach
    void setUp() {
        wallet = WalletAccount.builder()
                .id(1L)
                .walletNumber("WAL-0001")
                .ownerType("MEMBER")
                .ownerCode("MBR-1")
                .currency("XAF")
                .availableBalance(new BigDecimal("10000"))
                .ledgerBalance(new BigDecimal("10000"))
                .heldBalance(BigDecimal.ZERO)
                .build();

        when(ledgerRepository.sumSince(anyLong(), anyString(), any(Instant.class))).thenReturn(BigDecimal.ZERO);
        when(ledgerRepository.countDebitsSince(anyLong(), any(Instant.class))).thenReturn(0L);
        when(policyRepository.findForKycLevel(anyInt())).thenReturn(Optional.empty());
    }

    private WalletLimitPolicy level(int kycLevel, String singleTransfer, String dailyTransfer) {
        return WalletLimitPolicy.builder()
                .code("WLP-" + kycLevel)
                .name("Niveau " + kycLevel)
                .kycLevel(kycLevel)
                .currency("XAF")
                .maxSingleTransfer(singleTransfer == null ? null : new BigDecimal(singleTransfer))
                .maxDailyTransfer(dailyTransfer == null ? null : new BigDecimal(dailyTransfer))
                .build();
    }

    // ---------------------------------------------------------------------------------------

    @Test
    void sansAucunePolitiqueRienNestBloque() {
        WalletLimitService.LimitVerdict verdict =
                limitService.checkTransfer(wallet, 1, new BigDecimal("999999999"));

        assertTrue(verdict.allowed(), "Sans palier configuré, un montant n'a aucune raison d'être refusé");
    }

    @Test
    void leMontantUnitaireAuDelaDuPlafondEstRefuse() {
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", "100000")));

        WalletLimitService.LimitVerdict verdict =
                limitService.checkTransfer(wallet, 1, new BigDecimal("50001"));

        assertFalse(verdict.allowed());
        assertTrue(verdict.reason().contains("50000"),
                "Le refus doit citer le plafond, pas seulement le constater");
    }

    @Test
    void leMontantEgalAuPlafondPasse() {
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", "100000")));

        assertTrue(limitService.checkTransfer(wallet, 1, new BigDecimal("50000")).allowed(),
                "Le plafond est une borne incluse · le refuser rendrait le chiffre affiché faux");
    }

    @Test
    void unPlafondAbsentNestPasUnPlafondAZero() {
        // Le niveau le plus élevé laisse volontairement des plafonds nuls. Les lire comme des zéros
        // bloquerait tout précisément là où l'on veut laisser passer.
        when(policyRepository.findForKycLevel(3)).thenReturn(Optional.of(level(3, null, null)));

        assertTrue(limitService.checkTransfer(wallet, 3, new BigDecimal("5000000")).allowed());
    }

    @Test
    void leCumulDuJourSAjouteAuMontantDemande() {
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", "100000")));
        when(ledgerRepository.sumSince(anyLong(), anyString(), any(Instant.class)))
                .thenReturn(new BigDecimal("80000"));

        WalletLimitService.LimitVerdict verdict =
                limitService.checkTransfer(wallet, 1, new BigDecimal("30000"));

        assertFalse(verdict.allowed(), "80 000 déjà envoyés plus 30 000 dépassent le plafond journalier");
        assertTrue(verdict.reason().contains("jour"));
    }

    @Test
    void leRefusIndiqueCommentLeverLaLimite() {
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", "100000")));
        when(policyRepository.findForKycLevel(2)).thenReturn(Optional.of(level(2, "300000", "500000")));

        WalletLimitService.LimitVerdict verdict =
                limitService.checkTransfer(wallet, 1, new BigDecimal("60000"));

        assertNotNull(verdict.upgradePath(),
                "Un refus muet laisse le titulaire sans savoir s'il doit attendre, fournir une pièce ou renoncer");
        assertTrue(verdict.upgradePath().contains("Niveau 2"));
    }

    @Test
    void auDernierNiveauLeRefusNePrometRien() {
        when(policyRepository.findForKycLevel(3)).thenReturn(Optional.of(level(3, "1000000", null)));
        when(policyRepository.findForKycLevel(4)).thenReturn(Optional.empty());

        WalletLimitService.LimitVerdict verdict =
                limitService.checkTransfer(wallet, 3, new BigDecimal("2000000"));

        assertFalse(verdict.allowed());
        assertNull(verdict.upgradePath(), "Promettre un niveau supérieur qui n'existe pas serait mentir");
    }

    @Test
    void unePolitiqueNommeeSurLePortefeuilleLEmporteSurLePalier() {
        wallet.setLimitPolicyCode("WLP-DEROGATION");
        when(policyRepository.findByCode("WLP-DEROGATION"))
                .thenReturn(Optional.of(level(9, "5000000", null)));
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", "100000")));

        assertTrue(limitService.checkTransfer(wallet, 1, new BigDecimal("4000000")).allowed(),
                "Une dérogation nominative doit pouvoir ouvrir un plafond sans l'ouvrir à tout le monde");
    }

    @Test
    void unePolitiqueNommeeIntrouvableRetombeSurLePalier() {
        // Un code qui ne désigne plus rien ne doit pas ouvrir les vannes : on retombe sur le palier,
        // qui est le régime le plus strict des deux.
        wallet.setLimitPolicyCode("WLP-DISPARUE");
        when(policyRepository.findByCode("WLP-DISPARUE")).thenReturn(Optional.empty());
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", "100000")));

        assertFalse(limitService.checkTransfer(wallet, 1, new BigDecimal("4000000")).allowed());
    }

    @Test
    void leNombreDOperationsQuotidiennesEstUnPlafondAPart() {
        WalletLimitPolicy policy = level(1, "50000", "100000");
        policy.setMaxDailyOperations(3);
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(policy));
        when(ledgerRepository.countDebitsSince(anyLong(), any(Instant.class))).thenReturn(3L);

        WalletLimitService.LimitVerdict verdict =
                limitService.checkTransfer(wallet, 1, new BigDecimal("100"));

        assertFalse(verdict.allowed(), "Un petit montant reste refusé si le nombre d'opérations est atteint");
        assertTrue(verdict.reason().contains("opérations"));
    }

    @Test
    void leSoldeMaximalTientCompteDuSoldeDejaPresent() {
        WalletLimitPolicy policy = level(1, null, null);
        policy.setMaxBalance(new BigDecimal("15000"));
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(policy));

        assertFalse(limitService.checkTopUp(wallet, 1, new BigDecimal("6000")).allowed(),
                "10 000 en caisse plus 6 000 dépassent un plafond de solde de 15 000");
        assertTrue(limitService.checkTopUp(wallet, 1, new BigDecimal("5000")).allowed());
    }

    @Test
    void lExceptionPorteLaRaisonEtLeCheminDeSortie() {
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", null)));
        when(policyRepository.findForKycLevel(2)).thenReturn(Optional.of(level(2, "300000", null)));

        WalletLimitService.LimitVerdict verdict =
                limitService.checkTransfer(wallet, 1, new BigDecimal("60000"));

        ConflictException thrown = assertThrows(ConflictException.class,
                () -> limitService.assertAllowed(verdict));
        assertTrue(thrown.getMessage().contains("50000"));
        assertTrue(thrown.getMessage().contains("Niveau 2"));
    }

    @Test
    void lEtatDesPlafondsNeRenvoieJamaisDeResteNegatif() {
        when(policyRepository.findForKycLevel(1)).thenReturn(Optional.of(level(1, "50000", "100000")));
        when(ledgerRepository.sumSince(anyLong(), anyString(), any(Instant.class)))
                .thenReturn(new BigDecimal("130000"));

        WalletLimitService.LimitSnapshot snapshot = limitService.snapshot(wallet, 1);

        assertEquals(0, snapshot.remainingDailyTransfer().compareTo(BigDecimal.ZERO),
                "Un dépassement affiche zéro restant, pas un reste négatif");
    }

    @Test
    void lEtatDesPlafondsDistingueLAbsenceDeLimiteDuZero() {
        when(policyRepository.findForKycLevel(3)).thenReturn(Optional.of(level(3, null, null)));

        WalletLimitService.LimitSnapshot snapshot = limitService.snapshot(wallet, 3);

        assertNull(snapshot.remainingDailyTransfer(),
                "Sans plafond, le reste est indéfini · zéro se lirait comme « plus rien »");
    }
}

package com.sni.bokaticowork.features.payment.compliance.detector;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletDeviceRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Les détecteurs lisent une règle · ils n'en portent aucune.
 *
 * <p>Chaque test change un seuil dans la règle et vérifie que le détecteur suit. C'est le contrat :
 * un seuil qui se change par déploiement ne se change jamais.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComplianceDetectorsTest {

    @Mock private WalletLedgerEntryRepository ledgerRepository;
    @Mock private WalletTransferRepository transferRepository;
    @Mock private WalletDeviceRepository deviceRepository;
    @Mock private WalletRiskFlagRepository flagRepository;

    private final ComplianceDetectors detectors = new ComplianceDetectors();
    private final Instant now = Instant.parse("2026-09-18T12:00:00Z");
    private WalletAccount alice;
    private WalletAccount bob;
    private WalletAccount carol;

    @BeforeEach
    void setUp() {
        alice = wallet(1L, "WAL-A");
        bob = wallet(2L, "WAL-B");
        carol = wallet(3L, "WAL-C");
        when(ledgerRepository.findBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(transferRepository.findCompletedFromSince(anyLong(), any())).thenReturn(List.of());
        when(transferRepository.findCompletedToSince(anyLong(), any())).thenReturn(List.of());
        when(flagRepository.countByWallet_IdAndFlagTypeAndDetectedAtAfter(anyLong(), any(), any())).thenReturn(0L);
        when(flagRepository.findFirstByWallet_IdAndFlagTypeOrderByDetectedAtDesc(anyLong(), any())).thenReturn(Optional.empty());
    }

    private WalletAccount wallet(Long id, String number) {
        return WalletAccount.builder().id(id).walletNumber(number).currency("XAF").build();
    }

    private ComplianceRule rule(String detector, String amount, Integer count, String ratio, int windowMinutes) {
        return ComplianceRule.builder().ruleCode("CR-" + detector).name(detector).detector(detector)
                .amountThreshold(amount == null ? null : new BigDecimal(amount)).countThreshold(count)
                .ratioThreshold(ratio == null ? null : new BigDecimal(ratio)).windowMinutes(windowMinutes)
                .severity(ComplianceRule.Severity.HIGH).action(ComplianceRule.Action.FLAG).build();
    }

    private DetectionContext context(ComplianceRule rule, String deviceId) {
        return new DetectionContext(alice, rule, now, deviceId, ledgerRepository, transferRepository, deviceRepository, flagRepository);
    }

    private WalletLedgerEntry entry(WalletEntryType type, WalletEntryDirection direction, String amount, int minutesAgo) {
        return WalletLedgerEntry.builder().wallet(alice).entryType(type).direction(direction)
                .amount(new BigDecimal(amount)).createdAt(now.minusSeconds(minutesAgo * 60L)).transactionNumber("WTX-" + minutesAgo).build();
    }

    private WalletTransfer transfer(WalletAccount from, WalletAccount to, String amount) {
        return WalletTransfer.builder().transferNumber("WTR-" + from.getId() + "-" + to.getId()).sourceWallet(from)
                .targetWallet(to).amount(new BigDecimal(amount)).status(WalletTransferStatus.COMPLETED).completedAt(now).build();
    }

    // ---------------------------------------------------------------------------------------

    @Test
    void laVelociteEnNombreSuitLeSeuilDeLaRegle() {
        when(ledgerRepository.findBetween(anyLong(), any(), any())).thenReturn(List.of(
                entry(WalletEntryType.PAYMENT, WalletEntryDirection.DEBIT, "100", 5),
                entry(WalletEntryType.TRANSFER_OUT, WalletEntryDirection.DEBIT, "100", 10),
                entry(WalletEntryType.HOLD, WalletEntryDirection.DEBIT, "100", 12),
                entry(WalletEntryType.TOPUP, WalletEntryDirection.CREDIT, "500", 20)));

        assertTrue(detectors.run("VELOCITY_COUNT", context(rule("VELOCITY_COUNT", null, 2, null, 60), null)).isPresent());
        assertFalse(detectors.run("VELOCITY_COUNT", context(rule("VELOCITY_COUNT", null, 3, null, 60), null)).isPresent(),
                "Une retenue n'est pas une sortie · deux sorties, seuil à trois, rien");
    }

    @Test
    void laVelociteEnMontantAdditionneLesSorties() {
        when(ledgerRepository.findBetween(anyLong(), any(), any())).thenReturn(List.of(
                entry(WalletEntryType.TRANSFER_OUT, WalletEntryDirection.DEBIT, "600000", 5),
                entry(WalletEntryType.PAYMENT, WalletEntryDirection.DEBIT, "500000", 30)));

        Optional<ComplianceDetectors.Detection> detection =
                detectors.run("VELOCITY_AMOUNT", context(rule("VELOCITY_AMOUNT", "1000000", null, null, 1440), null));

        assertTrue(detection.isPresent());
        assertTrue(detection.get().details().contains("1100000"));
    }

    @Test
    void leFractionnementCompteLesMontantsJusteSousLeSeuil() {
        when(transferRepository.findCompletedFromSince(eq(1L), any())).thenReturn(List.of(
                transfer(alice, bob, "290000"), transfer(alice, bob, "285000"), transfer(alice, carol, "295000"),
                transfer(alice, bob, "50000")));

        Optional<ComplianceDetectors.Detection> detection =
                detectors.run("STRUCTURING", context(rule("STRUCTURING", "300000", 3, "0.8", 1440), null));

        assertTrue(detection.isPresent());
        assertEquals(WalletRiskFlag.Type.STRUCTURING, detection.get().flagType());
        assertTrue(detection.get().details().startsWith("3 transferts"), "Le petit de 50 000 ne compte pas");
    }

    @Test
    void lAllerRetourExigeLEntreeAvantLaSortie() {
        // Entrée de 100 000 à t-30, sortie de 90 000 à t-10 · un tuyau.
        when(ledgerRepository.findBetween(anyLong(), any(), any())).thenReturn(List.of(
                entry(WalletEntryType.TOPUP, WalletEntryDirection.CREDIT, "100000", 30),
                entry(WalletEntryType.TRANSFER_OUT, WalletEntryDirection.DEBIT, "90000", 10)));
        assertTrue(detectors.run("RAPID_IN_OUT", context(rule("RAPID_IN_OUT", "50000", null, "0.8", 120), null)).isPresent());

        // Sortie avant l'entrée · ce n'est pas un aller-retour.
        when(ledgerRepository.findBetween(anyLong(), any(), any())).thenReturn(List.of(
                entry(WalletEntryType.TRANSFER_OUT, WalletEntryDirection.DEBIT, "90000", 30),
                entry(WalletEntryType.TOPUP, WalletEntryDirection.CREDIT, "100000", 10)));
        assertFalse(detectors.run("RAPID_IN_OUT", context(rule("RAPID_IN_OUT", "50000", null, "0.8", 120), null)).isPresent());
    }

    @Test
    void lArrosageCompteLesDestinatairesDistincts() {
        when(transferRepository.findCompletedFromSince(eq(1L), any())).thenReturn(List.of(
                transfer(alice, bob, "10"), transfer(alice, bob, "10"), transfer(alice, carol, "10")));

        assertTrue(detectors.run("FAN_OUT", context(rule("FAN_OUT", null, 2, null, 1440), null)).isPresent());
        assertFalse(detectors.run("FAN_OUT", context(rule("FAN_OUT", null, 3, null, 1440), null)).isPresent(),
                "Deux destinataires distincts, pas trois · les envois répétés au même ne comptent qu'une fois");
    }

    @Test
    void laCirculariteDetecteUnCycleATrois() {
        when(transferRepository.findCompletedFromSince(eq(1L), any())).thenReturn(List.of(transfer(alice, bob, "10")));
        when(transferRepository.findCompletedFromSince(eq(2L), any())).thenReturn(List.of(transfer(bob, carol, "10")));
        when(transferRepository.findCompletedFromSince(eq(3L), any())).thenReturn(List.of(transfer(carol, alice, "10")));

        Optional<ComplianceDetectors.Detection> detection =
                detectors.run("CIRCULARITY", context(rule("CIRCULARITY", null, null, null, 4320), null));

        assertTrue(detection.isPresent());
        assertTrue(detection.get().details().contains("WAL-A → WAL-B → WAL-C → WAL-A"));
    }

    @Test
    void unAllerRetourADeuxNEstPasUneCircularite() {
        // A → B → A : un remboursement entre amis, rien de plus.
        when(transferRepository.findCompletedFromSince(eq(1L), any())).thenReturn(List.of(transfer(alice, bob, "10")));
        when(transferRepository.findCompletedFromSince(eq(2L), any())).thenReturn(List.of(transfer(bob, alice, "10")));

        assertFalse(detectors.run("CIRCULARITY", context(rule("CIRCULARITY", null, null, null, 4320), null)).isPresent());
    }

    @Test
    void lAppareilPartageSeMesureSurLAppareilDeclencheur() {
        when(deviceRepository.countDistinctWalletsUsingDevice(eq("phone-x"), any())).thenReturn(4L);

        assertTrue(detectors.run("SHARED_DEVICE", context(rule("SHARED_DEVICE", null, 3, null, 43200), "phone-x")).isPresent());
        assertFalse(detectors.run("SHARED_DEVICE", context(rule("SHARED_DEVICE", null, 3, null, 43200), null)).isPresent(),
                "Sans appareil déclencheur, rien à mesurer");
    }

    @Test
    void unDetecteurInconnuNeParleJamais() {
        assertFalse(detectors.knows("TELEPATHY"));
        assertFalse(detectors.run("TELEPATHY", context(rule("TELEPATHY", null, null, null, 60), null)).isPresent());
    }
}

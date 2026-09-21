package com.sni.bokaticowork.features.payment.control.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.control.model.WalletTreasuryReconciliation;
import com.sni.bokaticowork.features.payment.control.repository.WalletTreasuryReconciliationRepository;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rapprochement avec la trésorerie.
 *
 * <p>Ce qui compte : une trésorerie insuffisante est {@code UNDER_REVIEW} quoi qu'on ait expliqué
 * · une explication justifie un écart comptable, pas de l'argent qui manque · et la direction en est
 * alertée, pas seulement informée dans un rapport.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletTreasuryReconciliationServiceTest {

    @Mock private WalletTreasuryReconciliationRepository reconciliationRepository;
    @Mock private WalletAccountRepository walletRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private OutboxService outboxService;

    @InjectMocks
    private WalletTreasuryReconciliationService service;

    private static final LocalDate DAY = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setUp() {
        // Encours de 1 000 000, dont 50 000 retenus, sur 120 portefeuilles.
        when(walletRepository.aggregateOpen("XAF"))
                .thenReturn(new Object[]{new BigDecimal("1000000.0000"), new BigDecimal("50000.0000"), 120L});
        when(reconciliationRepository.findByReconciliationDateAndCurrency(any(), anyString())).thenReturn(Optional.empty());
        when(reconciliationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next("wallet_treasury_reconciliation")).thenReturn("WTS-202609-000001");
        ReflectionTestUtils.setField(service, "coverageAlertRatio", new BigDecimal("1.0"));
        ReflectionTestUtils.setField(service, "varianceTolerance", new BigDecimal("1"));
        ReflectionTestUtils.setField(service, "alertEmail", "direction@example.test");
    }

    private WalletTreasuryReconciliationService.Inputs inputs(String ledger, String cash, String explanation) {
        return new WalletTreasuryReconciliationService.Inputs(DAY, "XAF",
                ledger == null ? null : new BigDecimal(ledger), new BigDecimal(cash), explanation);
    }

    @Test
    void laTresorerieDisponibleEstObligatoire() {
        assertThrows(BadRequestException.class, () -> service.reconcile(
                new WalletTreasuryReconciliationService.Inputs(DAY, "XAF", null, null, null), "cfo"));
    }

    @Test
    void toutConcordeQuandLaTresorerieCouvreEtLeCompteEgaleLEncours() {
        WalletTreasuryReconciliation result = service.reconcile(inputs("1000000", "1500000", null), "cfo");

        assertEquals(WalletTreasuryReconciliation.Status.BALANCED, result.getStatus());
        assertEquals(0, result.getCoverageRatio().compareTo(new BigDecimal("1.5")));
        assertEquals(0, result.getVariance().compareTo(BigDecimal.ZERO));
        verify(outboxService, never()).publish(any(), any(), any(), any());
    }

    @Test
    void uneTresorerieInsuffisanteAlerteLaDirection() {
        WalletTreasuryReconciliation result = service.reconcile(inputs("1000000", "600000", null), "cfo");

        assertEquals(WalletTreasuryReconciliation.Status.UNDER_REVIEW, result.getStatus());
        assertEquals(0, result.getCoverageRatio().compareTo(new BigDecimal("0.6")));
        verify(outboxService).publish(eq("WALLET_TREASURY_COVERAGE_ALERT"), eq("WALLET"), eq("WTS-202609-000001:direction@example.test"), any());
    }

    @Test
    void plusieursAdressesRecoiventChacuneLeurCourriel() {
        ReflectionTestUtils.setField(service, "alertEmail", "direction@example.test, finance@example.test;direction@example.test");

        service.reconcile(inputs("1000000", "600000", null), "cfo");

        verify(outboxService).publish(eq("WALLET_TREASURY_COVERAGE_ALERT"), eq("WALLET"), eq("WTS-202609-000001:direction@example.test"), any());
        verify(outboxService).publish(eq("WALLET_TREASURY_COVERAGE_ALERT"), eq("WALLET"), eq("WTS-202609-000001:finance@example.test"), any());
        verify(outboxService, org.mockito.Mockito.times(2)).publish(any(), any(), any(), any());
    }

    @Test
    void uneExplicationNeCouvrePasUneTresorerieQuiManque() {
        // Une explication justifie un écart comptable · pas de l'argent qui n'est pas là.
        WalletTreasuryReconciliation result = service.reconcile(
                inputs("1000000", "600000", "Virement en transit"), "cfo");

        assertEquals(WalletTreasuryReconciliation.Status.UNDER_REVIEW, result.getStatus());
    }

    @Test
    void unEcartComptableExpliqueEstVisibleSansEtreAlarmant() {
        WalletTreasuryReconciliation result = service.reconcile(
                inputs("990000", "1500000", "Écriture du 17 non encore passée"), "cfo");

        assertEquals(WalletTreasuryReconciliation.Status.VARIANCE, result.getStatus());
        assertEquals(0, result.getVariance().compareTo(new BigDecimal("-10000")));
    }

    @Test
    void unEcartComptableSansExplicationDemandeUneRevue() {
        WalletTreasuryReconciliation result = service.reconcile(inputs("990000", "1500000", null), "cfo");

        assertEquals(WalletTreasuryReconciliation.Status.UNDER_REVIEW, result.getStatus());
    }

    @Test
    void sansEncoursLaCouvertureEstEntiere() {
        when(walletRepository.aggregateOpen("XAF")).thenReturn(new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L});

        WalletTreasuryReconciliation result = service.reconcile(inputs("0", "0", null), "cfo");

        assertEquals(WalletTreasuryReconciliation.Status.BALANCED, result.getStatus(),
                "Rien à couvrir · diviser par zéro ne doit pas devenir une alerte");
    }

    @Test
    void refaireLeRapprochementDuJourLeRemplace() {
        WalletTreasuryReconciliation existing = WalletTreasuryReconciliation.builder()
                .reconciliationNumber("WTS-OLD").reconciliationDate(DAY).currency("XAF")
                .totalWalletBalance(BigDecimal.ONE).preparedBy("x").build();
        when(reconciliationRepository.findByReconciliationDateAndCurrency(DAY, "XAF")).thenReturn(Optional.of(existing));

        WalletTreasuryReconciliation result = service.reconcile(inputs("1000000", "1500000", null), "cfo");

        assertEquals("WTS-OLD", result.getReconciliationNumber(), "On refait un rapprochement, on n'en empile pas deux");
        assertEquals(0, result.getTotalWalletBalance().compareTo(new BigDecimal("1000000")));
    }
}

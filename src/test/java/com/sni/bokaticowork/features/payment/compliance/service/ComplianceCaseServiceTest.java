package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceCaseNoteRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceCaseRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceRuleRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
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

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le dossier, pas seulement l'alerte.
 *
 * <p>Un responsable, une échéance, une décision motivée. Et le verdict retourne aux règles :
 * c'est ainsi que le taux de faux positifs se mesure sans que personne ne le tienne à la main.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComplianceCaseServiceTest {

    @Mock private ComplianceCaseRepository caseRepository;
    @Mock private ComplianceCaseNoteRepository noteRepository;
    @Mock private ComplianceRuleRepository ruleRepository;
    @Mock private WalletRiskFlagRepository flagRepository;
    @Mock private WalletAccountRepository walletRepository;
    @Mock private ComplianceAuditService auditService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private ComplianceCaseService service;

    private WalletAccount wallet;
    private WalletRiskFlag flag;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dueHoursCritical", 24);
        ReflectionTestUtils.setField(service, "dueHoursHigh", 72);
        ReflectionTestUtils.setField(service, "dueHoursMedium", 168);
        ReflectionTestUtils.setField(service, "dueHoursLow", 336);
        wallet = WalletAccount.builder().id(1L).walletNumber("WAL-1").status(WalletStatus.ACTIVE).build();
        flag = WalletRiskFlag.builder().flagNumber("WRF-1").wallet(wallet).flagType(WalletRiskFlag.Type.STRUCTURING)
                .status(WalletRiskFlag.Status.OPEN).ruleCode("CR-STRUCTURING").build();
        when(sequenceGenerator.next("compliance_case")).thenReturn("CCS-2026-000001");
        when(caseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(flagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(caseRepository.findFirstBySubjectTypeAndSubjectCodeAndStatusInOrderByCreatedAtDesc(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(wallet));
    }

    @Test
    void ouvrirUnDossierPoseUneEcheanceSelonLaPriorite() {
        Instant before = Instant.now();
        ComplianceCase opened = service.openOrAttach(wallet, flag, ComplianceCase.Priority.HIGH, "Fractionnement", "RULE:CR-STRUCTURING");

        assertEquals(ComplianceCase.Status.OPEN, opened.getStatus());
        assertNotNull(opened.getDueAt());
        long hours = java.time.Duration.between(before, opened.getDueAt()).toHours();
        assertTrue(hours >= 71 && hours <= 72, "HIGH · 72 heures, pas une semaine");
        assertEquals("CCS-2026-000001", flag.getCaseNumber());
        assertEquals(WalletRiskFlag.Status.UNDER_REVIEW, flag.getStatus());
    }

    @Test
    void unSecondSignalementSAjouteAuDossierOuvertEtLaPrioriteNeBaisseJamais() {
        ComplianceCase existing = ComplianceCase.builder().caseNumber("CCS-2026-000001").subjectType("WALLET")
                .subjectCode("WAL-1").wallet(wallet).title("x").status(ComplianceCase.Status.OPEN)
                .priority(ComplianceCase.Priority.HIGH).triggeredByFlags("WRF-0")
                .dueAt(Instant.now().plusSeconds(3600)).createdAt(Instant.now()).build();
        when(caseRepository.findFirstBySubjectTypeAndSubjectCodeAndStatusInOrderByCreatedAtDesc(anyString(), anyString(), any()))
                .thenReturn(Optional.of(existing));

        ComplianceCase attached = service.openOrAttach(wallet, flag, ComplianceCase.Priority.LOW, "autre", "SYSTEM");

        assertEquals("CCS-2026-000001", attached.getCaseNumber(), "Un sujet n'a qu'un dossier ouvert à la fois");
        assertEquals("WRF-0,WRF-1", attached.getTriggeredByFlags());
        assertEquals(ComplianceCase.Priority.HIGH, attached.getPriority(), "LOW n'abaisse pas un dossier HIGH");
    }

    @Test
    void laClotureExigeUnMotifEcrit() {
        ComplianceCase opened = service.openOrAttach(wallet, flag, ComplianceCase.Priority.MEDIUM, "x", "SYSTEM");
        when(caseRepository.findByCaseNumber("CCS-2026-000001")).thenReturn(Optional.of(opened));

        assertThrows(BadRequestException.class,
                () -> service.close("CCS-2026-000001", ComplianceCase.Decision.CLEARED, "ok", null, "alice"));
        assertThrows(BadRequestException.class,
                () -> service.close("CCS-2026-000001", null, "Rien à signaler après vérification", null, "alice"));
    }

    @Test
    void ecarterUnDossierCompteUnFauxPositifPourChaqueRegleQuiLAlimentait() {
        ComplianceRule rule = ComplianceRule.builder().ruleCode("CR-STRUCTURING").hitCount(5L).confirmedCount(1L).dismissedCount(2L).build();
        when(ruleRepository.findByRuleCode("CR-STRUCTURING")).thenReturn(Optional.of(rule));
        when(flagRepository.findByFlagNumber("WRF-1")).thenReturn(Optional.of(flag));
        ComplianceCase opened = service.openOrAttach(wallet, flag, ComplianceCase.Priority.MEDIUM, "x", "SYSTEM");
        when(caseRepository.findByCaseNumber("CCS-2026-000001")).thenReturn(Optional.of(opened));

        ComplianceCase closed = service.close("CCS-2026-000001", ComplianceCase.Decision.CLEARED,
                "Titulaire contacté, versements de salaire justifiés", null, "alice");

        assertEquals(ComplianceCase.Status.CLOSED_CLEARED, closed.getStatus());
        assertEquals(3L, rule.getDismissedCount());
        assertEquals(1L, rule.getConfirmedCount());
        assertEquals(WalletRiskFlag.Status.DISMISSED, flag.getStatus());
        assertEquals(0, rule.falsePositiveRate().compareTo(new java.math.BigDecimal("0.75")));
    }

    @Test
    void confirmerUnDossierCompteUnSuccesEtNeLibereRien() {
        ComplianceRule rule = ComplianceRule.builder().ruleCode("CR-STRUCTURING").hitCount(5L).confirmedCount(0L).dismissedCount(0L).build();
        when(ruleRepository.findByRuleCode("CR-STRUCTURING")).thenReturn(Optional.of(rule));
        when(flagRepository.findByFlagNumber("WRF-1")).thenReturn(Optional.of(flag));
        wallet.setStatus(WalletStatus.UNDER_REVIEW);
        ComplianceCase opened = service.openOrAttach(wallet, flag, ComplianceCase.Priority.MEDIUM, "x", "SYSTEM");
        when(caseRepository.findByCaseNumber("CCS-2026-000001")).thenReturn(Optional.of(opened));

        service.close("CCS-2026-000001", ComplianceCase.Decision.CONFIRMED, "Fractionnement avéré sur trois jours", "…", "alice");

        assertEquals(1L, rule.getConfirmedCount());
        assertEquals(WalletRiskFlag.Status.CONFIRMED, flag.getStatus());
        assertEquals(WalletStatus.UNDER_REVIEW, wallet.getStatus(),
                "Ce qui suit une confirmation est une action administrative, pas un effet de bord de la clôture");
    }

    @Test
    void ecarterUnDossierLibereLePortefeuilleSiRienDAutreNEstOuvert() {
        when(flagRepository.findByFlagNumber("WRF-1")).thenReturn(Optional.of(flag));
        wallet.setStatus(WalletStatus.UNDER_REVIEW);
        ComplianceCase opened = service.openOrAttach(wallet, flag, ComplianceCase.Priority.MEDIUM, "x", "SYSTEM");
        when(caseRepository.findByCaseNumber("CCS-2026-000001")).thenReturn(Optional.of(opened));

        service.close("CCS-2026-000001", ComplianceCase.Decision.CLEARED, "Homonymie confirmée par pièce d'identité", null, "alice");

        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
    }

    @Test
    void unDossierClosNeSeRouvrePas() {
        ComplianceCase closed = ComplianceCase.builder().caseNumber("CCS-2026-000009").subjectType("WALLET").subjectCode("WAL-1")
                .status(ComplianceCase.Status.CLOSED_CLEARED).priority(ComplianceCase.Priority.LOW).dueAt(Instant.now()).build();
        when(caseRepository.findByCaseNumber("CCS-2026-000009")).thenReturn(Optional.of(closed));

        assertThrows(BadRequestException.class, () -> service.assign("CCS-2026-000009", "bob", "alice"));
        assertThrows(BadRequestException.class, () -> service.addNote("CCS-2026-000009", "alice", "encore"));
    }

    @Test
    void chaqueGesteLaisseUneTrace() {
        ComplianceCase opened = service.openOrAttach(wallet, flag, ComplianceCase.Priority.MEDIUM, "x", "SYSTEM");
        when(caseRepository.findByCaseNumber("CCS-2026-000001")).thenReturn(Optional.of(opened));

        service.assign("CCS-2026-000001", "bob", "alice");

        verify(auditService).record(any(), org.mockito.ArgumentMatchers.eq("CASE_OPENED"), any(), any(), any());
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("alice"), any(), org.mockito.ArgumentMatchers.eq("CASE_ASSIGNED"),
                any(), any(), any(), org.mockito.ArgumentMatchers.eq("bob"), any());
    }
}

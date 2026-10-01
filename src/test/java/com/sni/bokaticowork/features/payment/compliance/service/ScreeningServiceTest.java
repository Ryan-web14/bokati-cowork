package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningListEntry;
import com.sni.bokaticowork.features.payment.compliance.repository.ScreeningCheckRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ScreeningListEntryRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
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
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le contrôle des listes, et sa preuve.
 *
 * <p>Le rapprochement est large et un humain tranche. Ce que le test protège surtout : chaque
 * contrôle laisse une ligne qui dit quelle liste, quelle version, quel résultat · même quand il
 * n'a rien trouvé, surtout quand il n'a rien trouvé.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScreeningServiceTest {

    @Mock private ScreeningListEntryRepository entryRepository;
    @Mock private ScreeningCheckRepository checkRepository;
    @Mock private WalletAccountRepository walletRepository;
    @Mock private WalletRiskFlagService flagService;
    @Mock private ComplianceCaseService caseService;
    @Mock private ComplianceAuditService auditService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private ScreeningService service;

    @BeforeEach
    void setUp() {
        when(sequenceGenerator.next("screening_check")).thenReturn("SCR-202609-000001");
        when(checkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entryRepository.activeLists()).thenReturn(List.<Object[]>of(new Object[]{"UN-SANCTIONS", "2026-09", 2L}));
        when(entryRepository.findByActiveTrue()).thenReturn(List.of(
                entry(1L, "Jean-Pierre Mabiala", "Mabiala Jean Pierre;J.P. Mabiala", 1970),
                entry(2L, "Émile Ngoma", null, null)));
        when(walletRepository.list(anyString(), anyString(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    private ScreeningListEntry entry(Long id, String name, String aliases, Integer birthYear) {
        ScreeningListEntry entry = ScreeningListEntry.builder().id(id).listCode("UN-SANCTIONS").listVersion("2026-09")
                .entryType(ScreeningListEntry.Type.SANCTION).fullName(name).normalizedName(service.normalize(name))
                .birthYear(birthYear).build();
        if (aliases != null) {
            entry.setAliases(String.join(";", java.util.Arrays.stream(aliases.split(";")).map(service::normalize).toList()));
        }
        return entry;
    }

    @Test
    void laNormalisationEffaceAccentsCasseEtPonctuation() {
        assertEquals("emile ngoma", service.normalize("  Émile  NGOMA. "));
        assertEquals("jean pierre mabiala", service.normalize("Jean-Pierre Mabiala"));
    }

    @Test
    void unNomEgalEstUnRapprochementExact() {
        ScreeningCheck check = service.screen("MEMBER", "MBR-1", "emile ngoma", null, ScreeningCheck.Trigger.ONBOARDING, "SYSTEM");

        assertEquals(ScreeningCheck.Result.MATCH, check.getResult());
        assertEquals("2", check.getMatchedEntryIds());
    }

    @Test
    void lesMotsDansUnAutreOrdreSontUnRapprochementPossible() {
        ScreeningCheck check = service.screen("MEMBER", "MBR-1", "Mabiala Jean", null, ScreeningCheck.Trigger.ONBOARDING, "SYSTEM");

        assertEquals(ScreeningCheck.Result.POSSIBLE_MATCH, check.getResult());
        assertTrue(check.getMatchDetails().contains("PARTIAL"));
    }

    @Test
    void uneAnneeDeNaissanceQuiContreditEcarteLeRapprochement() {
        ScreeningCheck check = service.screen("MEMBER", "MBR-1", "Jean Pierre Mabiala", 1995, ScreeningCheck.Trigger.ONBOARDING, "SYSTEM");

        assertEquals(ScreeningCheck.Result.CLEAR, check.getResult(),
                "Même nom, mais né vingt-cinq ans plus tard · ce n'est pas lui, et l'algorithme peut le dire");
    }

    @Test
    void unControleSansRapprochementLaisseQuandMemeSaPreuve() {
        ScreeningCheck check = service.screen("MEMBER", "MBR-1", "Alice Okemba", null, ScreeningCheck.Trigger.PERIODIC, "SYSTEM");

        assertEquals(ScreeningCheck.Result.CLEAR, check.getResult());
        assertEquals("UN-SANCTIONS/2026-09(2)", check.getListsChecked(), "Quelle liste, quelle version · c'est la preuve");
        assertNotNull(check.getCheckedAt());
        verify(checkRepository).save(any());
        verify(auditService).record(any(), eq("SCREENING_CHECK"), eq("MEMBER"), eq("MBR-1"), any());
        verify(caseService, never()).openOrAttach(any(), any(), any(), any(), any());
    }

    @Test
    void unRapprochementOuvreUnDossierSurLePortefeuilleDuSujet() {
        WalletAccount wallet = WalletAccount.builder().id(9L).walletNumber("WAL-9").build();
        when(walletRepository.list(eq("MEMBER"), eq("MBR-1"), any())).thenReturn(new PageImpl<>(List.of(wallet)));
        WalletRiskFlag flag = WalletRiskFlag.builder().flagNumber("WRF-7").wallet(wallet).flagType(WalletRiskFlag.Type.LIST_MATCH).build();
        when(flagService.raiseForRule(eq(wallet), eq(WalletRiskFlag.Type.LIST_MATCH), any(), any(), any(), eq("SCREENING")))
                .thenReturn(Optional.of(new WalletRiskFlagService.RaiseResult(flag, true)));
        when(caseService.openOrAttach(eq(wallet), eq(flag), eq(ComplianceCase.Priority.CRITICAL), any(), any()))
                .thenReturn(ComplianceCase.builder().caseNumber("CCS-2026-000003").build());

        ScreeningCheck check = service.screen("MEMBER", "MBR-1", "Émile Ngoma", null, ScreeningCheck.Trigger.ONBOARDING, "SYSTEM");

        assertEquals("CCS-2026-000003", check.getCaseNumber());
    }

    @Test
    void laRevueSeMotiveEtNeTouchePasAuResultat() {
        ScreeningCheck check = ScreeningCheck.builder().checkNumber("SCR-1").subjectType("MEMBER").subjectCode("MBR-1")
                .result(ScreeningCheck.Result.POSSIBLE_MATCH).build();
        when(checkRepository.findByCheckNumber("SCR-1")).thenReturn(Optional.of(check));

        assertThrows(BadRequestException.class, () -> service.review("SCR-1", false, "non", "alice"));

        ScreeningCheck reviewed = service.review("SCR-1", false, "Homonyme · pièce d'identité vérifiée à l'accueil", "alice");
        assertEquals("FALSE_MATCH", reviewed.getReviewOutcome());
        assertEquals(ScreeningCheck.Result.POSSIBLE_MATCH, reviewed.getResult(), "Le résultat du contrôle ne se réécrit pas");
    }

    @Test
    void chargerUneListeDesactiveLaVersionPrecedenteSansLaSupprimer() {
        ScreeningListEntry old = entry(5L, "Ancien Nom", null, null);
        when(entryRepository.findActiveByList("UN-SANCTIONS")).thenReturn(List.of(old));
        when(entryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int loaded = service.loadList("UN-SANCTIONS", "2026-10", ScreeningListEntry.Type.SANCTION,
                List.of(new ScreeningService.ListEntryInput("Nouveau Nom", "N. Nom", null, null, "REF-1"),
                        new ScreeningService.ListEntryInput("  ", null, null, null, null)), "alice");

        assertEquals(1, loaded);
        assertEquals(Boolean.FALSE, old.getActive(), "Désactivée, pas supprimée · les contrôles passés la citent");
        verify(entryRepository, never()).delete(any());
    }
}

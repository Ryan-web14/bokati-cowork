package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceAuditEntry;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceAuditEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * La piste d'audit · chaînée, conservée dix ans.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComplianceAuditServiceTest {

    @Mock
    private ComplianceAuditEntryRepository auditRepository;

    @InjectMocks
    private ComplianceAuditService service;

    private final List<ComplianceAuditEntry> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(auditRepository.save(any())).thenAnswer(invocation -> {
            ComplianceAuditEntry entry = invocation.getArgument(0);
            stored.add(entry);
            return entry;
        });
        when(auditRepository.findLastHash()).thenAnswer(invocation ->
                stored.isEmpty() ? Optional.empty() : Optional.of(stored.get(stored.size() - 1).getCurrentHash()));
        when(auditRepository.findChain()).thenAnswer(invocation -> new ArrayList<>(stored));
    }

    @Test
    void chaqueLigneSAccrocheALaPrecedente() {
        ComplianceAuditEntry first = service.record("alice", "CASE_OPENED", "WALLET", "WAL-1", "ouverture");
        ComplianceAuditEntry second = service.record("bob", "CASE_ASSIGNED", "WALLET", "WAL-1", "confié");

        assertEquals(ComplianceAuditService.GENESIS, first.getPreviousHash());
        assertEquals(first.getCurrentHash(), second.getPreviousHash());
        assertTrue(service.verify().intact());
    }

    @Test
    void uneLigneModifieeRomptLaChaine() {
        service.record("alice", "CASE_OPENED", "WALLET", "WAL-1", "ouverture");
        service.record("bob", "CASE_CLOSED", "WALLET", "WAL-1", "clos · rien à signaler");

        stored.get(0).setRationale("ouverture · modifiée après coup");

        ComplianceAuditService.ChainReport report = service.verify();
        assertFalse(report.intact());
        assertEquals(1, report.brokenEntries().size(), "La ligne touchée ne correspond plus à son empreinte");
    }

    @Test
    void recalculerLEmpreinteDUneLigneModifieeRomptLaSuivante() {
        // Un falsificateur soigneux recalcule l'empreinte de la ligne qu'il a touchée · c'est alors
        // la ligne suivante, accrochée à l'ancienne empreinte, qui le trahit.
        service.record("alice", "CASE_OPENED", "WALLET", "WAL-1", "ouverture");
        service.record("bob", "CASE_CLOSED", "WALLET", "WAL-1", "clos");

        ComplianceAuditEntry tampered = stored.get(0);
        tampered.setRationale("ouverture · modifiée après coup");
        tampered.setCurrentHash(ComplianceAuditService.hash(tampered));

        ComplianceAuditService.ChainReport report = service.verify();
        assertFalse(report.intact());
        assertEquals(1, report.brokenEntries().size());
        assertEquals(stored.get(1).getEntryUuid().toString(), report.brokenEntries().get(0));
    }

    @Test
    void uneLigneRetireeRomptLaSuivante() {
        service.record("alice", "A", "WALLET", "WAL-1", null);
        service.record("alice", "B", "WALLET", "WAL-1", null);
        service.record("alice", "C", "WALLET", "WAL-1", null);

        stored.remove(1);

        ComplianceAuditService.ChainReport report = service.verify();
        assertFalse(report.intact());
        assertEquals(1, report.brokenEntries().size());
    }

    @Test
    void laConservationEstDeDixAnsEtSePoseALEcriture() {
        ComplianceAuditEntry entry = service.record("alice", "A", "WALLET", "WAL-1", null);

        LocalDate expected = LocalDate.ofInstant(entry.getOccurredAt(), ZoneOffset.UTC).plusYears(10);
        assertEquals(expected, entry.getRetainUntil(), "Une purge écrite trop tôt est irréversible · la date se pose dès la conception");
    }

    @Test
    void unActeurAbsentDevientLeSysteme() {
        assertEquals("SYSTEM", service.record(null, "A", "WALLET", "WAL-1", null).getActor());
        assertEquals("SYSTEM", service.record("  ", "A", "WALLET", "WAL-1", null).getActor());
    }
}

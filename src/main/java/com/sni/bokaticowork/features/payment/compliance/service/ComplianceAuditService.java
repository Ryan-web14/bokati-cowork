package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.generator.uuid.TimeOrderedUuid;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceAuditEntry;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceAuditEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * La piste d'audit de conformite · chainee, conservee dix ans.
 *
 * <p>Une seule chaine pour tout le module, pas une par sujet : c'est ce qui rend indetectable
 * l'insertion d'une ligne « entre deux » sur un sujet peu consulte. L'ecriture est serialisee par
 * un verrou applicatif court, parce que deux lignes qui liraient le meme maillon precedent
 * dedoubleraient la chaine sans que rien ne paraisse rompu.</p>
 *
 * <p>Chaque ligne est ecrite dans sa propre transaction : ce qu'un administrateur a fait doit rester
 * ecrit meme si l'operation qu'il tentait a ensuite echoue. La trace de la tentative est une
 * information, et la seule qui reste quand tout le reste a ete annule.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceAuditService {

    public static final String GENESIS = "GENESIS";
    private static final Object CHAIN_LOCK = new Object();

    private final ComplianceAuditEntryRepository auditRepository;

    /** Ecrit une ligne · le maillon precedent est lu et le nouveau calcule sous verrou. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ComplianceAuditEntry record(String actor, String actorRole, String action,
                                       String subjectType, String subjectCode,
                                       String beforeState, String afterState, String rationale) {
        synchronized (CHAIN_LOCK) {
            Instant now = Instant.now();
            String previous = auditRepository.findLastHash().orElse(GENESIS);
            ComplianceAuditEntry entry = ComplianceAuditEntry.builder()
                    .entryUuid(TimeOrderedUuid.next())
                    .occurredAt(now)
                    .actor(actor == null || actor.isBlank() ? "SYSTEM" : actor)
                    .actorRole(actorRole)
                    .action(action)
                    .subjectType(subjectType)
                    .subjectCode(subjectCode)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .rationale(rationale)
                    .previousHash(previous)
                    .retainUntil(LocalDate.ofInstant(now, ZoneOffset.UTC).plusYears(ComplianceAuditEntry.RETENTION_YEARS))
                    .build();
            entry.setCurrentHash(hash(entry));
            return auditRepository.save(entry);
        }
    }

    /** Raccourci sans etats avant et apres · pour les actions qui n'en ont pas. */
    public ComplianceAuditEntry record(String actor, String action, String subjectType, String subjectCode, String rationale) {
        return record(actor, null, action, subjectType, subjectCode, null, null, rationale);
    }

    @Transactional(readOnly = true)
    public List<ComplianceAuditEntry> trail(String subjectType, String subjectCode) {
        return auditRepository.findBySubjectTypeAndSubjectCodeOrderByIdAsc(subjectType, subjectCode);
    }

    public record ChainReport(long entries, List<String> brokenEntries) {
        public boolean intact() {
            return brokenEntries.isEmpty();
        }
    }

    /** Reparcourt toute la chaine · lent par construction, c'est un controle, pas une consultation. */
    @Transactional(readOnly = true)
    public ChainReport verify() {
        List<ComplianceAuditEntry> chain = auditRepository.findChain();
        List<String> broken = new ArrayList<>();
        String expected = GENESIS;
        for (ComplianceAuditEntry entry : chain) {
            if (!expected.equals(entry.getPreviousHash()) || !hash(entry).equals(entry.getCurrentHash())) {
                broken.add(entry.getEntryUuid().toString());
            }
            expected = entry.getCurrentHash();
        }
        if (!broken.isEmpty()) {
            log.error("Piste d'audit de conformite · {} maillon(s) rompu(s)", broken.size());
        }
        return new ChainReport(chain.size(), broken);
    }

    /** Fige la facon de calculer · une divergence entre ecriture et verification rendrait toute la chaine fausse. */
    static String hash(ComplianceAuditEntry entry) {
        String payload = String.join("|",
                entry.getEntryUuid().toString(),
                String.valueOf(entry.getOccurredAt().toEpochMilli()),
                entry.getActor(),
                entry.getAction(),
                entry.getSubjectType(),
                entry.getSubjectCode(),
                entry.getBeforeState() == null ? "" : entry.getBeforeState(),
                entry.getAfterState() == null ? "" : entry.getAfterState(),
                entry.getRationale() == null ? "" : entry.getRationale(),
                entry.getPreviousHash());
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 indisponible", ex);
        }
    }
}

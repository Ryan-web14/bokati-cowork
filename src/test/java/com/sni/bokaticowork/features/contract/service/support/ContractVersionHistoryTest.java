package com.sni.bokaticowork.features.contract.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.contract.enums.AmendmentStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractAmendment;
import com.sni.bokaticowork.features.contract.model.ContractAmendmentVariable;
import com.sni.bokaticowork.features.contract.model.ContractAuditEvent;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentVariableRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAuditEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le contrat montre ses versions · l'original, ses avenants et ses extensions de date, dans l'ordre.
 */
class ContractVersionHistoryTest {

    private final ContractAmendmentRepository amendments = mock(ContractAmendmentRepository.class);
    private final ContractAmendmentVariableRepository variables = mock(ContractAmendmentVariableRepository.class);
    private final ContractAuditEventRepository audit = mock(ContractAuditEventRepository.class);
    private final ContractVersionHistory history = new ContractVersionHistory(amendments, variables, audit, new ObjectMapper());

    private final Contract contract = Contract.builder().contractCode("CTR-1").title("Contrat abonnement SUB-1")
            .startDate(LocalDate.of(2026, 9, 1)).endDate(LocalDate.of(2026, 12, 15))
            .signedAt(Instant.parse("2026-09-01T08:00:00Z")).signedDocumentCode("DOC-1").build();

    private ContractAmendment renewal(String code, String at, String previousEnd, String newEnd) {
        ContractAmendment amendment = ContractAmendment.builder().code(code).status(AmendmentStatus.ACTIVE)
                .description("Renouvellement").effectiveDate(LocalDate.parse(previousEnd).plusDays(1))
                .activatedAt(Instant.parse(at)).draftDocumentCode("DOC-" + code).build();
        when(variables.findAllByAmendmentCodeOrderByVariableKeyAsc(code)).thenReturn(List.of(
                ContractAmendmentVariable.builder().variableKey("periode_debut").previousValue("2026-09-01").newValue("2026-10-01").build(),
                ContractAmendmentVariable.builder().variableKey("periode_fin").previousValue(previousEnd).newValue(newEnd).build()));
        return amendment;
    }

    @Test
    @DisplayName("L'original, l'avenant puis l'extension, numerotes dans l'ordre, chacun avec ses dates")
    void versionsInOrder() {
        ContractAmendment amendment = renewal("AVN-1", "2026-10-01T09:00:00Z", "2026-09-30", "2026-11-30");
        when(amendments.findAllByOriginalContractCodeAndStatusIn("CTR-1", List.of(AmendmentStatus.ACTIVE))).thenReturn(List.of(amendment));
        ContractAuditEvent extension = mock(ContractAuditEvent.class);
        when(extension.getEventType()).thenReturn("CONTRACT_TERM_EXTENDED");
        when(extension.getOccurredAt()).thenReturn(Instant.parse("2026-11-20T09:00:00Z"));
        when(extension.getJustification()).thenReturn("Reprise de l'abonnement après une pause");
        when(extension.getPayload()).thenReturn("{\"previousEndDate\":\"2026-11-30\",\"newEndDate\":\"2026-12-15\"}");
        ContractAuditEvent other = mock(ContractAuditEvent.class);
        when(other.getEventType()).thenReturn("CONTRACT_AMENDED");
        when(audit.findAllByContractCodeOrderByOccurredAtAsc("CTR-1")).thenReturn(List.of(other, extension));

        List<ContractVersionHistory.Version> versions = history.of(contract);

        assertThat(versions).extracting(ContractVersionHistory.Version::version).containsExactly(1, 2, 3);
        assertThat(versions).extracting(ContractVersionHistory.Version::kind).containsExactly(
                ContractVersionHistory.Kind.ORIGINAL, ContractVersionHistory.Kind.AMENDMENT, ContractVersionHistory.Kind.EXTENSION);
        assertThat(versions.getFirst().endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(versions.getFirst().documentCode()).isEqualTo("DOC-1");
        assertThat(versions.get(1).amendmentCode()).isEqualTo("AVN-1");
        assertThat(versions.get(1).previousEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(versions.get(1).endDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(versions.get(1).documentCode()).isEqualTo("DOC-AVN-1");
        assertThat(versions.getLast().endDate()).isEqualTo(contract.getEndDate());
        assertThat(versions.getLast().description()).contains("Reprise");
    }

    @Test
    @DisplayName("Un contrat jamais modifie n'a que sa version d'origine")
    void untouchedContract() {
        when(amendments.findAllByOriginalContractCodeAndStatusIn("CTR-1", List.of(AmendmentStatus.ACTIVE))).thenReturn(List.of());
        when(audit.findAllByContractCodeOrderByOccurredAtAsc("CTR-1")).thenReturn(List.of());

        List<ContractVersionHistory.Version> versions = history.of(contract);

        assertThat(versions).hasSize(1);
        assertThat(versions.getFirst().endDate()).isEqualTo(contract.getEndDate());
    }
}

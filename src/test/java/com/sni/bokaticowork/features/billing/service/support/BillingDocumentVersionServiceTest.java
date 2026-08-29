package com.sni.bokaticowork.features.billing.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEditHistory;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEditHistoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingDocumentVersionServiceTest {

    private final BillingDocumentEditHistoryRepository historyRepository =
            mock(BillingDocumentEditHistoryRepository.class);

    private final BillingDocumentVersionService service =
            new BillingDocumentVersionService(historyRepository, new ObjectMapper());

    @Test
    void shouldArchiveTheStateBeforeModificationWithAFilledSnapshot() {
        // Regression : la table etait ecrite a chaque modification mais snapshot_json restait
        // vide et changed_by valait la constante "SYSTEM". On savait qu'une modification avait
        // eu lieu, jamais ce que le document contenait avant.
        BillingDocument document = document("QUO-001", "10000");
        when(historyRepository.findLastVersionNumber(document)).thenReturn(null);
        when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingDocumentEditHistory archived = service.archive(
                document, List.of(line("Salle", "10000")), "FULL_EDIT");

        assertThat(archived.getVersionNumber()).isEqualTo(1);
        assertThat(archived.getSnapshotJson())
                .contains("\"documentNumber\":\"QUO-001\"")
                .contains("\"totalAmount\":\"10000\"")
                .contains("\"lines\"")
                .contains("Salle");
        assertThat(archived.getChangeSummary()).isEqualTo("Version initiale");
    }

    @Test
    void shouldContinueTheExistingNumbering() {
        BillingDocument document = document("QUO-002", "10000");
        when(historyRepository.findLastVersionNumber(document)).thenReturn(4);
        when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.archive(document, List.of(), "FULL_EDIT").getVersionNumber()).isEqualTo(5);
    }

    @Test
    void shouldSummariseWhatChangedSinceThePreviousVersion() {
        BillingDocument document = document("QUO-003", "12000");
        when(historyRepository.findLastVersionNumber(document)).thenReturn(1);
        when(historyRepository.findFirstByDocumentOrderByVersionNumberDesc(document))
                .thenReturn(Optional.of(BillingDocumentEditHistory.builder()
                        .versionNumber(1)
                        .snapshotJson("{\"documentNumber\":\"QUO-003\",\"totalAmount\":\"10000\",\"lines\":[]}")
                        .build()));
        when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingDocumentEditHistory archived = service.archive(document, List.of(), "FULL_EDIT");

        assertThat(archived.getChangeSummary()).contains("totalAmount");
    }

    @Test
    void shouldMarkTheLastVersionAsSentOnlyOnce() {
        BillingDocument document = document("QUO-004", "10000");
        BillingDocumentEditHistory last = BillingDocumentEditHistory.builder().versionNumber(2).build();
        when(historyRepository.findFirstByDocumentOrderByVersionNumberDesc(document))
                .thenReturn(Optional.of(last));

        service.markLastVersionSent(document);
        assertThat(last.getSentToCustomerAt()).isNotNull();

        // Un second envoi ne doit pas reecrire la date de transmission d'origine.
        Instant first = last.getSentToCustomerAt();
        service.markLastVersionSent(document);
        assertThat(last.getSentToCustomerAt()).isEqualTo(first);
        verify(historyRepository).save(last);
    }

    @Test
    void shouldNotFailWhenADocumentHasNoVersionYet() {
        BillingDocument document = document("QUO-005", "10000");
        when(historyRepository.findFirstByDocumentOrderByVersionNumberDesc(document))
                .thenReturn(Optional.empty());

        service.markLastVersionSent(document);

        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldReportOnlyTheFieldsThatActuallyChangedBetweenTwoVersions() {
        BillingDocument document = document("QUO-006", "10000");
        when(historyRepository.findByDocumentAndVersionNumber(document, 1))
                .thenReturn(Optional.of(BillingDocumentEditHistory.builder()
                        .snapshotJson("{\"totalAmount\":\"10000\",\"customerName\":\"Jean\"}").build()));
        when(historyRepository.findByDocumentAndVersionNumber(document, 2))
                .thenReturn(Optional.of(BillingDocumentEditHistory.builder()
                        .snapshotJson("{\"totalAmount\":\"9000\",\"customerName\":\"Jean\"}").build()));

        var diff = service.diff(document, 1, 2);

        assertThat(diff.changes()).hasSize(1);
        assertThat(diff.changes().getFirst().field()).isEqualTo("totalAmount");
        assertThat(diff.changes().getFirst().before()).isEqualTo("10000");
        assertThat(diff.changes().getFirst().after()).isEqualTo("9000");
    }

    @Test
    void shouldReportTheRevisionToDisplay() {
        BillingDocument document = document("QUO-007", "10000");
        when(historyRepository.findLastVersionNumber(document)).thenReturn(null);
        assertThat(service.currentRevision(document)).isEqualTo(1);

        when(historyRepository.findLastVersionNumber(document)).thenReturn(3);
        assertThat(service.currentRevision(document)).isEqualTo(4);
    }

    private BillingDocument document(String number, String total) {
        return BillingDocument.builder()
                .documentNumber(number)
                .status(BillingDocumentStatus.DRAFT)
                .customerCode("MBR-001")
                .customerName("Jean")
                .currency("XAF")
                .subtotalAmount(new BigDecimal(total))
                .totalAmount(new BigDecimal(total))
                .build();
    }

    private BillingDocumentLine line(String description, String amount) {
        return BillingDocumentLine.builder()
                .description(description)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal(amount))
                .subtotalAmount(new BigDecimal(amount))
                .totalAmount(new BigDecimal(amount))
                .build();
    }
}

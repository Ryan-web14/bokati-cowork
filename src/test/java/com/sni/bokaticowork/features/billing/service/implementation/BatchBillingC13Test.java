package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.BatchBillingDtos;
import com.sni.bokaticowork.features.billing.model.BillingBatchRun;
import com.sni.bokaticowork.features.billing.model.BillingBatchRunItem;
import com.sni.bokaticowork.features.billing.repository.BillingBatchRunItemRepository;
import com.sni.bokaticowork.features.billing.repository.BillingBatchRunRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot C13 : l'apercu de novembre regroupe les consommations en attente par client ; un second
 * apercu ne reprend pas celles deja retenues ; un client de l'apercu peut etre exclu.
 */
class BatchBillingC13Test {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BillingBatchRunRepository runs = mock(BillingBatchRunRepository.class);
    private final BillingBatchRunItemRepository items = mock(BillingBatchRunItemRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private BatchBillingService service;
    private final List<BillingBatchRun> savedRuns = new ArrayList<>();
    private final List<BillingBatchRunItem> savedItems = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new BatchBillingService(jdbc, runs, items, sequences, staff, mock(ObjectProvider.class), mock(PlatformTransactionManager.class));
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("facturation@elleaose.com", "Facturation", null));
        when(sequences.next("billing_batch_run")).thenAnswer(invocation -> "LFG-" + (savedRuns.size() + 1));
        when(jdbc.queryForList(anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(
                row("BIL-1", "BOOKING", "MEMBER", "MBR-1", "Awa", "25000"),
                row("BIL-2", "OVERAGE", "MEMBER", "MBR-1", "Awa", "5000"),
                row("BIL-3", "BOOKING", "COMPANY", "ORG-1", "Société Mbote", "120000")));
        when(runs.save(any())).thenAnswer(invocation -> {
            BillingBatchRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId((long) savedRuns.size() + 1);
                savedRuns.add(run);
            }
            return run;
        });
        when(runs.findByStatus("PREVIEW")).thenAnswer(invocation -> savedRuns.stream().filter(run -> "PREVIEW".equals(run.getStatus())).toList());
        when(runs.findByRunNumber(anyString())).thenAnswer(invocation -> savedRuns.stream()
                .filter(run -> run.getRunNumber().equals(invocation.getArgument(0))).findFirst());
        when(items.saveAll(anyList())).thenAnswer(invocation -> {
            Collection<BillingBatchRunItem> list = invocation.getArgument(0);
            long id = savedItems.size();
            for (BillingBatchRunItem item : list) {
                item.setId(++id);
                savedItems.add(item);
            }
            return List.copyOf(list);
        });
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findByRunIdIn(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return savedItems.stream().filter(item -> ids.contains(item.getRunId())).toList();
        });
        when(items.findByRunIdOrderByCustomerNameAsc(any())).thenAnswer(invocation -> savedItems.stream()
                .filter(item -> item.getRunId().equals(invocation.getArgument(0))).toList());
        when(items.findById(any())).thenAnswer(invocation -> savedItems.stream()
                .filter(item -> item.getId().equals(invocation.getArgument(0))).findFirst());
    }

    @Test
    void shouldGroupPendingConsumptionsByCustomer() {
        BatchBillingDtos.RunView run = service.preview(new BatchBillingDtos.PreviewRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, null));

        assertThat(run.customers()).hasSize(2);
        assertThat(run.customers()).filteredOn(customer -> customer.customerCode().equals("MBR-1")).singleElement()
                .satisfies(customer -> {
                    assertThat(customer.itemCount()).isEqualTo(2);
                    assertThat(customer.totalAmount()).isEqualByComparingTo("30000");
                });
        assertThat(run.totalToInvoice()).isEqualByComparingTo("150000");
        assertThat(run.title()).isEqualTo("Facture du 01/11/2026 au 30/11/2026");
    }

    @Test
    void shouldNotTakeAgainWhatAnOpenPreviewHoldsAndLetExcludeACustomer() {
        BatchBillingDtos.RunView first = service.preview(new BatchBillingDtos.PreviewRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                null, null));
        assertThatThrownBy(() -> service.preview(new BatchBillingDtos.PreviewRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null)))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("RIEN_A_FACTURER"));

        Long company = first.customers().stream().filter(customer -> customer.customerCode().equals("ORG-1")).findFirst().orElseThrow().itemId();
        BatchBillingDtos.RunView excluded = service.exclude(first.runNumber(), company, true);
        assertThat(excluded.totalToInvoice()).isEqualByComparingTo("30000");
    }

    @Test
    void shouldKeepOnlyTheRequestedSources() {
        BatchBillingDtos.RunView run = service.preview(new BatchBillingDtos.PreviewRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                List.of("booking"), null));

        assertThat(run.totalToInvoice()).isEqualByComparingTo("145000");
    }

    private static Map<String, Object> row(String number, String source, String type, String code, String name, String amount) {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("billable_number", number);
        row.put("source_type", source);
        row.put("subscriber_type", type);
        row.put("subscriber_code", code);
        row.put("subscriber_name", name);
        row.put("billing_company_code", null);
        row.put("amount", new BigDecimal(amount));
        row.put("currency", "XAF");
        return row;
    }
}

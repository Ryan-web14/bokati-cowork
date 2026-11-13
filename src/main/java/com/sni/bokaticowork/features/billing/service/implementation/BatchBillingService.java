package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.BatchBillingDtos;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.model.BillingBatchRun;
import com.sni.bokaticowork.features.billing.model.BillingBatchRunItem;
import com.sni.bokaticowork.features.billing.repository.BillingBatchRunItemRepository;
import com.sni.bokaticowork.features.billing.repository.BillingBatchRunRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lot C13 : facturation groupee periodique. Un lancement rassemble, client par client, les
 * consommations en attente d'une periode (elements facturables : abonnements, options,
 * depassements, reservations...) ; l'apercu se relit, des clients s'excluent, puis l'emission
 * produit une facture emise par client, chacune dans sa transaction. Une consommation facturee
 * porte sa facture et ne revient plus ; une consommation deja retenue par un apercu en cours
 * n'entre pas dans un second.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BatchBillingService {

    static final String PREVIEW = "PREVIEW";
    static final String ISSUED = "ISSUED";
    static final String CANCELLED = "CANCELLED";

    private final JdbcTemplate jdbc;
    private final BillingBatchRunRepository runs;
    private final BillingBatchRunItemRepository items;
    private final SequenceGeneratorFacade sequences;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<BillingDocumentService> billingServices;
    private final PlatformTransactionManager transactionManager;

    /** Apercu : consommations en attente de la periode, regroupees par client, organisation payeuse et devise. */
    public BatchBillingDtos.RunView preview(BatchBillingDtos.PreviewRequest request) {
        if (request.periodTo().isBefore(request.periodFrom())) {
            throw BadRequestException.coded("LANCEMENT_INVALIDE", "La fin de la période précède son début.");
        }
        Set<String> reserved = reservedBillables();
        List<String> sources = request.sourceTypes() == null ? List.of()
                : request.sourceTypes().stream().filter(StringUtils::hasText).map(value -> value.trim().toUpperCase(Locale.ROOT)).toList();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT b.billable_number, b.source_type, b.subscriber_type, b.subscriber_code, b.subscriber_name, b.billing_company_code,
                       b.amount, b.currency
                FROM billable_item b
                WHERE b.status = 'PENDING' AND b.invoice_id IS NULL
                  AND COALESCE(b.billing_period_start, CAST(b.created_at AS DATE)) BETWEEN ? AND ?
                ORDER BY b.subscriber_code, b.created_at
                """, request.periodFrom(), request.periodTo());
        Map<String, BillingBatchRunItem> groups = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String number = (String) row.get("billable_number");
            String source = row.get("source_type") == null ? "" : row.get("source_type").toString().toUpperCase(Locale.ROOT);
            if (reserved.contains(number) || (!sources.isEmpty() && sources.stream().noneMatch(source::startsWith))) {
                continue;
            }
            String type = String.valueOf(row.get("subscriber_type"));
            String code = String.valueOf(row.get("subscriber_code"));
            String company = (String) row.get("billing_company_code");
            String currency = row.get("currency") == null ? "XAF" : row.get("currency").toString();
            BigDecimal amount = row.get("amount") == null ? BigDecimal.ZERO : (BigDecimal) row.get("amount");
            BillingBatchRunItem group = groups.computeIfAbsent(type + "|" + code + "|" + company + "|" + currency,
                    key -> BillingBatchRunItem.builder().customerType(type).customerCode(code).customerName((String) row.get("subscriber_name"))
                            .billingCompany(company).billableNumbers("").itemCount(0).totalAmount(BigDecimal.ZERO).currency(currency).build());
            group.setBillableNumbers(group.getBillableNumbers().isEmpty() ? number : group.getBillableNumbers() + "," + number);
            group.setItemCount(group.getItemCount() + 1);
            group.setTotalAmount(group.getTotalAmount().add(amount));
        }
        if (groups.isEmpty()) {
            throw BadRequestException.coded("RIEN_A_FACTURER", "Aucune consommation en attente sur la période.");
        }
        String title = StringUtils.hasText(request.title()) ? request.title().trim()
                : "Facture du " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(request.periodFrom()) + " au "
                + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(request.periodTo());
        BillingBatchRun run = runs.save(BillingBatchRun.builder().runNumber(sequences.next("billing_batch_run")).periodFrom(request.periodFrom())
                .periodTo(request.periodTo()).title(title).status(PREVIEW).createdBy(staffProfiles.currentAuthor().login()).build());
        groups.values().forEach(group -> group.setRunId(run.getId()));
        items.saveAll(new ArrayList<>(groups.values()));
        return view(run);
    }

    /** Exclut ou reintegre un client de l'apercu. */
    public BatchBillingDtos.RunView exclude(String runNumber, Long itemId, boolean excluded) {
        BillingBatchRun run = preview(runNumber);
        BillingBatchRunItem item = items.findById(itemId).filter(found -> found.getRunId().equals(run.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable dans le lancement " + runNumber));
        item.setExcluded(excluded);
        items.save(item);
        return view(run);
    }

    /**
     * Emission : une facture par client non exclu, emise aussitot, chacune dans sa transaction ;
     * une erreur sur un client est notee sur sa ligne sans arreter les autres.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public BatchBillingDtos.RunView issue(String runNumber) {
        BillingBatchRun run = new TransactionTemplate(transactionManager).execute(status -> preview(runNumber));
        TransactionTemplate each = new TransactionTemplate(transactionManager);
        each.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        BillingDocumentService billing = billingServices.getObject();
        for (BillingBatchRunItem item : items.findByRunIdOrderByCustomerNameAsc(run.getId())) {
            if (Boolean.TRUE.equals(item.getExcluded()) || StringUtils.hasText(item.getInvoiceNumber())) {
                continue;
            }
            try {
                String invoice = each.execute(status -> {
                    BillingDocumentResponse created = billing.createInvoiceFromBillableItems(new CreateInvoiceFromBillableItemsRequest(run.getTitle(),
                            "Lancement " + run.getRunNumber(), LocalDate.now(), null, Arrays.asList(item.getBillableNumbers().split(","))));
                    return billing.issue(created.documentNumber()).documentNumber();
                });
                item.setInvoiceNumber(invoice);
                item.setError(null);
            } catch (RuntimeException exception) {
                item.setError(exception.getMessage() == null ? exception.getClass().getSimpleName()
                        : exception.getMessage().length() > 1000 ? exception.getMessage().substring(0, 1000) : exception.getMessage());
            }
            each.executeWithoutResult(status -> items.save(item));
        }
        return new TransactionTemplate(transactionManager).execute(status -> {
            BillingBatchRun fresh = runs.findByRunNumber(runNumber).orElseThrow();
            fresh.setStatus(ISSUED);
            fresh.setIssuedAt(Instant.now());
            fresh.setIssuedBy(staffProfiles.currentAuthor().login());
            return view(runs.save(fresh));
        });
    }

    public BatchBillingDtos.RunView cancel(String runNumber) {
        BillingBatchRun run = preview(runNumber);
        run.setStatus(CANCELLED);
        return view(runs.save(run));
    }

    @Transactional(readOnly = true)
    public BatchBillingDtos.RunView get(String runNumber) {
        return view(run(runNumber));
    }

    @Transactional(readOnly = true)
    public List<BatchBillingDtos.RunSummary> list() {
        return runs.findTop50ByOrderByCreatedAtDesc().stream().map(run -> {
            List<BillingBatchRunItem> lines = items.findByRunIdOrderByCustomerNameAsc(run.getId());
            return new BatchBillingDtos.RunSummary(run.getRunNumber(), run.getPeriodFrom(), run.getPeriodTo(), run.getTitle(), run.getStatus(),
                    lines.size(), lines.stream().filter(line -> StringUtils.hasText(line.getInvoiceNumber())).count(), run.getCreatedAt());
        }).toList();
    }

    // ------------------------------------------------------------------ outils

    /** Consommations deja retenues par un apercu en cours. */
    private Set<String> reservedBillables() {
        List<Long> previews = runs.findByStatus(PREVIEW).stream().map(BillingBatchRun::getId).toList();
        Set<String> reserved = new HashSet<>();
        if (!previews.isEmpty()) {
            items.findByRunIdIn(previews).forEach(item -> reserved.addAll(Arrays.asList(item.getBillableNumbers().split(","))));
        }
        return reserved;
    }

    private BillingBatchRun preview(String runNumber) {
        BillingBatchRun run = run(runNumber);
        if (!PREVIEW.equals(run.getStatus())) {
            throw ConflictException.coded("LANCEMENT_STATUT", "Le lancement " + runNumber + " n'est plus en aperçu.");
        }
        return run;
    }

    private BillingBatchRun run(String runNumber) {
        return runs.findByRunNumber(runNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Lancement introuvable : " + runNumber));
    }

    BatchBillingDtos.RunView view(BillingBatchRun run) {
        List<BatchBillingDtos.RunItem> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (BillingBatchRunItem item : items.findByRunIdOrderByCustomerNameAsc(run.getId())) {
            lines.add(new BatchBillingDtos.RunItem(item.getId(), item.getCustomerType(), item.getCustomerCode(), item.getCustomerName(),
                    item.getBillingCompany(), item.getItemCount(), item.getTotalAmount(), item.getCurrency(), Boolean.TRUE.equals(item.getExcluded()),
                    item.getInvoiceNumber(), item.getError()));
            if (!Boolean.TRUE.equals(item.getExcluded())) {
                total = total.add(item.getTotalAmount());
            }
        }
        return new BatchBillingDtos.RunView(run.getRunNumber(), run.getPeriodFrom(), run.getPeriodTo(), run.getTitle(), run.getStatus(),
                run.getCreatedBy(), run.getIssuedBy(), run.getIssuedAt(), total, lines);
    }
}

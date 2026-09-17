package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotQuarantineRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.NonConformanceRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityControlPlanRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityInspectionRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.NonConformanceResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityControlPlanResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityInspectionResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceDisposition;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceSeverity;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityControlStage;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import com.sni.bokaticowork.features.inventory.stock.model.*;
import com.sni.bokaticowork.features.inventory.stock.repository.NonConformanceRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.QualityControlPlanRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.QualityInspectionRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.QualityControlService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockQuarantineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QualityControlServiceImpl implements QualityControlService {

    private static final String PLAN_SEQUENCE = "quality_control_plan";
    private static final String INSPECTION_SEQUENCE = "quality_inspection";
    private static final String NON_CONFORMANCE_SEQUENCE = "non_conformance";

    private final QualityControlPlanRepository planRepository;
    private final QualityInspectionRepository inspectionRepository;
    private final NonConformanceRepository nonConformanceRepository;
    private final StockLotRepository lotRepository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryCategoryService categoryService;
    private final StockQuarantineService quarantineService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public QualityControlPlanResponse createPlan(QualityControlPlanRequest request) {
        boolean hasItem = StringUtils.hasText(request.getItemCode());
        boolean hasCategory = StringUtils.hasText(request.getCategoryCode());
        if (hasItem == hasCategory) {
            throw new BadRequestException("A control plan targets either an item or a category, not both nor neither");
        }

        QualityControlPlan plan = QualityControlPlan.builder()
                .planCode(sequenceGenerator.next(PLAN_SEQUENCE, LocalDate.now()))
                .name(request.getName().trim())
                .item(hasItem ? itemLookupService.findByItemCodeOrThrow(request.getItemCode()) : null)
                .category(hasCategory ? categoryService.findByCodeOrThrow(request.getCategoryCode()) : null)
                .controlStage(request.getControlStage())
                .samplingMode(request.getSamplingMode())
                .samplingParameter(request.getSamplingParameter())
                .decisionOnFail(request.getDecisionOnFail() == null
                        ? QualityDecision.QUARANTINED : request.getDecisionOnFail())
                .quarantineOnReceipt(Boolean.TRUE.equals(request.getQuarantineOnReceipt()))
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .criteria(new ArrayList<>())
                .build();

        int position = 1;
        for (QualityControlPlanRequest.Criterion criterion : request.getCriteria()) {
            plan.getCriteria().add(QualityCriterion.builder()
                    .plan(plan)
                    .criterionCode(normalize(criterion.getCriterionCode()))
                    .name(criterion.getName().trim())
                    .unit(trimToNull(criterion.getUnit()))
                    .minValue(criterion.getMinValue())
                    .maxValue(criterion.getMaxValue())
                    .booleanExpected(Boolean.TRUE.equals(criterion.getBooleanExpected()))
                    .blocking(criterion.getBlocking() == null ? Boolean.TRUE : criterion.getBlocking())
                    .position(criterion.getPosition() == null ? position : criterion.getPosition())
                    .build());
            position++;
        }

        return toResponse(planRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    public QualityControlPlanResponse getPlan(String planCode) {
        return toResponse(findPlanOrThrow(planCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityControlPlanResponse> listPlans() {
        return planRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Override
    public QualityInspectionResponse inspect(Long lotId, QualityInspectionRequest request) {
        StockLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock lot not found"));

        QualityControlPlan plan = resolvePlan(request.getPlanCode(), lot.getItem());
        Map<String, QualityCriterion> criteriaByCode = plan == null
                ? Map.of()
                : plan.getCriteria().stream()
                    .collect(Collectors.toMap(QualityCriterion::getCriterionCode, Function.identity()));

        QualityInspection inspection = QualityInspection.builder()
                .inspectionCode(sequenceGenerator.next(INSPECTION_SEQUENCE, LocalDate.now()))
                .plan(plan)
                .item(lot.getItem())
                .lot(lot)
                .location(lot.getLocation())
                .sampledQuantity(request.getSampledQuantity())
                .conformQuantity(request.getConformQuantity())
                .nonConformQuantity(request.getNonConformQuantity())
                .inspectedBy(trimToNull(request.getInspectedBy()))
                .inspectedAt(Instant.now())
                .decision(QualityDecision.ACCEPTED)
                .results(new ArrayList<>())
                .build();

        for (QualityInspectionRequest.Measure measure : request.getMeasures()) {
            QualityCriterion criterion = criteriaByCode.get(normalize(measure.getCriterionCode()));
            inspection.getResults().add(evaluate(inspection, criterion, measure));
        }

        boolean failed = inspection.hasBlockingFailure();
        QualityDecision decision = resolveDecision(plan, request, failed);
        inspection.setDecision(decision);
        inspection.setDecisionBy(trimToNull(request.getDecisionBy()));
        inspection.setDecisionReason(trimToNull(request.getDecisionReason()));

        boolean quarantined = applyDecision(lot, inspection, decision, failed);
        QualityInspection saved = inspectionRepository.save(inspection);

        log.info("Quality inspection {} on lot {} : decision {}, blocking failure {}",
                saved.getInspectionCode(), lot.getLotNumber(), decision, failed);
        return toResponse(saved, quarantined);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityInspectionResponse> inspectionsForLot(Long lotId) {
        StockLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock lot not found"));
        return inspectionRepository.findAllByLotOrderByInspectedAtDesc(lot).stream()
                .map(inspection -> toResponse(inspection, false))
                .toList();
    }

    @Override
    public boolean applyReceiptPlan(StockLot lot) {
        List<QualityControlPlan> plans =
                planRepository.findApplicable(lot.getItem(), QualityControlStage.ON_RECEIPT);

        // Sans plan demandant la quarantaine a la reception, la marchandise reste disponible : c est
        // le comportement d avant le lot 3, conserve pour tout ce qui n a rien declare.
        boolean shouldQuarantine = plans.stream()
                .anyMatch(plan -> Boolean.TRUE.equals(plan.getQuarantineOnReceipt()));
        if (!shouldQuarantine || Boolean.TRUE.equals(lot.getQuarantined())) {
            return false;
        }

        LotQuarantineRequest request = new LotQuarantineRequest();
        request.setReason("Mise en quarantaine automatique a la reception, plan de controle actif");
        request.setQuarantinedBy("system");
        quarantineService.quarantine(lot.getId(), request);
        return true;
    }

    @Override
    public NonConformanceResponse openNonConformance(NonConformanceRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        StockLot lot = request.getLotId() == null ? null
                : lotRepository.findById(request.getLotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stock lot not found"));

        NonConformance record = NonConformance.builder()
                .nonConformanceCode(sequenceGenerator.next(NON_CONFORMANCE_SEQUENCE, LocalDate.now()))
                .item(item)
                .lot(lot)
                .quantity(request.getQuantity())
                .severity(request.getSeverity())
                .description(request.getDescription().trim())
                .disposition(request.getDisposition() == null
                        ? NonConformanceDisposition.PENDING : request.getDisposition())
                .correctiveAction(trimToNull(request.getCorrectiveAction()))
                .responsibleCode(trimToNull(request.getResponsibleCode()))
                .dueDate(request.getDueDate())
                .supplierClaimCode(trimToNull(request.getSupplierClaimCode()))
                .costImpact(request.getCostImpact())
                .detectedBy(trimToNull(request.getDetectedBy()))
                .detectedAt(Instant.now())
                .build();

        return toResponse(nonConformanceRepository.save(record));
    }

    @Override
    public NonConformanceResponse closeNonConformance(String code, String closedBy, String correctiveAction) {
        NonConformance record = nonConformanceRepository.findByNonConformanceCode(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("Non conformance not found: " + code));
        if (!record.isOpen()) {
            throw new BadRequestException("Non conformance " + code + " is already closed");
        }
        if (record.getDisposition() == NonConformanceDisposition.PENDING) {
            throw new BadRequestException(
                    "Set a disposition before closing non conformance " + code);
        }

        record.setClosedBy(trimToNull(closedBy));
        record.setClosedAt(Instant.now());
        if (StringUtils.hasText(correctiveAction)) {
            record.setCorrectiveAction(correctiveAction.trim());
        }
        return toResponse(nonConformanceRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NonConformanceResponse> searchNonConformances(String itemCode, Boolean openOnly, Pageable pageable) {
        return nonConformanceRepository.search(normalizeOptional(itemCode), openOnly, pageable)
                .map(this::toResponse);
    }

    /**
     * Evalue une mesure contre son critere.
     *
     * <p>Une mesure sans critere correspondant est conservee mais consideree non bloquante : elle
     * documente une observation hors plan plutot que de faire echouer le controle.</p>
     */
    private QualityInspectionResult evaluate(QualityInspection inspection,
                                             QualityCriterion criterion,
                                             QualityInspectionRequest.Measure measure) {
        if (criterion == null) {
            return QualityInspectionResult.builder()
                    .inspection(inspection)
                    .criterionCode(normalize(measure.getCriterionCode()))
                    .criterionName(null)
                    .measuredValue(measure.getMeasuredValue())
                    .measuredFlag(measure.getMeasuredFlag())
                    .conform(Boolean.TRUE)
                    .blocking(Boolean.FALSE)
                    .notes(trimToNull(measure.getNotes()))
                    .build();
        }

        boolean conform = Boolean.TRUE.equals(criterion.getBooleanExpected())
                ? Boolean.TRUE.equals(measure.getMeasuredFlag())
                : criterion.accepts(measure.getMeasuredValue());

        return QualityInspectionResult.builder()
                .inspection(inspection)
                .criterionCode(criterion.getCriterionCode())
                .criterionName(criterion.getName())
                .measuredValue(measure.getMeasuredValue())
                .measuredFlag(measure.getMeasuredFlag())
                .conform(conform)
                .blocking(criterion.getBlocking())
                .notes(trimToNull(measure.getNotes()))
                .build();
    }

    /**
     * Deduit la decision : conforme, derogation explicite, ou ce que le plan prevoit en cas d echec.
     */
    private QualityDecision resolveDecision(QualityControlPlan plan,
                                            QualityInspectionRequest request,
                                            boolean failed) {
        if (!failed) {
            return QualityDecision.ACCEPTED;
        }
        if (Boolean.TRUE.equals(request.getAcceptByDerogation())) {
            // Une derogation engage quelqu un : sans nom ni motif, elle n a aucune valeur.
            if (!StringUtils.hasText(request.getDecisionBy()) || !StringUtils.hasText(request.getDecisionReason())) {
                throw new BadRequestException(
                        "A derogation requires both decisionBy and decisionReason");
            }
            return QualityDecision.ACCEPTED_BY_DEROGATION;
        }
        return plan == null ? QualityDecision.QUARANTINED : plan.getDecisionOnFail();
    }

    /**
     * Applique la decision au lot, et ouvre une non-conformite lorsque le controle a echoue.
     *
     * @return vrai si le lot a ete mis en quarantaine
     */
    private boolean applyDecision(StockLot lot, QualityInspection inspection,
                                  QualityDecision decision, boolean failed) {
        if (failed) {
            NonConformance record = nonConformanceRepository.save(NonConformance.builder()
                    .nonConformanceCode(sequenceGenerator.next(NON_CONFORMANCE_SEQUENCE, LocalDate.now()))
                    .item(lot.getItem())
                    .lot(lot)
                    .quantity(inspection.getNonConformQuantity())
                    .severity(decision == QualityDecision.REJECTED
                            ? NonConformanceSeverity.CRITICAL : NonConformanceSeverity.MAJOR)
                    .description("Ouverte automatiquement par l inspection " + inspection.getInspectionCode())
                    .disposition(NonConformanceDisposition.PENDING)
                    .detectedBy(inspection.getInspectedBy())
                    .detectedAt(Instant.now())
                    .build());
            inspection.setNonConformanceCode(record.getNonConformanceCode());
        }

        boolean needsQuarantine = decision == QualityDecision.QUARANTINED
                || decision == QualityDecision.REJECTED;
        if (needsQuarantine && !Boolean.TRUE.equals(lot.getQuarantined())) {
            LotQuarantineRequest quarantineRequest = new LotQuarantineRequest();
            quarantineRequest.setReason("Inspection " + inspection.getInspectionCode() + " : " + decision);
            quarantineRequest.setQuarantinedBy(inspection.getInspectedBy());
            quarantineService.quarantine(lot.getId(), quarantineRequest);
            return true;
        }

        // Un lot accepte, y compris par derogation, sort de quarantaine s il y etait.
        if (!needsQuarantine && Boolean.TRUE.equals(lot.getQuarantined())) {
            lot.setQuarantined(Boolean.FALSE);
            lot.setReleaseApprovedBy(inspection.getDecisionBy());
            lot.setReleasedAt(Instant.now());
            lotRepository.save(lot);
        }
        return false;
    }

    private QualityControlPlan resolvePlan(String planCode, InventoryItem item) {
        if (StringUtils.hasText(planCode)) {
            return findPlanOrThrow(planCode);
        }
        return planRepository.findApplicable(item, QualityControlStage.ON_RECEIPT).stream()
                .findFirst()
                .orElse(null);
    }

    private QualityControlPlan findPlanOrThrow(String planCode) {
        return planRepository.findByPlanCode(normalize(planCode))
                .orElseThrow(() -> new ResourceNotFoundException("Quality control plan not found: " + planCode));
    }

    private QualityControlPlanResponse toResponse(QualityControlPlan plan) {
        return QualityControlPlanResponse.builder()
                .planCode(plan.getPlanCode())
                .name(plan.getName())
                .itemCode(plan.getItem() == null ? null : plan.getItem().getItemCode())
                .categoryCode(plan.getCategory() == null ? null : plan.getCategory().getCode())
                .controlStage(plan.getControlStage())
                .samplingMode(plan.getSamplingMode())
                .samplingParameter(plan.getSamplingParameter())
                .decisionOnFail(plan.getDecisionOnFail())
                .quarantineOnReceipt(plan.getQuarantineOnReceipt())
                .active(plan.getActive())
                .criteria(plan.getCriteria().stream()
                        .map(criterion -> QualityControlPlanResponse.Criterion.builder()
                                .criterionCode(criterion.getCriterionCode())
                                .name(criterion.getName())
                                .unit(criterion.getUnit())
                                .minValue(criterion.getMinValue())
                                .maxValue(criterion.getMaxValue())
                                .booleanExpected(criterion.getBooleanExpected())
                                .blocking(criterion.getBlocking())
                                .position(criterion.getPosition())
                                .build())
                        .toList())
                .build();
    }

    private QualityInspectionResponse toResponse(QualityInspection inspection, boolean quarantined) {
        return QualityInspectionResponse.builder()
                .inspectionCode(inspection.getInspectionCode())
                .planCode(inspection.getPlan() == null ? null : inspection.getPlan().getPlanCode())
                .itemCode(inspection.getItem().getItemCode())
                .lotId(inspection.getLot() == null ? null : inspection.getLot().getId())
                .lotNumber(inspection.getLot() == null ? null : inspection.getLot().getLotNumber())
                .locationCode(inspection.getLocation() == null ? null : inspection.getLocation().getLocationCode())
                .sampledQuantity(inspection.getSampledQuantity())
                .conformQuantity(inspection.getConformQuantity())
                .nonConformQuantity(inspection.getNonConformQuantity())
                .decision(inspection.getDecision())
                .decisionBy(inspection.getDecisionBy())
                .decisionReason(inspection.getDecisionReason())
                .inspectedBy(inspection.getInspectedBy())
                .inspectedAt(inspection.getInspectedAt())
                .nonConformanceCode(inspection.getNonConformanceCode())
                .lotQuarantined(quarantined)
                .results(inspection.getResults().stream()
                        .map(result -> QualityInspectionResponse.Result.builder()
                                .criterionCode(result.getCriterionCode())
                                .criterionName(result.getCriterionName())
                                .measuredValue(result.getMeasuredValue())
                                .measuredFlag(result.getMeasuredFlag())
                                .conform(result.getConform())
                                .blocking(result.getBlocking())
                                .notes(result.getNotes())
                                .build())
                        .toList())
                .build();
    }

    private NonConformanceResponse toResponse(NonConformance record) {
        return NonConformanceResponse.builder()
                .nonConformanceCode(record.getNonConformanceCode())
                .itemCode(record.getItem().getItemCode())
                .lotId(record.getLot() == null ? null : record.getLot().getId())
                .lotNumber(record.getLot() == null ? null : record.getLot().getLotNumber())
                .sourceType(record.getSourceType() == null ? null : record.getSourceType().name())
                .sourceCode(record.getSourceCode())
                .quantity(record.getQuantity())
                .severity(record.getSeverity())
                .description(record.getDescription())
                .disposition(record.getDisposition())
                .correctiveAction(record.getCorrectiveAction())
                .responsibleCode(record.getResponsibleCode())
                .dueDate(record.getDueDate())
                .supplierClaimCode(record.getSupplierClaimCode())
                .costImpact(record.getCostImpact())
                .detectedBy(record.getDetectedBy())
                .detectedAt(record.getDetectedAt())
                .closedBy(record.getClosedBy())
                .closedAt(record.getClosedAt())
                .open(record.isOpen())
                .build();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Code is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? normalize(value) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}

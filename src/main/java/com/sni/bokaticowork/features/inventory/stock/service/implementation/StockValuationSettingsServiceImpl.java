package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.AdjustmentReasonRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryAdjustmentApprovalRuleRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.ItemValuationMethodRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.AdjustmentReasonResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryAdjustmentApprovalRuleResponse;
import com.sni.bokaticowork.features.inventory.stock.model.AdjustmentReason;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryAdjustmentApprovalRule;
import com.sni.bokaticowork.features.inventory.stock.repository.AdjustmentReasonRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryAdjustmentApprovalRuleRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockValuationService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockValuationSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockValuationSettingsServiceImpl implements StockValuationSettingsService {

    private final AdjustmentReasonRepository reasonRepository;
    private final InventoryAdjustmentApprovalRuleRepository approvalRuleRepository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryItemRepository itemRepository;
    private final StockValuationService valuationService;

    @Override
    public AdjustmentReasonResponse createReason(AdjustmentReasonRequest request) {
        String code = normalize(request.getReasonCode());
        if (reasonRepository.existsByReasonCode(code)) {
            throw new ResourceAlreadyExistException("Adjustment reason already exists: " + code);
        }
        if (Boolean.TRUE.equals(request.getNegativeOnly()) && Boolean.TRUE.equals(request.getPositiveOnly())) {
            throw new BadRequestException("An adjustment reason cannot be restricted to both senses");
        }

        AdjustmentReason reason = AdjustmentReason.builder()
                .reasonCode(code)
                .label(request.getLabel().trim())
                .counterpartAccount(request.getCounterpartAccount().trim())
                .negativeOnly(Boolean.TRUE.equals(request.getNegativeOnly()))
                .positiveOnly(Boolean.TRUE.equals(request.getPositiveOnly()))
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        return toResponse(reasonRepository.save(reason));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdjustmentReasonResponse> listReasons(Boolean active) {
        List<AdjustmentReason> reasons = Boolean.TRUE.equals(active)
                ? reasonRepository.findAllByActiveTrueOrderByLabelAsc()
                : reasonRepository.findAllByOrderByLabelAsc();
        return reasons.stream().map(this::toResponse).toList();
    }

    @Override
    public InventoryAdjustmentApprovalRuleResponse createApprovalRule(InventoryAdjustmentApprovalRuleRequest request) {
        if (request.getMaxAmount() != null && request.getMaxAmount() < request.getMinAmount()) {
            throw new BadRequestException("Approval rule max amount must not precede its min amount");
        }

        // Deux tranches qui se recouvrent rendraient le niveau requis dependant de l'ordre de lecture.
        approvalRuleRepository.findAllByActiveTrueOrderByMinAmountAsc().stream()
                .filter(existing -> overlaps(existing, request))
                .findFirst()
                .ifPresent(existing -> {
                    throw new BadRequestException("Approval rule overlaps with an existing range starting at "
                            + existing.getMinAmount());
                });

        InventoryAdjustmentApprovalRule rule = InventoryAdjustmentApprovalRule.builder()
                .approvalLevel(request.getApprovalLevel())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        return toResponse(approvalRuleRepository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAdjustmentApprovalRuleResponse> listApprovalRules() {
        return approvalRuleRepository.findAllByOrderByMinAmountAsc().stream().map(this::toResponse).toList();
    }

    @Override
    public int changeItemValuationMethod(String itemCode, ItemValuationMethodRequest request) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(itemCode);
        int seeded = valuationService.switchValuationMethod(item, request.getValuationMethod());
        itemRepository.save(item);

        // Le changement de methode est une decision comptable, pas un reglage technique : il doit
        // rester lisible dans les journaux d'exploitation.
        log.info("Valuation method of {} changed to {} by {} : {}",
                item.getItemCode(), request.getValuationMethod(),
                StringUtils.hasText(request.getChangedBy()) ? request.getChangedBy() : "unknown",
                StringUtils.hasText(request.getReason()) ? request.getReason() : "no reason given");
        return seeded;
    }

    private boolean overlaps(InventoryAdjustmentApprovalRule existing, InventoryAdjustmentApprovalRuleRequest request) {
        long newMin = request.getMinAmount();
        long newMax = request.getMaxAmount() == null ? Long.MAX_VALUE : request.getMaxAmount();
        long oldMin = existing.getMinAmount();
        long oldMax = existing.getMaxAmount() == null ? Long.MAX_VALUE : existing.getMaxAmount();
        return newMin <= oldMax && oldMin <= newMax;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Reason code is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(Locale.ROOT);
    }

    private AdjustmentReasonResponse toResponse(AdjustmentReason reason) {
        return AdjustmentReasonResponse.builder()
                .reasonCode(reason.getReasonCode())
                .label(reason.getLabel())
                .counterpartAccount(reason.getCounterpartAccount())
                .negativeOnly(reason.getNegativeOnly())
                .positiveOnly(reason.getPositiveOnly())
                .active(reason.getActive())
                .build();
    }

    private InventoryAdjustmentApprovalRuleResponse toResponse(InventoryAdjustmentApprovalRule rule) {
        return InventoryAdjustmentApprovalRuleResponse.builder()
                .id(rule.getId())
                .approvalLevel(rule.getApprovalLevel())
                .minAmount(rule.getMinAmount())
                .maxAmount(rule.getMaxAmount())
                .active(rule.getActive())
                .build();
    }
}

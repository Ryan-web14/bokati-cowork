package com.sni.bokaticowork.features.inventory.procurement.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.ReorderSuggestionResponse;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryReorderRuleService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.procurement.dto.ProcurementDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.enums.*;
import com.sni.bokaticowork.features.inventory.procurement.mapper.ProcurementMapper;
import com.sni.bokaticowork.features.inventory.procurement.model.*;
import com.sni.bokaticowork.features.inventory.procurement.repository.*;
import com.sni.bokaticowork.features.inventory.procurement.service.interfaces.ProcurementService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcurementServiceImpl implements ProcurementService {

    private final SupplierRepository supplierRepository;
    private final PurchaseApprovalRuleRepository approvalRuleRepository;
    private final PurchaseApprovalStepRepository approvalStepRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationService locationService;
    private final StockService stockService;
    private final InventoryReorderRuleService reorderRuleService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;
    private final ProcurementMapper mapper;

    @Override
    public SupplierResponse createSupplier(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(normalizeCode(code("supplier")));
        if (supplierRepository.existsBySupplierCode(supplier.getSupplierCode())) {
            throw new BadRequestException("Supplier code already exists");
        }
        applySupplier(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        publish("inventory.supplier.created", "INVENTORY_SUPPLIER", saved.getSupplierCode(), saved.getSupplierCode());
        return mapper.toSupplierResponse(saved);
    }

    @Override
    public SupplierResponse updateSupplier(String supplierCode, SupplierRequest request) {
        Supplier supplier = findSupplier(supplierCode);
        applySupplier(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        publish("inventory.supplier.updated", "INVENTORY_SUPPLIER", saved.getSupplierCode(), saved.getSupplierCode());
        return mapper.toSupplierResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> suppliers(String query, Pageable pageable) {
        Page<Supplier> suppliers = StringUtils.hasText(query)
                ? supplierRepository.search(query.trim().toUpperCase(Locale.ROOT), unsortedPage(pageable))
                : supplierRepository.findAll(pageable);
        return suppliers.map(mapper::toSupplierResponse);
    }

    @Override
    public PurchaseRequestResponse createPurchaseRequest(PurchaseRequestCreateRequest request) {
        PurchaseRequest pr = PurchaseRequest.builder()
                .requestCode(code("purchase_request"))
                .location(StringUtils.hasText(request.getLocationCode()) ? locationService.findByLocationCodeOrThrow(request.getLocationCode()) : null)
                .status(PurchaseRequestStatus.DRAFT)
                .requestedBy(trimToNull(request.getRequestedBy()))
                .build();
        request.getLines().forEach(line -> pr.getLines().add(PurchaseRequestLine.builder()
                .purchaseRequest(pr)
                .item(itemLookupService.findByItemCodeOrThrow(line.getItemCode()))
                .quantity(line.getQuantity())
                .estimatedUnitCost(line.getUnitCost())
                .build()));
        PurchaseRequest saved = purchaseRequestRepository.save(pr);
        publish("inventory.purchase_request.created", "PURCHASE_REQUEST", saved.getRequestCode(), saved.getRequestCode());
        return mapper.toPurchaseRequestResponse(saved);
    }

    @Override
    public PurchaseRequestResponse createPurchaseRequestFromReorderSuggestions(String locationCode, String supplierCode, String requestedBy) {
        String normalizedLocation = normalizeOptionalCode(locationCode);
        String normalizedSupplier = normalizeOptionalCode(supplierCode);
        List<ReorderSuggestionResponse> suggestions = reorderRuleService.suggestions(null, normalizedLocation, null).stream()
                .filter(suggestion -> normalizedLocation == null || normalizedLocation.equalsIgnoreCase(suggestion.getLocationCode()))
                .filter(suggestion -> normalizedSupplier == null || normalizedSupplier.equalsIgnoreCase(suggestion.getPreferredSupplierCode()))
                .filter(suggestion -> suggestion.getReorderQuantity() != null && suggestion.getReorderQuantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (suggestions.isEmpty()) {
            throw new BadRequestException("No reorder suggestion available for the requested filters");
        }
        if (normalizedSupplier != null) {
            findSupplier(normalizedSupplier);
        }
        PurchaseRequest pr = PurchaseRequest.builder()
                .requestCode(code("purchase_request"))
                .location(normalizedLocation == null ? null : locationService.findByLocationCodeOrThrow(normalizedLocation))
                .status(PurchaseRequestStatus.DRAFT)
                .requestedBy(trimToNull(requestedBy))
                .build();
        suggestions.forEach(suggestion -> pr.getLines().add(PurchaseRequestLine.builder()
                .purchaseRequest(pr)
                .item(itemLookupService.findByItemCodeOrThrow(suggestion.getItemCode()))
                .quantity(suggestion.getReorderQuantity())
                .build()));
        PurchaseRequest saved = purchaseRequestRepository.save(pr);
        publish("inventory.purchase_request.created_from_reorder", "PURCHASE_REQUEST", saved.getRequestCode(), saved.getRequestCode());
        return mapper.toPurchaseRequestResponse(saved);
    }

    @Override
    public PurchaseRequestResponse submitPurchaseRequest(String requestCode) {
        PurchaseRequest pr = findPurchaseRequestForUpdate(requestCode);
        require(pr.getStatus() == PurchaseRequestStatus.DRAFT, "Purchase request must be DRAFT");
        pr.setStatus(PurchaseRequestStatus.SUBMITTED);
        PurchaseRequest saved = purchaseRequestRepository.save(pr);
        publish("inventory.purchase_request.submitted", "PURCHASE_REQUEST", saved.getRequestCode(), saved.getRequestCode());
        return mapper.toPurchaseRequestResponse(saved);
    }

    @Override
    public PurchaseRequestResponse approvePurchaseRequest(String requestCode, String approvedBy, String supplierCode) {
        PurchaseRequest pr = findPurchaseRequestForUpdate(requestCode);
        require(pr.getStatus() == PurchaseRequestStatus.SUBMITTED, "Purchase request must be SUBMITTED");
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        pr.setApprovedBy(trimToNull(approvedBy));
        PurchaseRequest saved = purchaseRequestRepository.save(pr);
        publish("inventory.purchase_request.approved", "PURCHASE_REQUEST", saved.getRequestCode(), saved.getRequestCode());

        String autoCreatedOrderCode = null;
        if (StringUtils.hasText(supplierCode)) {
            PurchaseOrderResponse po = createPurchaseOrderFromRequest(saved.getRequestCode(), supplierCode);
            autoCreatedOrderCode = po.getOrderCode();
        }

        PurchaseRequestResponse response = mapper.toPurchaseRequestResponse(saved);
        response.setAutoCreatedOrderCode(autoCreatedOrderCode);
        return response;
    }

    @Override
    public PurchaseRequestResponse rejectPurchaseRequest(String requestCode, String reason) {
        PurchaseRequest pr = findPurchaseRequestForUpdate(requestCode);
        require(pr.getStatus() == PurchaseRequestStatus.SUBMITTED, "Purchase request must be SUBMITTED");
        pr.setStatus(PurchaseRequestStatus.REJECTED);
        pr.setRejectionReason(trimToNull(reason));
        PurchaseRequest saved = purchaseRequestRepository.save(pr);
        publish("inventory.purchase_request.rejected", "PURCHASE_REQUEST", saved.getRequestCode(), saved.getRequestCode());
        return mapper.toPurchaseRequestResponse(saved);
    }

    @Override
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request) {
        Supplier supplier = findSupplier(request.getSupplierCode());
        PurchaseOrder po = PurchaseOrder.builder()
                .orderCode(code("purchase_order"))
                .supplier(supplier)
                .location(StringUtils.hasText(request.getLocationCode()) ? locationService.findByLocationCodeOrThrow(request.getLocationCode()) : null)
                .sourceRequestCode(normalizeOptionalCode(request.getSourceRequestCode()))
                .status(PurchaseOrderStatus.DRAFT)
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .orderedBy(trimToNull(request.getOrderedBy()))
                .build();
        request.getLines().forEach(line -> addOrderLine(po, line));
        applyPurchaseOrderTotals(po);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        publish("inventory.purchase_order.created", "PURCHASE_ORDER", saved.getOrderCode(), saved.getOrderCode());
        return mapper.toPurchaseOrderResponse(saved);
    }

    @Override
    public PurchaseOrderResponse createPurchaseOrderFromRequest(String requestCode, String supplierCode) {
        PurchaseRequest pr = findPurchaseRequestForUpdate(requestCode);
        require(pr.getStatus() == PurchaseRequestStatus.APPROVED, "Purchase request must be APPROVED");
        Supplier supplier = findSupplier(supplierCode);
        PurchaseOrder po = PurchaseOrder.builder()
                .orderCode(code("purchase_order"))
                .supplier(supplier)
                .location(pr.getLocation())
                .sourceRequestCode(pr.getRequestCode())
                .status(PurchaseOrderStatus.DRAFT)
                .orderedBy(pr.getApprovedBy())
                .build();
        pr.getLines().forEach(line -> po.getLines().add(PurchaseOrderLine.builder()
                .purchaseOrder(po)
                .item(line.getItem())
                .orderedQuantity(line.getQuantity())
                .receivedQuantity(BigDecimal.ZERO)
                .unitCost(line.getEstimatedUnitCost())
                .build()));
        applyPurchaseOrderTotals(po);
        pr.setStatus(PurchaseRequestStatus.CONVERTED);
        purchaseRequestRepository.save(pr);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        publish("inventory.purchase_order.created", "PURCHASE_ORDER", saved.getOrderCode(), saved.getOrderCode());
        return mapper.toPurchaseOrderResponse(saved);
    }

    @Override
    public PurchaseOrderResponse approvePurchaseOrder(String orderCode, String approvedBy) {
        PurchaseOrder po = findPurchaseOrderForUpdate(orderCode);
        require(po.getStatus() == PurchaseOrderStatus.DRAFT, "Purchase order must be DRAFT");
        require(StringUtils.hasText(approvedBy), "Approver is required");
        applyPurchaseOrderTotals(po);
        if (approvalStepRepository.existsByPurchaseOrderAndApprovedByIgnoreCase(po, approvedBy.trim())) {
            throw new BadRequestException("Approver has already approved this purchase order");
        }
        approvalStepRepository.save(PurchaseApprovalStep.builder()
                .purchaseOrder(po)
                .approvalLevel(po.getApprovalLevel())
                .approvedBy(approvedBy.trim())
                .approvedAt(Instant.now())
                .build());
        long approvals = approvalStepRepository.countByPurchaseOrder(po);
        int requiredApprovals = requiredApprovals(po.getApprovalLevel());
        if (approvals >= requiredApprovals) {
            po.setStatus(PurchaseOrderStatus.APPROVED);
            po.setApprovedBy(trimToNull(approvedBy));
            po.setApprovedAt(Instant.now());
        }
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        publish("inventory.purchase_order.approved", "PURCHASE_ORDER", saved.getOrderCode(), saved.getOrderCode());
        return enrichApprovalSteps(saved);
    }

    @Override
    public PurchaseOrderResponse markOrdered(String orderCode) {
        PurchaseOrder po = findPurchaseOrderForUpdate(orderCode);
        require(po.getStatus() == PurchaseOrderStatus.APPROVED, "Purchase order must be APPROVED");
        po.setStatus(PurchaseOrderStatus.ORDERED);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        publish("inventory.purchase_order.ordered", "PURCHASE_ORDER", saved.getOrderCode(), saved.getOrderCode());
        return mapper.toPurchaseOrderResponse(saved);
    }

    @Override
    public GoodsReceiptResponse receiveGoods(GoodsReceiptRequest request) {
        PurchaseOrder po = StringUtils.hasText(request.getOrderCode()) ? findPurchaseOrderForUpdate(request.getOrderCode()) : null;
        if (po != null) {
            require(po.getStatus() == PurchaseOrderStatus.ORDERED || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED,
                    "Purchase order must be ORDERED or PARTIALLY_RECEIVED");
        }
        InventoryLocation location = locationService.findByLocationCodeOrThrow(request.getLocationCode());
        GoodsReceipt receipt = GoodsReceipt.builder()
                .receiptCode(code("goods_receipt"))
                .purchaseOrder(po)
                .location(location)
                .status(GoodsReceiptStatus.POSTED)
                .invoiceDocumentCode(normalizeOptionalCode(request.getInvoiceDocumentCode()))
                .deliveryNoteDocumentCode(normalizeOptionalCode(request.getDeliveryNoteDocumentCode()))
                .proofDocumentCode(normalizeOptionalCode(request.getProofDocumentCode()))
                .receivedBy(trimToNull(request.getReceivedBy()))
                .receivedAt(Instant.now())
                .postedAt(Instant.now())
                .build();
        for (GoodsReceiptLineRequest line : request.getLines()) {
            InventoryItem item = itemLookupService.findByItemCodeOrThrow(line.getItemCode());
            BigDecimal rejectedQuantity = line.getRejectedQuantity() == null ? BigDecimal.ZERO : line.getRejectedQuantity();
            require(rejectedQuantity.compareTo(BigDecimal.ZERO) >= 0, "Rejected quantity cannot be negative");
            require(rejectedQuantity.compareTo(line.getReceivedQuantity()) <= 0, "Rejected quantity cannot exceed received quantity");
            boolean qualityAccepted = line.getQualityAccepted() == null || line.getQualityAccepted();
            BigDecimal acceptedQuantity = qualityAccepted ? line.getReceivedQuantity().subtract(rejectedQuantity) : BigDecimal.ZERO;
            BigDecimal backorderQuantity = updatePurchaseOrderReceivedQuantity(po, item, acceptedQuantity);
            receipt.getLines().add(GoodsReceiptLine.builder()
                    .goodsReceipt(receipt)
                    .item(item)
                    .receivedQuantity(line.getReceivedQuantity())
                    .rejectedQuantity(rejectedQuantity)
                    .rejectionReason(trimToNull(line.getRejectionReason()))
                    .qualityAccepted(qualityAccepted)
                    .backorderQuantity(backorderQuantity)
                    .unitCost(line.getUnitCost())
                    .lotNumber(normalizeOptionalCode(line.getLotNumber()))
                    .expiryDate(line.getExpiryDate())
                    .build());
            if (acceptedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                StockInRequest stockIn = new StockInRequest();
                stockIn.setItemCode(item.getItemCode());
                stockIn.setLocationCode(location.getLocationCode());
                stockIn.setQuantity(acceptedQuantity);
                stockIn.setUnitCost(line.getUnitCost());
                stockIn.setLotNumber(line.getLotNumber());
                stockIn.setExpiryDate(line.getExpiryDate());
                stockIn.setQuarantined(Boolean.TRUE.equals(line.getQuarantineAcceptedStock()));
                stockIn.setQuarantineReason(line.getQuarantineReason());
                stockIn.setOwnershipType(line.getOwnershipType());
                stockIn.setOwnerCode(line.getOwnerCode());
                stockIn.setReferenceType(StockReferenceType.GOODS_RECEIPT);
                stockIn.setReferenceCode(receipt.getReceiptCode());
                stockIn.setReason("Goods receipt posting");
                stockIn.setPerformedBy(request.getReceivedBy());
                stockService.receive(stockIn);
            }
        }
        if (po != null) {
            po.setStatus(allReceived(po) ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
            purchaseOrderRepository.save(po);
        }
        GoodsReceipt saved = goodsReceiptRepository.save(receipt);
        publish("inventory.goods_receipt.posted", "GOODS_RECEIPT", saved.getReceiptCode(), saved.getReceiptCode());
        return mapper.toGoodsReceiptResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> purchaseRequests(PurchaseRequestStatus status, String locationCode, String query, Pageable pageable) {
        return purchaseRequestRepository.search(status == null ? null : status.name(), normalizeOptionalCode(locationCode), upperQuery(query), unsortedPage(pageable))
                .map(mapper::toPurchaseRequestResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> purchaseOrders(PurchaseOrderStatus status, String supplierCode, String locationCode, String query, Pageable pageable) {
        return purchaseOrderRepository.search(status == null ? null : status.name(), normalizeOptionalCode(supplierCode), normalizeOptionalCode(locationCode), upperQuery(query), unsortedPage(pageable))
                .map(mapper::toPurchaseOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GoodsReceiptResponse> goodsReceipts(GoodsReceiptStatus status, String locationCode, String orderCode, String query, Pageable pageable) {
        return goodsReceiptRepository.search(status == null ? null : status.name(), normalizeOptionalCode(locationCode), normalizeOptionalCode(orderCode), upperQuery(query), unsortedPage(pageable))
                .map(mapper::toGoodsReceiptResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierPerformanceResponse supplierPerformance(String supplierCode) {
        Supplier supplier = findSupplier(supplierCode);
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllBySupplier(supplier);
        List<GoodsReceipt> receipts = goodsReceiptRepository.findAllByPurchaseOrder_Supplier(supplier);
        long receivedOrders = orders.stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.RECEIVED || order.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .count();
        long completeReceipts = orders.stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.RECEIVED)
                .count();
        Double averageDelay = receipts.stream()
                .filter(receipt -> receipt.getPurchaseOrder() != null && receipt.getPurchaseOrder().getExpectedDeliveryDate() != null)
                .mapToLong(receipt -> ChronoUnit.DAYS.between(
                        receipt.getPurchaseOrder().getExpectedDeliveryDate(),
                        receipt.getReceivedAt().atZone(ZoneOffset.UTC).toLocalDate()))
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
        Long averageUnitCost = orders.stream()
                .flatMap(order -> order.getLines().stream())
                .map(PurchaseOrderLine::getUnitCost)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .stream()
                .mapToLong(value -> Math.round(value))
                .boxed()
                .findFirst()
                .orElse(null);
        return SupplierPerformanceResponse.builder()
                .supplierCode(supplier.getSupplierCode())
                .supplierName(supplier.getName())
                .purchaseOrderCount(orders.size())
                .receivedOrderCount(receivedOrders)
                .completeReceiptCount(completeReceipts)
                .completeReceiptRate(orders.isEmpty() ? 0 : (double) completeReceipts / orders.size())
                .averageDeliveryDelayDays(averageDelay)
                .averageUnitCost(averageUnitCost)
                .build();
    }

    @Override
    public PurchaseApprovalRuleResponse saveApprovalRule(PurchaseApprovalRuleRequest request) {
        if (request.getMinAmount() < 0 || (request.getMaxAmount() != null && request.getMaxAmount() < request.getMinAmount())) {
            throw new BadRequestException("Invalid approval amount range");
        }
        PurchaseApprovalRule rule = approvalRuleRepository.findByApprovalLevel(request.getApprovalLevel()).orElseGet(PurchaseApprovalRule::new);
        rule.setApprovalLevel(request.getApprovalLevel());
        rule.setMinAmount(request.getMinAmount());
        rule.setMaxAmount(request.getMaxAmount());
        rule.setRequiredApprovals(request.getRequiredApprovals());
        rule.setActive(request.getActive() == null || request.getActive());
        return mapper.toApprovalRuleResponse(approvalRuleRepository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseApprovalRuleResponse> approvalRules() {
        return approvalRuleRepository.findAllByActiveTrueOrderByMinAmountAsc().stream()
                .map(mapper::toApprovalRuleResponse)
                .toList();
    }

    private void applySupplier(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.getName().trim());
        supplier.setEmail(trimToNull(request.getEmail()));
        supplier.setPhone(trimToNull(request.getPhone()));
        supplier.setTaxId(normalizeOptionalCode(request.getTaxId()));
        supplier.setAddress(trimToNull(request.getAddress()));
        supplier.setStatus(request.getStatus() == null ? SupplierStatus.ACTIVE : request.getStatus());
    }

    private void addOrderLine(PurchaseOrder po, PurchaseLineRequest line) {
        po.getLines().add(PurchaseOrderLine.builder()
                .purchaseOrder(po)
                .item(itemLookupService.findByItemCodeOrThrow(line.getItemCode()))
                .orderedQuantity(line.getQuantity())
                .receivedQuantity(BigDecimal.ZERO)
                .unitCost(line.getUnitCost())
                .build());
    }

    private BigDecimal updatePurchaseOrderReceivedQuantity(PurchaseOrder po, InventoryItem item, BigDecimal quantity) {
        if (po == null) return null;
        PurchaseOrderLine line = po.getLines().stream()
                .filter(candidate -> candidate.getItem().getId().equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Received item is not present on purchase order"));
        BigDecimal newQuantity = line.getReceivedQuantity().add(quantity);
        if (newQuantity.compareTo(line.getOrderedQuantity()) > 0) {
            throw new BadRequestException("Received quantity exceeds ordered quantity");
        }
        line.setReceivedQuantity(newQuantity);
        return line.getOrderedQuantity().subtract(newQuantity);
    }

    private boolean allReceived(PurchaseOrder po) {
        return po.getLines().stream().allMatch(line -> line.getReceivedQuantity().compareTo(line.getOrderedQuantity()) >= 0);
    }

    private void applyPurchaseOrderTotals(PurchaseOrder po) {
        long total = po.getLines().stream()
                .mapToLong(line -> {
                    if (line.getUnitCost() == null || line.getOrderedQuantity() == null) return 0L;
                    return line.getOrderedQuantity().multiply(BigDecimal.valueOf(line.getUnitCost())).longValue();
                })
                .sum();
        po.setTotalAmount(total);
        po.setApprovalLevel(resolveApprovalLevel(total));
    }

    private PurchaseApprovalLevel resolveApprovalLevel(long totalAmount) {
        for (PurchaseApprovalRule rule : approvalRuleRepository.findAllByActiveTrueOrderByMinAmountAsc()) {
            boolean minOk = totalAmount >= rule.getMinAmount();
            boolean maxOk = rule.getMaxAmount() == null || totalAmount <= rule.getMaxAmount();
            if (minOk && maxOk) {
                return rule.getApprovalLevel();
            }
        }
        if (totalAmount <= 0) return PurchaseApprovalLevel.NONE;
        if (totalAmount < 500_000L) return PurchaseApprovalLevel.MANAGER;
        if (totalAmount < 5_000_000L) return PurchaseApprovalLevel.DIRECTOR;
        return PurchaseApprovalLevel.EXECUTIVE;
    }

    private int requiredApprovals(PurchaseApprovalLevel level) {
        return approvalRuleRepository.findByApprovalLevel(level)
                .map(PurchaseApprovalRule::getRequiredApprovals)
                .orElseGet(() -> switch (level) {
                    case EXECUTIVE -> 3;
                    case DIRECTOR -> 2;
                    case MANAGER -> 1;
                    case NONE -> 1;
                });
    }

    private PurchaseOrderResponse enrichApprovalSteps(PurchaseOrder po) {
        PurchaseOrderResponse response = mapper.toPurchaseOrderResponse(po);
        response.setApprovalSteps(approvalStepRepository.findAllByPurchaseOrderOrderByApprovedAtAsc(po).stream()
                .map(mapper::toApprovalStepResponse)
                .toList());
        return response;
    }

    private Supplier findSupplier(String supplierCode) {
        return supplierRepository.findBySupplierCode(normalizeCode(supplierCode))
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    private PurchaseRequest findPurchaseRequestForUpdate(String requestCode) {
        return purchaseRequestRepository.findByRequestCodeForUpdate(normalizeCode(requestCode))
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request not found"));
    }

    private PurchaseOrder findPurchaseOrderForUpdate(String orderCode) {
        return purchaseOrderRepository.findByOrderCodeForUpdate(normalizeCode(orderCode))
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));
    }

    private String code(String sequenceCode) {
        return sequenceGenerator.next(sequenceCode, LocalDate.now()) + "-" + System.currentTimeMillis();
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Code is required");
        return normalizeOptionalCode(value);
    }

    private String normalizeOptionalCode(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toUpperCase(Locale.ROOT) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void require(boolean expression, String message) {
        if (!expression) throw new BadRequestException(message);
    }

    private void publish(String eventType, String aggregateType, String aggregateId, String code) {
        outboxService.publish(eventType, aggregateType, aggregateId, java.util.Map.of("code", code));
    }

    private String upperQuery(String query) {
        return StringUtils.hasText(query) ? query.trim().toUpperCase(Locale.ROOT) : null;
    }

    private Pageable unsortedPage(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}

package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockTransferRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockTransferWorkflowRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockTransferWorkflowResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockTransferWorkflowStatus;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockTransferWorkflow;
import com.sni.bokaticowork.features.inventory.stock.repository.StockTransferWorkflowRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockTransferWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class StockTransferWorkflowServiceImpl implements StockTransferWorkflowService {

    private final StockTransferWorkflowRepository repository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationService locationService;
    private final StockService stockService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public StockTransferWorkflowResponse request(StockTransferWorkflowRequest request) {
        if (request.getFromLocationCode().equalsIgnoreCase(request.getToLocationCode())) {
            throw new BadRequestException("Transfer source and destination must be different");
        }
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(request.getItemCode());
        InventoryLocation from = locationService.findByLocationCodeOrThrow(request.getFromLocationCode());
        InventoryLocation to = locationService.findByLocationCodeOrThrow(request.getToLocationCode());
        StockTransferWorkflow transfer = StockTransferWorkflow.builder()
                .transferCode(nextCode())
                .item(item)
                .fromLocation(from)
                .toLocation(to)
                .quantity(request.getQuantity())
                .status(StockTransferWorkflowStatus.REQUESTED)
                .requestedBy(trimToNull(request.getRequestedBy()))
                .reason(trimToNull(request.getReason()))
                .requestedAt(Instant.now())
                .build();
        return toResponse(repository.save(transfer));
    }

    @Override
    public StockTransferWorkflowResponse approve(String transferCode, String approvedBy) {
        StockTransferWorkflow transfer = findForUpdate(transferCode);
        require(transfer.getStatus() == StockTransferWorkflowStatus.REQUESTED, "Transfer must be REQUESTED");
        transfer.setStatus(StockTransferWorkflowStatus.APPROVED);
        transfer.setApprovedBy(trimToNull(approvedBy));
        transfer.setApprovedAt(Instant.now());
        return toResponse(repository.save(transfer));
    }

    @Override
    public StockTransferWorkflowResponse ship(String transferCode, String shippedBy) {
        StockTransferWorkflow transfer = findForUpdate(transferCode);
        require(transfer.getStatus() == StockTransferWorkflowStatus.APPROVED, "Transfer must be APPROVED");
        transfer.setStatus(StockTransferWorkflowStatus.SHIPPED);
        transfer.setShippedBy(trimToNull(shippedBy));
        transfer.setShippedAt(Instant.now());
        return toResponse(repository.save(transfer));
    }

    @Override
    public StockTransferWorkflowResponse receive(String transferCode, String receivedBy) {
        StockTransferWorkflow transfer = findForUpdate(transferCode);
        require(transfer.getStatus() == StockTransferWorkflowStatus.SHIPPED, "Transfer must be SHIPPED");
        StockTransferRequest request = new StockTransferRequest();
        request.setItemCode(transfer.getItem().getItemCode());
        request.setFromLocationCode(transfer.getFromLocation().getLocationCode());
        request.setToLocationCode(transfer.getToLocation().getLocationCode());
        request.setQuantity(transfer.getQuantity());
        request.setReferenceType(StockReferenceType.OTHER);
        request.setReferenceCode(transfer.getTransferCode());
        request.setReason("Approved stock transfer " + transfer.getTransferCode());
        request.setPerformedBy(receivedBy);
        StockMovementResponse movement = stockService.transfer(request);
        transfer.setStatus(StockTransferWorkflowStatus.RECEIVED);
        transfer.setReceivedBy(trimToNull(receivedBy));
        transfer.setReceivedAt(Instant.now());
        transfer.setMovementCode(movement.getMovementCode());
        return toResponse(repository.save(transfer));
    }

    @Override
    public StockTransferWorkflowResponse cancel(String transferCode, String cancelledBy, String reason) {
        StockTransferWorkflow transfer = findForUpdate(transferCode);
        require(transfer.getStatus() != StockTransferWorkflowStatus.RECEIVED, "Received transfer cannot be cancelled");
        transfer.setStatus(StockTransferWorkflowStatus.CANCELLED);
        transfer.setReason(trimToNull(reason) == null ? transfer.getReason() : trimToNull(reason));
        return toResponse(repository.save(transfer));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockTransferWorkflowResponse> list(StockTransferWorkflowStatus status, Pageable pageable) {
        Page<StockTransferWorkflow> page = status == null ? repository.findAll(pageable) : repository.findAllByStatus(status, pageable);
        return page.map(this::toResponse);
    }

    private StockTransferWorkflow findForUpdate(String transferCode) {
        return repository.findByTransferCodeForUpdate(normalize(transferCode))
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found"));
    }

    private StockTransferWorkflowResponse toResponse(StockTransferWorkflow transfer) {
        return StockTransferWorkflowResponse.builder()
                .transferCode(transfer.getTransferCode())
                .itemCode(transfer.getItem().getItemCode())
                .itemName(transfer.getItem().getName())
                .fromLocationCode(transfer.getFromLocation().getLocationCode())
                .toLocationCode(transfer.getToLocation().getLocationCode())
                .quantity(transfer.getQuantity())
                .status(transfer.getStatus())
                .requestedBy(transfer.getRequestedBy())
                .approvedBy(transfer.getApprovedBy())
                .shippedBy(transfer.getShippedBy())
                .receivedBy(transfer.getReceivedBy())
                .movementCode(transfer.getMovementCode())
                .reason(transfer.getReason())
                .requestedAt(transfer.getRequestedAt())
                .approvedAt(transfer.getApprovedAt())
                .shippedAt(transfer.getShippedAt())
                .receivedAt(transfer.getReceivedAt())
                .build();
    }

    private String nextCode() {
        String code;
        do {
            code = sequenceGenerator.next("stock_transfer", LocalDate.now()) + "-" + System.currentTimeMillis();
        } while (repository.existsByTransferCode(code));
        return code;
    }

    private void require(boolean expression, String message) {
        if (!expression) throw new BadRequestException(message);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) throw new BadRequestException("Code is required");
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

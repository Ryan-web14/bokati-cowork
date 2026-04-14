package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockTransferWorkflowRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockTransferWorkflowResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockTransferWorkflowStatus;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockTransferWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/stock/transfers")
public class StockTransferWorkflowController {

    private final StockTransferWorkflowService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "STOCK_TRANSFER_REQUEST", ressource = "stock_transfer")
    @Idempotent(operation = "INVENTORY_STOCK_TRANSFER_REQUEST", required = false)
    public ResponseEntity<StockTransferWorkflowResponse> request(@Valid @RequestBody StockTransferWorkflowRequest request) {
        return ResponseEntity.ok(service.request(request));
    }

    @PatchMapping("/{transferCode}/approve")
    public ResponseEntity<StockTransferWorkflowResponse> approve(@PathVariable String transferCode,
                                                                 @RequestParam(required = false) String approvedBy) {
        return ResponseEntity.ok(service.approve(transferCode, approvedBy));
    }

    @PatchMapping("/{transferCode}/ship")
    public ResponseEntity<StockTransferWorkflowResponse> ship(@PathVariable String transferCode,
                                                              @RequestParam(required = false) String shippedBy) {
        return ResponseEntity.ok(service.ship(transferCode, shippedBy));
    }

    @PatchMapping("/{transferCode}/receive")
    public ResponseEntity<StockTransferWorkflowResponse> receive(@PathVariable String transferCode,
                                                                 @RequestParam(required = false) String receivedBy) {
        return ResponseEntity.ok(service.receive(transferCode, receivedBy));
    }

    @PatchMapping("/{transferCode}/cancel")
    public ResponseEntity<StockTransferWorkflowResponse> cancel(@PathVariable String transferCode,
                                                                @RequestParam(required = false) String cancelledBy,
                                                                @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.cancel(transferCode, cancelledBy, reason));
    }

    @GetMapping
    public ResponseEntity<Page<StockTransferWorkflowResponse>> list(@RequestParam(required = false) StockTransferWorkflowStatus status,
                                                                    @PageableDefault(size = 20, sort = "requestedAt") Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }
}

package com.sni.bokaticowork.features.inventory.asset.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetAssignRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetReserveRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetReturnRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetAssignmentResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetLocationHistoryResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetResponse;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.asset.service.interfaces.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/assets")
public class AssetController {

    private final AssetService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "CREATE_ASSET", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_CREATE", required = false)
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{assetCode}")
    @Audited(module = "INVENTORY", action = "UPDATE_ASSET", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_UPDATE", required = false)
    public ResponseEntity<AssetResponse> update(@PathVariable String assetCode, @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(service.update(assetCode, request));
    }

    @GetMapping("/{assetCode}")
    public ResponseEntity<AssetResponse> get(@PathVariable String assetCode) {
        return ResponseEntity.ok(service.get(assetCode));
    }

    @GetMapping
    public ResponseEntity<Page<AssetResponse>> search(@RequestParam(required = false, name = "q") String query,
                                                      @RequestParam(required = false) String itemCode,
                                                      @RequestParam(required = false) AssetStatus status,
                                                      @RequestParam(required = false) String locationCode,
                                                      @RequestParam(required = false) AssetAssigneeType assignedToType,
                                                      @RequestParam(required = false) String assignedToCode,
                                                      @PageableDefault(size = 20, sort = "assetCode") Pageable pageable) {
        return ResponseEntity.ok(service.search(query, itemCode, status, locationCode, assignedToType, assignedToCode, pageable));
    }

    @PatchMapping("/{assetCode}/assign")
    @Audited(module = "INVENTORY", action = "ASSIGN_ASSET", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_ASSIGN", required = false)
    public ResponseEntity<AssetAssignmentResponse> assign(@PathVariable String assetCode,
                                                          @Valid @RequestBody AssetAssignRequest request) {
        return ResponseEntity.ok(service.assign(assetCode, request));
    }

    @PatchMapping("/{assetCode}/reserve")
    @Audited(module = "INVENTORY", action = "RESERVE_ASSET", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_RESERVE", required = false)
    public ResponseEntity<AssetAssignmentResponse> reserve(@PathVariable String assetCode,
                                                           @Valid @RequestBody AssetReserveRequest request) {
        return ResponseEntity.ok(service.reserve(assetCode, request));
    }

    @PatchMapping("/{assetCode}/cancel-reservation")
    @Audited(module = "INVENTORY", action = "CANCEL_ASSET_RESERVATION", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_CANCEL_RESERVATION", required = false)
    public ResponseEntity<AssetAssignmentResponse> cancelReservation(@PathVariable String assetCode,
                                                                     @RequestParam(required = false) String cancelledBy,
                                                                     @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.cancelReservation(assetCode, cancelledBy, reason));
    }

    @PatchMapping("/{assetCode}/return")
    @Audited(module = "INVENTORY", action = "RETURN_ASSET", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_RETURN", required = false)
    public ResponseEntity<AssetAssignmentResponse> returnAsset(@PathVariable String assetCode,
                                                               @RequestBody AssetReturnRequest request) {
        return ResponseEntity.ok(service.returnAsset(assetCode, request));
    }

    @PatchMapping("/{assetCode}/mark-lost")
    @Audited(module = "INVENTORY", action = "MARK_ASSET_LOST", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_MARK_LOST", required = false)
    public ResponseEntity<AssetResponse> markLost(@PathVariable String assetCode) {
        return ResponseEntity.ok(service.markLost(assetCode));
    }

    @PatchMapping("/{assetCode}/mark-damaged")
    @Audited(module = "INVENTORY", action = "MARK_ASSET_DAMAGED", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_MARK_DAMAGED", required = false)
    public ResponseEntity<AssetResponse> markDamaged(@PathVariable String assetCode) {
        return ResponseEntity.ok(service.markDamaged(assetCode));
    }

    @PatchMapping("/{assetCode}/retire")
    @Audited(module = "INVENTORY", action = "RETIRE_ASSET", ressource = "asset")
    @Idempotent(operation = "INVENTORY_ASSET_RETIRE", required = false)
    public ResponseEntity<AssetResponse> retire(@PathVariable String assetCode) {
        return ResponseEntity.ok(service.retire(assetCode));
    }

    @GetMapping("/{assetCode}/location-history")
    public ResponseEntity<List<AssetLocationHistoryResponse>> locationHistory(@PathVariable String assetCode) {
        return ResponseEntity.ok(service.locationHistory(assetCode));
    }

    @GetMapping("/{assetCode}/assignments/{assignmentId}/loan-sheet.pdf")
    public ResponseEntity<byte[]> loanSheetPdf(@PathVariable String assetCode, @PathVariable Long assignmentId) {
        byte[] pdf = service.loanSheetPdf(assetCode, assignmentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "loan-sheet-" + assetCode + "-" + assignmentId + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}

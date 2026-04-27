package com.sni.bokaticowork.features.subscription.usage.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.UsageRecordResponse;
import com.sni.bokaticowork.features.subscription.usage.enums.UsageRecordStatus;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.V1 + "/usage-records")
@RequiredArgsConstructor
public class UsageRecordController {

    private final UsageRecordService usageRecordService;

    @PostMapping
    public ResponseEntity<UsageRecordResponse> record(@Valid @RequestBody CreateUsageRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usageRecordService.record(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<UsageRecordResponse>> list(
            @RequestParam(required = false) SubscriberType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String entitlementCode,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) UsageRecordStatus status,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(usageRecordService.list(ownerType, ownerCode, entitlementCode, referenceType, referenceId, status, pageable));
    }
}

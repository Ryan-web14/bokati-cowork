package com.sni.bokaticowork.features.subscription.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillableItemResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionBillingMapper;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.BillableItemSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.specification.BillableItemSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/billable-items")
@RequiredArgsConstructor
public class BillingBridgeController {

    private final BillableItemRepository billableItemRepository;
    private final SubscriptionBillingMapper responseMapper;

    @GetMapping
    public ResponseEntity<PaginatedResponse<BillableItemResponse>> list(
            @RequestParam(required = false) BillableItemStatus status,
            @RequestParam(required = false) SubscriberType subscriberType,
            @RequestParam(required = false) String subscriberCode,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(billableItemRepository.findAll(
                BillableItemSpecification.search(BillableItemSearchCriteria.builder()
                        .status(status)
                        .subscriberType(subscriberType)
                        .subscriberCode(subscriberCode)
                        .sourceType(sourceType)
                        .sourceId(sourceId)
                        .build()),
                pageable
        ).map(responseMapper::toBillableItemResponse)));
    }
}

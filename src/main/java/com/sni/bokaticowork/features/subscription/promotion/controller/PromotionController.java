package com.sni.bokaticowork.features.subscription.promotion.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.dto.CouponRedemptionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.CreatePromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.dto.PromotionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.RedeemPromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;
import com.sni.bokaticowork.features.subscription.promotion.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.V1 + "/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody CreatePromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
    }

    @PatchMapping("/{code}/activate")
    public ResponseEntity<PromotionResponse> activate(@PathVariable String code) {
        return ResponseEntity.ok(promotionService.activate(code));
    }

    @PostMapping("/{code}/redeem")
    public ResponseEntity<CouponRedemptionResponse> redeem(@PathVariable String code,
                                                           @Valid @RequestBody RedeemPromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.redeem(code, request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<PromotionResponse>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) PromotionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(promotionService.list(code, name, status, pageable));
    }
}

package com.sni.bokaticowork.features.subscription.promotion.service;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.CouponRedemptionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.CreatePromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.dto.PromotionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.RedeemPromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;
import org.springframework.data.domain.Pageable;

public interface PromotionService {

    PromotionResponse create(CreatePromotionRequest request);

    PromotionResponse activate(String code);

    CouponRedemptionResponse redeem(String code, RedeemPromotionRequest request);

    PaginatedResponse<PromotionResponse> list(String code, String name, PromotionStatus status, Pageable pageable);

    int expireEndedPromotions();
}

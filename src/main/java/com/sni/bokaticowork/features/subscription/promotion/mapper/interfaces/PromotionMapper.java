package com.sni.bokaticowork.features.subscription.promotion.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.promotion.dto.CouponRedemptionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.CreatePromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.dto.PromotionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.RedeemPromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.mapper.decorator.PromotionMapperDecorator;
import com.sni.bokaticowork.features.subscription.promotion.model.CouponRedemption;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(PromotionMapperDecorator.class)
public interface PromotionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "redemptionCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "startsAt", ignore = true)
    @Mapping(target = "endsAt", ignore = true)
    Promotion toEntity(CreatePromotionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "redeemedAt", ignore = true)
    CouponRedemption toEntity(RedeemPromotionRequest request);

    PromotionResponse toResponse(Promotion promotion);

    CouponRedemptionResponse toResponse(CouponRedemption redemption);
}

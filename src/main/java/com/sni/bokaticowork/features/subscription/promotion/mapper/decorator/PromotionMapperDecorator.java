package com.sni.bokaticowork.features.subscription.promotion.mapper.decorator;

import com.sni.bokaticowork.features.subscription.promotion.dto.CouponRedemptionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.CreatePromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.dto.PromotionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.RedeemPromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;
import com.sni.bokaticowork.features.subscription.promotion.mapper.interfaces.PromotionMapper;
import com.sni.bokaticowork.features.subscription.promotion.model.CouponRedemption;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public abstract class PromotionMapperDecorator implements PromotionMapper {

    @Autowired
    @Qualifier("delegate")
    private PromotionMapper delegate;

    @Override
    public Promotion toEntity(CreatePromotionRequest request) {
        Promotion promotion = delegate.toEntity(request);
        promotion.setName(request.name().trim());
        promotion.setDescription(trim(request.description()));
        promotion.setRedemptionCount(0);
        promotion.setStatus(PromotionStatus.DRAFT);
        promotion.setMetadataJson(trim(request.metadataJson()));
        return promotion;
    }

    @Override
    public CouponRedemption toEntity(RedeemPromotionRequest request) {
        CouponRedemption redemption = delegate.toEntity(request);
        redemption.setSubscriberCode(request.subscriberCode().trim());
        return redemption;
    }

    @Override
    public PromotionResponse toResponse(Promotion promotion) {
        return new PromotionResponse(
                promotion.getCode(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getDiscountType(),
                promotion.getDiscountValue(),
                promotion.getStartsAt(),
                promotion.getEndsAt(),
                promotion.getMaxRedemptions(),
                promotion.getRedemptionCount(),
                promotion.getStatus(),
                promotion.getMetadataJson()
        );
    }

    @Override
    public CouponRedemptionResponse toResponse(CouponRedemption redemption) {
        return new CouponRedemptionResponse(
                redemption.getPromotion().getCode(),
                redemption.getSubscriberType(),
                redemption.getSubscriberCode(),
                redemption.getSubscription() == null ? null : redemption.getSubscription().getSubscriptionNumber(),
                redemption.getRedeemedAt()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

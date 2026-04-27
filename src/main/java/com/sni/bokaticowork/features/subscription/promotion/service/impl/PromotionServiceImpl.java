package com.sni.bokaticowork.features.subscription.promotion.service.impl;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.promotion.dto.CouponRedemptionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.CreatePromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.dto.PromotionResponse;
import com.sni.bokaticowork.features.subscription.promotion.dto.RedeemPromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;
import com.sni.bokaticowork.features.subscription.promotion.mapper.interfaces.PromotionMapper;
import com.sni.bokaticowork.features.subscription.promotion.model.CouponRedemption;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.repository.CouponRedemptionRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.specification.PromotionCriteria;
import com.sni.bokaticowork.features.subscription.promotion.repository.specification.PromotionSpecification;
import com.sni.bokaticowork.features.subscription.promotion.service.PromotionService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Service
@Transactional
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final SubscriptionService subscriptionService;
    private final PromotionMapper promotionMapper;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public PromotionResponse create(CreatePromotionRequest request) {
        if (request.discountType() == DiscountType.FREE_TRIAL_DAYS) {
            throw new BadRequestException("Trial promotions are not supported in this version");
        }
        String code = sequenceGenerator.next("promotion");
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new ResourceAlreadyExistException("Promotion code already exists");
        }
        Instant startsAt = parsePromotionStart(request.startsAt());
        Instant endsAt = parsePromotionEnd(request.endsAt());
        if (endsAt != null && endsAt.isBefore(startsAt)) {
            throw new BadRequestException("Promotion end date must be after or equal to the start date");
        }
        Promotion promotion = promotionMapper.toEntity(request);
        promotion.setCode(code);
        promotion.setStartsAt(startsAt);
        promotion.setEndsAt(endsAt);
        promotion = promotionRepository.save(promotion);
        return promotionMapper.toResponse(promotion);
    }

    @Override
    public PromotionResponse activate(String code) {
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        promotion.setStatus(PromotionStatus.ACTIVE);
        return promotionMapper.toResponse(promotionRepository.save(promotion));
    }

    @Override
    public CouponRedemptionResponse redeem(String code, RedeemPromotionRequest request) {
        Promotion promotion = promotionRepository.findUsablePromotion(code, Instant.now())
                .orElseThrow(() -> new ResourceNotFoundException("Usable promotion not found"));
        if (promotion.getMaxRedemptions() != null && promotion.getRedemptionCount() >= promotion.getMaxRedemptions()) {
            throw new ConflictException("promotion", "maximum redemptions reached");
        }
        if (redemptionRepository.existsRedemption(promotion.getCode(), request.subscriberType().name(), request.subscriberCode())) {
            throw new ConflictException("promotion", "subscriber already redeemed this promotion");
        }

        Subscription subscription = StringUtils.hasText(request.subscriptionNumber())
                ? subscriptionService.getForService(request.subscriptionNumber())
                : null;
        CouponRedemption redemption = promotionMapper.toEntity(request);
        redemption.setPromotion(promotion);
        redemption.setSubscription(subscription);
        redemption = redemptionRepository.save(redemption);
        promotion.setRedemptionCount(promotion.getRedemptionCount() + 1);
        promotionRepository.save(promotion);
        return promotionMapper.toResponse(redemption);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PromotionResponse> list(String code, String name, PromotionStatus status, Pageable pageable) {
        return new PaginatedResponse<>(promotionRepository.findAll(
                PromotionSpecification.search(PromotionCriteria.builder().code(code).name(name).status(status).build()),
                pageable
        ).map(promotionMapper::toResponse));
    }

    @Override
    public int expireEndedPromotions() {
        return promotionRepository.expireEndedPromotions(Instant.now());
    }

    private Instant parsePromotionStart(String value) {
        return parsePromotionInstant(value, false);
    }

    private Instant parsePromotionEnd(String value) {
        return parsePromotionInstant(value, true);
    }

    private Instant parsePromotionInstant(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        try {
            if (trimmed.length() == 10) {
                LocalDate date = LocalDate.parse(trimmed);
                return endOfDay
                        ? date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC)
                        : date.atStartOfDay().toInstant(ZoneOffset.UTC);
            }
            if (trimmed.endsWith("Z") || trimmed.contains("+")) {
                return Instant.parse(trimmed);
            }
            return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid promotion date format. Use YYYY-MM-DD or ISO-8601 date-time", ex);
        }
    }

}

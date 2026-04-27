package com.sni.bokaticowork.features.subscription.promotion.service.impl;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.promotion.dto.CreatePromotionRequest;
import com.sni.bokaticowork.features.subscription.promotion.dto.PromotionResponse;
import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.mapper.interfaces.PromotionMapper;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.repository.CouponRedemptionRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private CouponRedemptionRepository redemptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private PromotionMapper promotionMapper;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    @Test
    void shouldAcceptDateOnlyPromotionDates() {
        CreatePromotionRequest request = new CreatePromotionRequest(
                "Launch Offer",
                "Promo",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                "2026-04-27",
                "2026-04-30",
                100,
                null
        );

        Promotion promotion = Promotion.builder().build();
        Promotion saved = Promotion.builder()
                .code("PROMO-0001")
                .startsAt(Instant.parse("2026-04-27T00:00:00Z"))
                .endsAt(Instant.parse("2026-04-30T23:59:59.999999999Z"))
                .build();
        PromotionResponse response = new PromotionResponse(
                "PROMO-0001",
                "Launch Offer",
                "Promo",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                saved.getStartsAt(),
                saved.getEndsAt(),
                100,
                0,
                null,
                null
        );

        when(sequenceGenerator.next("promotion")).thenReturn("PROMO-0001");
        when(promotionRepository.existsByCodeIgnoreCase("PROMO-0001")).thenReturn(false);
        when(promotionMapper.toEntity(request)).thenReturn(promotion);
        when(promotionRepository.save(any(Promotion.class))).thenReturn(saved);
        when(promotionMapper.toResponse(saved)).thenReturn(response);

        PromotionResponse created = promotionService.create(request);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        assertEquals(Instant.parse("2026-04-27T00:00:00Z"), captor.getValue().getStartsAt());
        assertEquals(Instant.parse("2026-04-30T23:59:59.999999999Z"), captor.getValue().getEndsAt());
        assertEquals("PROMO-0001", created.code());
    }

    @Test
    void shouldRejectInvalidPromotionDateFormat() {
        CreatePromotionRequest request = new CreatePromotionRequest(
                "Launch Offer",
                null,
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                "27/04/2026",
                null,
                null,
                null
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> promotionService.create(request));

        assertEquals("Invalid promotion date format. Use YYYY-MM-DD or ISO-8601 date-time", ex.getMessage());
    }
}

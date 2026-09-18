package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.enums.PromotionAudienceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model.PromotionAudience;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository.PromotionAudienceRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository.PromotionBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Qui a le droit de voir quelle promotion.
 *
 * <p>La règle protégée ici est celle qui rend le ciblage utilisable : une promotion nominative
 * n'apparaît qu'à ses bénéficiaires. Si elle fuite dans un catalogue ou dans une réponse d'API,
 * le geste commercial accordé à un client devient une réclamation de tous les autres.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromotionAudienceFilterTest {

    @Mock private PromotionAudienceRepository audienceRepository;
    @Mock private PromotionBeneficiaryRepository beneficiaryRepository;

    @InjectMocks
    private PromotionAudienceFilter filter;

    private final List<PromotionAudience> audiences = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(audienceRepository.findAllByPromotionIds(any())).thenReturn(audiences);
        when(beneficiaryRepository.findGrantedPromotionIds(any(), anyString(), anyString())).thenReturn(List.of());
    }

    /**
     * Toutes les campagnes qui existaient avant ce ciblage n'ont pas d'audience déclarée. Elles ne
     * doivent pas disparaître du jour au lendemain.
     */
    @Test
    void treatsAPromotionWithoutDeclaredAudienceAsPublic() {
        assertEquals(Set.of(1L, 2L), filter.visibleTo(List.of(1L, 2L), context()));
    }

    @Test
    void hidesANamedListPromotionFromEveryoneElse() {
        audience(1L, PromotionAudienceType.SUBSCRIBER_LIST);

        assertTrue(filter.visibleTo(List.of(1L), context()).isEmpty());
    }

    @Test
    void showsANamedListPromotionToItsBeneficiary() {
        audience(1L, PromotionAudienceType.SUBSCRIBER_LIST);
        when(beneficiaryRepository.findGrantedPromotionIds(any(), anyString(), anyString())).thenReturn(List.of(1L));

        assertEquals(Set.of(1L), filter.visibleTo(List.of(1L), context()));
    }

    @Test
    void matchesASegmentAudience() {
        PromotionAudience segment = audience(1L, PromotionAudienceType.SEGMENT);
        segment.setSegmentCode("ETUDIANT");

        assertEquals(Set.of(1L), filter.visibleTo(List.of(1L), context()));

        segment.setSegmentCode("SENIOR");
        assertTrue(filter.visibleTo(List.of(1L), context()).isEmpty());
    }

    @Test
    void matchesAPlanAudienceAgainstTheCartLines() {
        PromotionAudience plan = audience(1L, PromotionAudienceType.PLAN);
        plan.setPlanCode("PLN-1");

        assertEquals(Set.of(1L), filter.visibleTo(List.of(1L), context()));

        plan.setPlanCode("PLN-AUTRE");
        assertTrue(filter.visibleTo(List.of(1L), context()).isEmpty());
    }

    /** Plusieurs audiences se lisent comme un « ou » : étudiants <i>et</i> invités nommés. */
    @Test
    void acceptsAnyOfSeveralAudiences() {
        PromotionAudience segment = audience(1L, PromotionAudienceType.SEGMENT);
        segment.setSegmentCode("SENIOR");
        audience(1L, PromotionAudienceType.SUBSCRIBER_LIST);
        when(beneficiaryRepository.findGrantedPromotionIds(any(), anyString(), anyString())).thenReturn(List.of(1L));

        assertEquals(Set.of(1L), filter.visibleTo(List.of(1L), context()));
    }

    /**
     * Le coût compte : une seule requête de bénéficiaires pour toute la liste, et aucune du tout
     * lorsqu'aucune campagne n'est nominative. Interroger campagne par campagne produirait autant
     * d'aller-retours que de campagnes actives, à chaque affichage de panier.
     */
    @Test
    void doesNotQueryBeneficiariesWhenNoPromotionIsNominative() {
        audience(1L, PromotionAudienceType.ALL);
        PromotionAudience segment = audience(2L, PromotionAudienceType.SEGMENT);
        segment.setSegmentCode("ETUDIANT");

        filter.visibleTo(List.of(1L, 2L), context());

        verify(beneficiaryRepository, never()).findGrantedPromotionIds(any(), anyString(), anyString());
    }

    @Test
    void handlesAnAnonymousVisitorWithoutFailing() {
        audience(1L, PromotionAudienceType.SUBSCRIBER_LIST);
        PricingContext anonymous = new PricingContext(null, null, null, null, null, false,
                List.of(line()), null, null, null, null, "XAF", List.of(), Instant.now(), true);

        assertFalse(filter.visibleTo(List.of(1L), anonymous).contains(1L));
    }

    // -------------------------------------------------------------------------------------

    private PromotionAudience audience(Long promotionId, PromotionAudienceType type) {
        Promotion promotion = new Promotion();
        promotion.setId(promotionId);
        PromotionAudience audience = PromotionAudience.builder()
                .promotion(promotion)
                .audienceType(type)
                .build();
        audiences.add(audience);
        return audience;
    }

    private PricingContext.PricingLine line() {
        return new PricingContext.PricingLine("L1", TargetScope.PLAN, "PLN-1", null, "Plan",
                1, new BigDecimal("10000"), null);
    }

    private PricingContext context() {
        return new PricingContext("MEMBER", "MEM-1", "ETUDIANT", null, null, false,
                List.of(line()), "MONTHLY", null, null, null, "XAF", List.of(), Instant.now(), true);
    }
}

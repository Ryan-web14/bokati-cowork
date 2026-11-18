package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.dto.request.AudienceRef;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAudience;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.model.ResourceVisibility;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAudienceRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.pricing.PriceContext;
import com.sni.bokaticowork.features.ressource.service.pricing.ResourcePriceEngine;
import com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService.Access;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot B10 : visibilite des ressources par client et prix au cas par cas. */
class ResourceB10Test {

    private final LocalDateTime at = LocalDateTime.of(2026, 12, 2, 10, 0);

    private static PriceContext member(String code, String... companies) {
        Set<String> owners = new java.util.LinkedHashSet<>();
        owners.add("MEMBER:" + code);
        for (String company : companies) owners.add("CUSTOMER:" + company);
        return new PriceContext(Set.of("MEMBER"), null, "PORTAL", LocalDateTime.now(), owners);
    }

    @Test
    void aClientPriceAppliesOnlyToItsClientsAndWins() throws Exception {
        ResourcePriceEngine engine = new ResourcePriceEngine(null);
        Method applies = ResourcePriceEngine.class.getDeclaredMethod("applies", ResourcePricingRule.class, LocalDateTime.class,
                LocalDateTime.class, long.class, int.class, PriceContext.class);
        applies.setAccessible(true);
        ResourcePricingRule forAcme = ResourcePricingRule.builder().price(3000).audience("CUSTOMER:ACME,MEMBER:MBR-9").build();
        ResourcePricingRule general = ResourcePricingRule.builder().price(5000).build();

        // Un membre rattache a l'organisation ACME, le membre MBR-9, et un autre membre.
        assertThat((boolean) applies.invoke(engine, forAcme, at, at, 60L, 1, member("MBR-1", "ACME"))).isTrue();
        assertThat((boolean) applies.invoke(engine, forAcme, at, at, 60L, 1, member("MBR-9"))).isTrue();
        assertThat((boolean) applies.invoke(engine, forAcme, at, at, 60L, 1, member("MBR-2"))).isFalse();
        assertThat((boolean) applies.invoke(engine, forAcme, at, at, 60L, 1, PriceContext.anonymous())).isFalse();
        // Plus precise que la regle generale : elle l'emporte a priorite egale.
        Method specificity = ResourcePriceEngine.class.getDeclaredMethod("specificity", ResourcePricingRule.class);
        specificity.setAccessible(true);
        assertThat((int) specificity.invoke(null, forAcme)).isGreaterThan((int) specificity.invoke(null, general));
    }

    @Test
    void aSegmentOrAPlanCanBeTargeted() throws Exception {
        ResourcePriceEngine engine = new ResourcePriceEngine(null);
        Method applies = ResourcePriceEngine.class.getDeclaredMethod("applies", ResourcePricingRule.class, LocalDateTime.class,
                LocalDateTime.class, long.class, int.class, PriceContext.class);
        applies.setAccessible(true);
        ResourcePricingRule members = ResourcePricingRule.builder().price(4000).audience("SEGMENT:MEMBER").build();
        ResourcePricingRule premium = ResourcePricingRule.builder().price(2000).audience("PLAN:PREMIUM").build();
        PriceContext subscriber = new PriceContext(Set.of("MEMBER", "SUBSCRIBER"), "PREMIUM", "PORTAL", LocalDateTime.now(), Set.of("MEMBER:MBR-1"));

        assertThat((boolean) applies.invoke(engine, members, at, at, 60L, 1, member("MBR-2"))).isTrue();
        assertThat((boolean) applies.invoke(engine, members, at, at, 60L, 1, PriceContext.anonymous())).isFalse();
        assertThat((boolean) applies.invoke(engine, premium, at, at, 60L, 1, subscriber)).isTrue();
        assertThat((boolean) applies.invoke(engine, premium, at, at, 60L, 1, member("MBR-2"))).isFalse();
    }

    @Test
    void audienceRefsAreValidated() {
        assertThat(AudienceRef.join(List.of(new AudienceRef("member", "MBR-1"), new AudienceRef("segment", "subscriber"))))
                .isEqualTo("MEMBER:MBR-1,SEGMENT:SUBSCRIBER");
        assertThatThrownBy(() -> new AudienceRef("GROUP", "X").key()).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new AudienceRef("SEGMENT", "VIP").key()).isInstanceOf(BadRequestException.class);
    }

    @Test
    void accessFollowsTheVisibilityMode() {
        ResourceAudienceRepository audiences = mock(ResourceAudienceRepository.class);
        ResourceVisibilityService service = new ResourceVisibilityService(mock(ResourceRepository.class), audiences);
        Resource room = Resource.builder().id(1L).code("SAL-1").visibilityMode(ResourceVisibility.RESTRICTED).build();
        when(audiences.findAllByResourceOrderByIdAsc(any())).thenReturn(List.of(
                ResourceAudience.builder().resource(room).audienceType("CUSTOMER").audienceCode("ACME").build()));

        assertThat(service.access(room, member("MBR-1", "ACME"))).isEqualTo(Access.DIRECT);
        assertThat(service.access(room, member("MBR-2"))).isEqualTo(Access.HIDDEN);

        room.setVisibilityMode(ResourceVisibility.ON_REQUEST);
        assertThat(service.access(room, member("MBR-2"))).isEqualTo(Access.ON_REQUEST);
        assertThat(service.access(room, member("MBR-1", "ACME"))).isEqualTo(Access.DIRECT);

        room.setVisibilityMode(ResourceVisibility.PUBLIC);
        assertThat(service.access(room, PriceContext.anonymous())).isEqualTo(Access.DIRECT);
    }

    @Test
    void aRestrictedResourceNeedsAnAudience() {
        ResourceRepository resources = mock(ResourceRepository.class);
        when(resources.findByCode("SAL-1")).thenReturn(Optional.of(Resource.builder().id(1L).code("SAL-1").build()));
        ResourceVisibilityService service = new ResourceVisibilityService(resources, mock(ResourceAudienceRepository.class));

        assertThatThrownBy(() -> service.set("SAL-1", new ResourceVisibilityService.VisibilityRequest("RESTRICTED", List.of())))
                .isInstanceOf(BadRequestException.class);
    }
}

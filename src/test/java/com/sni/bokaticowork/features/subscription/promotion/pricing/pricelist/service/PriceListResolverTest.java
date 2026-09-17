package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.enums.PriceListAudienceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.enums.PriceMode;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model.PriceList;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model.PriceListEntry;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.repository.PriceListEntryRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.repository.PriceListRepository;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * La grille tarifaire.
 *
 * <p>Un tarif négocié n'est pas une remise : il remplace le prix catalogue au lieu de le réduire.
 * Ces tests fixent les deux règles dont dépend la reconstitution d'une facture, à savoir qu'une
 * seule grille s'applique à une ligne, et laquelle.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceListResolverTest {

    @Mock private PriceListRepository priceListRepository;
    @Mock private PriceListEntryRepository entryRepository;

    @InjectMocks
    private PriceListResolver resolver;

    private final AtomicLong ids = new AtomicLong(1);
    private final List<PriceList> lists = new ArrayList<>();
    private final List<PriceListEntry> entries = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(priceListRepository.findCandidates(any(), any(), any(), any(), any(), any())).thenReturn(lists);
        when(entryRepository.findAllByPriceListIds(any())).thenReturn(entries);
    }

    @Test
    void leavesThePriceAloneWhenNoGridApplies() {
        PriceListResolver.Resolution resolution = resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertTrue(resolution.unchanged());
        assertEquals(new BigDecimal("10000"), resolution.lines().getFirst().unitPrice());
    }

    @Test
    void replacesThePriceWithANegotiatedOne() {
        PriceList partner = list("GRT-PARTENAIRE", PriceListAudienceType.PARTNER, "PART-1", 100);
        entry(partner, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "7000", 1);

        PriceListResolver.Resolution resolution = resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(new BigDecimal("7000.0000"), resolution.lines().getFirst().unitPrice());
        assertEquals(1, resolution.overrides().size());
        // Le prix catalogue reste lisible : sans lui, personne ne peut dire ce que la grille a
        // reellement concede.
        assertEquals(new BigDecimal("10000"), resolution.overrides().getFirst().catalogUnitPrice());
        assertEquals("GRT-PARTENAIRE", resolution.overrides().getFirst().priceListCode());
    }

    @Test
    void derivesFromTheCatalogueWhenTheGridSaysAPercentage() {
        PriceList student = list("GRT-ETUDIANT", PriceListAudienceType.SEGMENT, "ETUDIANT", 100);
        entry(student, TargetScope.PLAN, "PLN-1", PriceMode.PERCENTAGE_OFF_LIST, "20", 1);

        PriceListResolver.Resolution resolution = resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(new BigDecimal("8000.0000"), resolution.lines().getFirst().unitPrice());
    }

    /**
     * Le dégressif sans table supplémentaire : trois paliers sur le même objet, et c'est le plus
     * élevé que la quantité atteint qui gagne.
     */
    @Test
    void picksTheHighestQuantityTierTheOrderReaches() {
        PriceList volume = list("GRT-VOLUME", PriceListAudienceType.ALL, null, 100);
        entry(volume, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "10000", 1);
        entry(volume, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "9000", 5);
        entry(volume, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "8000", 10);

        assertEquals(new BigDecimal("10000.0000"),
                resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 3, "12000"))).lines().getFirst().unitPrice());
        assertEquals(new BigDecimal("9000.0000"),
                resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 7, "12000"))).lines().getFirst().unitPrice());
        assertEquals(new BigDecimal("8000.0000"),
                resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 12, "12000"))).lines().getFirst().unitPrice());
    }

    /**
     * La règle qui évite qu'un tarif négocié soit écrasé par un tarif public : à priorité égale, la
     * grille la plus spécifique l'emporte.
     */
    @Test
    void prefersTheMoreSpecificGridWhenPrioritiesAreEqual() {
        PriceList everyone = list("GRT-PUBLIC", PriceListAudienceType.ALL, null, 100);
        entry(everyone, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "9500", 1);
        PriceList named = list("GRT-NOMME", PriceListAudienceType.SUBSCRIBER, "MEM-1", 100);
        entry(named, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "7000", 1);

        PriceListResolver.Resolution resolution = resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(new BigDecimal("7000.0000"), resolution.lines().getFirst().unitPrice());
        assertEquals("GRT-NOMME", resolution.overrides().getFirst().priceListCode());
    }

    @Test
    void letsPriorityWinOverSpecificity() {
        PriceList named = list("GRT-NOMME", PriceListAudienceType.SUBSCRIBER, "MEM-1", 100);
        entry(named, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "7000", 1);
        PriceList campaign = list("GRT-CAMPAGNE", PriceListAudienceType.ALL, null, 10);
        entry(campaign, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_PRICE, "6000", 1);

        PriceListResolver.Resolution resolution = resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals("GRT-CAMPAGNE", resolution.overrides().getFirst().priceListCode());
    }

    @Test
    void appliesAGridLineWithoutCodeToEveryObjectOfItsScope() {
        PriceList allRooms = list("GRT-SALLES", PriceListAudienceType.ALL, null, 100);
        entry(allRooms, TargetScope.RESOURCE, null, PriceMode.PERCENTAGE_OFF_LIST, "10", 1);

        PriceListResolver.Resolution resolution = resolver.resolve(context(
                line("L1", TargetScope.RESOURCE, "SALLE-A", 1, "10000"),
                line("L2", TargetScope.PLAN, "PLN-1", 1, "20000")));

        assertEquals(new BigDecimal("9000.0000"), resolution.lines().getFirst().unitPrice());
        // Le plan n'est pas une ressource : la grille ne le touche pas.
        assertEquals(new BigDecimal("20000"), resolution.lines().get(1).unitPrice());
        assertEquals(1, resolution.overrides().size());
    }

    @Test
    void neverProducesANegativePriceEvenFromAClumsyGrid() {
        PriceList wrong = list("GRT-ERREUR", PriceListAudienceType.ALL, null, 100);
        entry(wrong, TargetScope.PLAN, "PLN-1", PriceMode.FIXED_AMOUNT_OFF_LIST, "99999", 1);

        PriceListResolver.Resolution resolution = resolver.resolve(context(line("L1", TargetScope.PLAN, "PLN-1", 1, "10000")));

        assertEquals(0, resolution.lines().getFirst().unitPrice().signum());
    }

    // -------------------------------------------------------------------------------------

    private PriceList list(String code, PriceListAudienceType audienceType, String audienceCode, int priority) {
        PriceList list = PriceList.builder()
                .code(code)
                .name("Grille " + code)
                .currency("XAF")
                .audienceType(audienceType)
                .audienceCode(audienceCode)
                .priority(priority)
                .active(Boolean.TRUE)
                .build();
        list.setId(ids.getAndIncrement());
        lists.add(list);
        return list;
    }

    private void entry(PriceList list, TargetScope scope, String targetCode,
                       PriceMode mode, String value, int minQuantity) {
        PriceListEntry entry = PriceListEntry.builder()
                .priceList(list)
                .targetScope(scope)
                .targetCode(targetCode)
                .priceMode(mode)
                .value(new BigDecimal(value))
                .minQuantity(minQuantity)
                .build();
        entry.setId(ids.getAndIncrement());
        entries.add(entry);
    }

    private PricingContext.PricingLine line(String reference, TargetScope scope, String code,
                                            int quantity, String unitPrice) {
        return new PricingContext.PricingLine(reference, scope, code, null, code,
                quantity, new BigDecimal(unitPrice), null);
    }

    private PricingContext context(PricingContext.PricingLine... lines) {
        return new PricingContext("MEMBER", "MEM-1", "ETUDIANT", null, null, false,
                List.of(lines), "MONTHLY", null, null, null, "XAF", List.of(), Instant.now(), true);
    }
}

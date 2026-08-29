package com.sni.bokaticowork.features.billing.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCatalogItemFloorPriceTest {

    @Test
    void shouldPreferAnExplicitFloorOverAnythingDerived() {
        ServiceCatalogItem item = item("8000", "12000", "20");

        assertThat(item.effectiveFloorPrice()).isEqualByComparingTo("12000");
    }

    @Test
    void shouldDeriveTheFloorFromCostAndMinimumMargin() {
        // Taux de marque : la marge est rapportee au prix de vente, donc cout / (1 - taux).
        // 8000 / 0,80 = 10000, et a ce prix la marge vaut bien 2000, soit 20 % de 10000.
        ServiceCatalogItem item = item("8000", null, "20");

        assertThat(item.effectiveFloorPrice()).isEqualByComparingTo("10000");
    }

    @Test
    void shouldFallBackToCostWhenNoMarginIsSet() {
        // Sans marge minimale, le plancher est le cout : on ne vend pas a perte.
        ServiceCatalogItem item = item("8000", null, null);

        assertThat(item.effectiveFloorPrice()).isEqualByComparingTo("8000");
    }

    @Test
    void shouldHaveNoFloorWhenNeitherCostNorFloorIsKnown() {
        ServiceCatalogItem item = item(null, null, "20");

        assertThat(item.effectiveFloorPrice()).isNull();
    }

    @Test
    void shouldTreatAZeroMarginAsNoMargin() {
        ServiceCatalogItem item = item("8000", null, "0");

        assertThat(item.effectiveFloorPrice()).isEqualByComparingTo("8000");
    }

    @Test
    void shouldNotDivideByZeroOnAMarginOfOneHundred() {
        // La contrainte de base interdit 100, mais une ligne anterieure a la migration
        // pourrait y echapper · le calcul ne doit pas exploser pour autant.
        ServiceCatalogItem item = item("8000", null, "100");

        assertThat(item.effectiveFloorPrice()).isEqualByComparingTo("8000");
    }

    private ServiceCatalogItem item(String costPrice, String floorPrice, String minMarginRate) {
        return ServiceCatalogItem.builder()
                .itemCode("SRV-001")
                .name("Salle de reunion")
                .unitPrice(new BigDecimal("15000"))
                .costPrice(costPrice == null ? null : new BigDecimal(costPrice))
                .floorPrice(floorPrice == null ? null : new BigDecimal(floorPrice))
                .minMarginRate(minMarginRate == null ? null : new BigDecimal(minMarginRate))
                .build();
    }
}

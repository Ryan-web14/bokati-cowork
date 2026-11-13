package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.billing.model.CatalogCategory;
import com.sni.bokaticowork.features.billing.model.CatalogUnit;
import com.sni.bokaticowork.features.billing.model.TaxRule;
import com.sni.bokaticowork.features.billing.repository.CatalogCategoryRepository;
import com.sni.bokaticowork.features.billing.repository.CatalogUnitRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.repository.TaxRuleRepository;
import com.sni.bokaticowork.features.billing.service.implementation.CatalogReferenceService.ItemReferences;
import com.sni.bokaticowork.features.billing.service.implementation.CatalogReferenceService.Resolved;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C0 : controle des references d'un article. Mode souple : la valeur inconnue rejoint la
 * liste ; mode strict : elle est refusee ; une valeur inchangee passe toujours ; un code de taxe
 * inconnu est garde et signale en mode souple.
 */
class CatalogReferenceC0Test {

    private final CatalogCategoryRepository categories = mock(CatalogCategoryRepository.class);
    private final CatalogUnitRepository units = mock(CatalogUnitRepository.class);
    private final TaxRuleRepository taxRules = mock(TaxRuleRepository.class);
    private final ServiceCatalogItemRepository items = mock(ServiceCatalogItemRepository.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);
    private final CatalogReferenceService service = new CatalogReferenceService(categories, units, taxRules, items);

    @BeforeEach
    void setUp() {
        service.setPlatformSettings(settings);
        when(categories.findFamily(anyString())).thenReturn(Optional.empty());
        when(categories.findFamiliesByLabel(anyString())).thenReturn(List.of());
        when(categories.findSubcategory(anyString(), anyString())).thenReturn(Optional.empty());
        when(categories.findSubcategoriesByLabel(anyString(), anyString())).thenReturn(List.of());
        when(units.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(units.findByLabelIgnoreCase(anyString())).thenReturn(List.of());
        when(taxRules.findVersions(anyString())).thenReturn(List.of());
        when(taxRules.findByCode(anyString())).thenReturn(Optional.empty());
        when(categories.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(units.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldAddAnUnknownValueToTheListInSoftMode() {
        when(settings.bool(eq(CatalogReferenceService.STRICT_KEY), eq(false))).thenReturn(false);

        Resolved resolved = service.resolveForItem(new ItemReferences("Salles", "Réunion", "heure", null), null);

        assertThat(resolved.category()).isEqualTo("Salles");
        assertThat(resolved.subcategory()).isEqualTo("Réunion");
        assertThat(resolved.unit()).isEqualTo("heure");
        verify(categories).save(org.mockito.ArgumentMatchers.argThat(c -> "Salles".equals(c.getCode()) && c.getCreatedFromItem()));
        verify(categories).save(org.mockito.ArgumentMatchers.argThat(c -> "Réunion".equals(c.getCode()) && "Salles".equals(c.getParentCode())));
        verify(units).save(org.mockito.ArgumentMatchers.argThat(u -> "heure".equals(u.getCode()) && u.getCreatedFromItem()));
    }

    @Test
    void shouldRefuseAnUnknownValueInStrictModeButKeepAnUnchangedOne() {
        when(settings.bool(eq(CatalogReferenceService.STRICT_KEY), eq(false))).thenReturn(true);

        assertThatThrownBy(() -> service.resolveForItem(new ItemReferences("Salles", null, null, null), null))
                .isInstanceOf(BadRequestException.class)
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("REFERENCE_INCONNUE"));

        // Un ancien ecran qui renvoie l'article tel quel n'est jamais bloque.
        Resolved unchanged = service.resolveForItem(new ItemReferences("salles", null, null, null),
                new ItemReferences("Salles", null, null, null));
        assertThat(unchanged.category()).isEqualTo("Salles");
        verify(categories, never()).save(any());
    }

    @Test
    void shouldMatchAnEntryByItsLabelAndStoreItsCode() {
        when(settings.bool(eq(CatalogReferenceService.STRICT_KEY), eq(false))).thenReturn(true);
        CatalogCategory meeting = CatalogCategory.builder().code("SALLES_DE_REUNION").label("Salles de réunion").active(true).build();
        when(categories.findFamiliesByLabel("Salles de réunion")).thenReturn(List.of(meeting));
        CatalogUnit day = CatalogUnit.builder().code("jour").label("Jour").pluralLabel("jours").active(true).build();
        when(units.findByLabelIgnoreCase("jours")).thenReturn(List.of(day));

        Resolved resolved = service.resolveForItem(new ItemReferences("Salles de réunion", null, "jours", null), null);

        assertThat(resolved.category()).isEqualTo("SALLES_DE_REUNION");
        assertThat(resolved.unit()).isEqualTo("jour");
    }

    @Test
    void shouldRefuseADeactivatedEntryOnlyInStrictMode() {
        CatalogUnit page = CatalogUnit.builder().code("page").label("Page").active(false).build();
        when(units.findByCodeIgnoreCase("page")).thenReturn(Optional.of(page));

        when(settings.bool(eq(CatalogReferenceService.STRICT_KEY), eq(false))).thenReturn(false);
        assertThat(service.resolveForItem(new ItemReferences(null, null, "page", null), null).unit()).isEqualTo("page");

        when(settings.bool(eq(CatalogReferenceService.STRICT_KEY), eq(false))).thenReturn(true);
        assertThatThrownBy(() -> service.resolveForItem(new ItemReferences(null, null, "page", null), null))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("REFERENCE_INACTIVE"));
        assertThat(service.resolveForItem(new ItemReferences(null, null, "page", null),
                new ItemReferences(null, null, "page", null)).unit()).isEqualTo("page");
    }

    @Test
    void shouldKeepAnUnknownTaxCodeWithAWarningInSoftModeAndRefuseItInStrictMode() {
        when(settings.bool(eq(CatalogReferenceService.STRICT_KEY), eq(false))).thenReturn(false);
        Resolved soft = service.resolveForItem(new ItemReferences(null, null, null, "TVA18"), null);
        assertThat(soft.taxRuleCode()).isEqualTo("TVA18");
        assertThat(soft.warnings()).singleElement().asString().contains("TVA18");

        when(settings.bool(eq(CatalogReferenceService.STRICT_KEY), eq(false))).thenReturn(true);
        assertThatThrownBy(() -> service.resolveForItem(new ItemReferences(null, null, null, "TVA18"), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldStoreTheBaseCodeOfAKnownVatRuleAndRefuseAnAdditionalCentRule() {
        TaxRule vat = TaxRule.builder().code("TVA_CG_18_20270101").baseCode("TVA_CG_18").taxType("VAT").rate(new BigDecimal("19"))
                .active(true).validFrom(LocalDate.now().minusDays(1)).build();
        when(taxRules.findVersions("TVA_CG_18_20270101")).thenReturn(List.of());
        when(taxRules.findByCode("TVA_CG_18_20270101")).thenReturn(Optional.of(vat));
        when(taxRules.findVersions("TVA_CG_18")).thenReturn(List.of(vat));
        assertThat(service.resolveForItem(new ItemReferences(null, null, null, "TVA_CG_18_20270101"), null).taxRuleCode())
                .isEqualTo("TVA_CG_18");

        TaxRule cents = TaxRule.builder().code("CAC_CG_ON_VAT_5").baseCode("CAC_CG_ON_VAT_5").name("Centimes").taxType("ADDITIONAL_CENT")
                .rate(new BigDecimal("5")).active(true).build();
        when(taxRules.findVersions("CAC_CG_ON_VAT_5")).thenReturn(List.of(cents));
        assertThatThrownBy(() -> service.resolveForItem(new ItemReferences(null, null, null, "CAC_CG_ON_VAT_5"), null))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("TAXE_INVALIDE"));
    }

    @Test
    void shouldBuildACodeFromTheLabelAndAvoidDuplicates() {
        assertThat(CatalogReferenceService.slug("Salles de réunion")).isEqualTo("SALLES_DE_REUNION");
        assertThat(CatalogReferenceService.newCode(null, "Café & boissons", 100, code -> code.equals("CAFE_BOISSONS")))
                .isEqualTo("CAFE_BOISSONS_2");
        assertThatThrownBy(() -> CatalogReferenceService.newCode("SALLES", "Salles", 100, code -> true))
                .satisfies(error -> assertThat(((com.sni.bokaticowork.core.exception.customs.ConflictException) error).getReason())
                        .isEqualTo("REFERENCE_EXISTANTE"));
    }
}

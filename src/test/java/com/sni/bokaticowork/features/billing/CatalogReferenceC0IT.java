package com.sni.bokaticowork.features.billing;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.MergeRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.ReferencesResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleSuccessorRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.UnitRequest;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.repository.TaxRuleRepository;
import com.sni.bokaticowork.features.billing.service.implementation.CatalogReferenceService;
import com.sni.bokaticowork.features.billing.service.implementation.CatalogReferenceService.ItemReferences;
import com.sni.bokaticowork.features.billing.service.implementation.CatalogReferenceService.Resolved;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lot C0 sur le schema de production : reprise des listes (unites de base, taxes exoneree et hors
 * champ, famille « Autres »), ajout en mode souple, libelles, changement de taux date, fusion et
 * suppression refusee d'une entree utilisee. Le schema est valide contre les entites.
 */
@Import(CatalogReferenceService.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CatalogReferenceC0IT extends PostgresIntegrationTestBase {

    @Autowired private CatalogReferenceService references;
    @Autowired private ServiceCatalogItemRepository items;
    @Autowired private TaxRuleRepository taxRules;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldSeedTheListsAndExposeThemInOneCall() {
        ReferencesResponse lists = references.references(false);

        assertThat(lists.units()).extracting(unit -> unit.code()).contains("heure", "jour", "mois", "forfait");
        assertThat(lists.categories()).extracting(CategoryResponse::code).contains("AUTRES");
        assertThat(lists.taxRules()).extracting(TaxRuleResponse::baseCode).contains("TVA_CG_18", "TVA_CG_EXONERE", "TVA_CG_HORS_CHAMP");
        // Les centimes additionnels se calculent sur la TVA : ils ne se choisissent pas sur un article.
        assertThat(lists.taxRules()).extracting(TaxRuleResponse::baseCode).doesNotContain("CAC_CG_ON_VAT_5");
        assertThat(lists.taxRules()).filteredOn(rule -> rule.baseCode().equals("TVA_CG_EXONERE"))
                .singleElement().satisfies(rule -> {
                    assertThat(rule.treatment()).isEqualTo("EXEMPT");
                    assertThat(rule.rate()).isEqualByComparingTo("0");
                    assertThat(rule.exemptionReason()).isNotBlank();
                });
        assertThat(lists.strict()).isFalse();
    }

    @Test
    void shouldAddUnknownValuesFromAnItemAndCountTheirUse() {
        String family = "Fam " + UUID.randomUUID().toString().substring(0, 6);
        Resolved resolved = references.resolveForItem(new ItemReferences(family, "Petite salle", "créneau", "TVA_CG_EXONERE"), null);
        items.save(ServiceCatalogItem.builder().itemCode("SRV-" + UUID.randomUUID().toString().substring(0, 8)).name("Salle Okoumé")
                .unitPrice(new BigDecimal("25000")).category(resolved.category()).subcategory(resolved.subcategory())
                .unit(resolved.unit()).taxRuleCode(resolved.taxRuleCode()).build());
        entityManager.flush();

        assertThat(resolved.taxRuleCode()).isEqualTo("TVA_CG_EXONERE");
        CategoryResponse created = references.listCategories(false).stream().filter(c -> c.code().equals(family)).findFirst().orElseThrow();
        assertThat(created.createdFromItem()).isTrue();
        assertThat(created.itemCount()).isEqualTo(1);
        assertThat(created.subcategories()).singleElement().satisfies(sub -> {
            assertThat(sub.code()).isEqualTo("Petite salle");
            assertThat(sub.itemCount()).isEqualTo(1);
        });
        CatalogReferenceService.Snapshot labels = references.snapshot();
        assertThat(labels.unitLabel("créneau")).isEqualTo("créneau");
        assertThat(labels.tax("TVA_CG_EXONERE").treatment()).isEqualTo("EXEMPT");

        // Une entree utilisee ne se supprime pas.
        assertThatThrownBy(() -> references.deleteUnit("créneau"))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(((ConflictException) error).getReason()).isEqualTo("REFERENCE_UTILISEE"));
    }

    @Test
    void shouldReplaceATaxRateFromADateWithoutTouchingTheItemCode() {
        LocalDate start = LocalDate.now().plusMonths(2).withDayOfMonth(1);
        TaxRuleResponse next = references.successor("TVA_CG_18", new TaxRuleSuccessorRequest(new BigDecimal("19"), start, "TVA Congo 19%", "TVA 19 %"));

        assertThat(next.baseCode()).isEqualTo("TVA_CG_18");
        assertThat(next.code()).isEqualTo("TVA_CG_18_" + start.toString().replace("-", ""));
        assertThat(references.taxRuleOn("TVA_CG_18", LocalDate.now()).orElseThrow().getRate()).isEqualByComparingTo("18");
        assertThat(references.taxRuleOn("TVA_CG_18", start.plusDays(3)).orElseThrow().getRate()).isEqualByComparingTo("19");
        assertThat(taxRules.findByCode("TVA_CG_18").orElseThrow().getValidUntil()).isEqualTo(start.minusDays(1));
        assertThat(references.taxRuleVersions("TVA_CG_18")).hasSize(2);
    }

    @Test
    void shouldMergeTwoUnitsAndMoveTheItems() {
        String suffix = UUID.randomUUID().toString().substring(0, 5);
        String typo = references.createUnit(new UnitRequest("hr-" + suffix, "Hr " + suffix, null, null, 2, null, 50)).code();
        String right = references.createUnit(new UnitRequest(null, "Heure pleine " + suffix, "heures pleines", "h", 2, null, 51)).code();
        items.save(ServiceCatalogItem.builder().itemCode("SRV-" + suffix).name("Coaching").unitPrice(new BigDecimal("15000")).unit(typo).build());
        entityManager.flush();

        long moved = references.merge(new MergeRequest("UNIT", null, typo, right));
        entityManager.flush();
        entityManager.clear();

        assertThat(moved).isEqualTo(1);
        assertThat(right).startsWith("HEURE_PLEINE_");
        assertThat(items.findByItemCode("SRV-" + suffix).orElseThrow().getUnit()).isEqualTo(right);
        assertThat(references.listUnits(false)).extracting(unit -> unit.code()).contains(right).doesNotContain(typo);
    }

    @Test
    void shouldRefuseTwoSubcategoriesWithTheSameCodeInOneFamily() {
        String family = references.createCategory(new CategoryRequest(null, "Impressions " + UUID.randomUUID().toString().substring(0, 4),
                null, "706100", 5)).code();
        references.createSubcategory(family, new CategoryRequest("A4", "A4 couleur", null, null, 1));

        assertThatThrownBy(() -> references.createSubcategory(family, new CategoryRequest("a4", "A4 noir", null, null, 2)))
                .isInstanceOf(ConflictException.class);
    }
}

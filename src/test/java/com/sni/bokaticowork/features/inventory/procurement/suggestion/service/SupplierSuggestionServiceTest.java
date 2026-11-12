package com.sni.bokaticowork.features.inventory.procurement.suggestion.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryCategoryRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.model.SupplierItem;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierItemRepository;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.dto.SupplierSuggestionDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryLink;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryRef;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryRefTarget;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryLinkRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryRefRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryRefTargetRepository;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupplierSuggestionServiceTest {

    private static final String NATURE = "ACHAT_FOURNITURES_BUREAU";

    @Mock private SupplierRepository supplierRepository;
    @Mock private SupplierItemRepository supplierItemRepository;
    @Mock private SupplierCategoryLinkRepository linkRepository;
    @Mock private SupplierCategoryRefRepository refRepository;
    @Mock private SupplierCategoryRefTargetRepository targetRepository;
    @Mock private SupplierCategoryService categories;
    @Mock private SupplierSuggestionHistory history;
    @Mock private CashOperationNatureRepository natureRepository;
    @Mock private InventoryCategoryRepository inventoryCategoryRepository;
    @Mock private InventoryItemRepository itemRepository;
    @Mock private PlatformSettings settings;

    private SupplierSuggestionService service;

    private final Supplier direct = supplier(1L, "SUP-DIRECT", "Bureau Direct", SupplierStatus.ACTIVE, 3);
    private final Supplier viaCategory = supplier(2L, "SUP-PAPETERIE", "Papeterie Centrale", SupplierStatus.ACTIVE, null);
    private final Supplier historyOnly = supplier(3L, "SUP-HISTO", "Librairie du Port", SupplierStatus.ACTIVE, null);
    private final Supplier blocked = supplier(4L, "SUP-BLOQUE", "Fournisseur bloque", SupplierStatus.BLOCKED, null);
    private final SupplierCategoryRef office = SupplierCategoryRef.builder().id(10L).code("FOURNITURES_BUREAU").label("Fournitures de bureau")
            .active(true).build();
    private final SupplierCategoryRef paper = SupplierCategoryRef.builder().id(11L).code("PAPETERIE").label("Papeterie").parentId(10L)
            .active(true).build();

    @BeforeEach
    void setUp() {
        service = new SupplierSuggestionService(supplierRepository, supplierItemRepository, linkRepository, refRepository, targetRepository,
                categories, history, natureRepository, inventoryCategoryRepository, itemRepository, settings);
        CashOperationNature nature = CashOperationNature.builder().code(NATURE).label("Achat de fournitures de bureau")
                .keywords("papier, ramette, stylo").active(true).usageCount(3L).build();
        when(natureRepository.findByCode(NATURE)).thenReturn(Optional.of(nature));
        when(natureRepository.findAll()).thenReturn(List.of(nature));
        when(settings.number(eq(SupplierSuggestionService.LIMIT_KEY), anyLong())).thenAnswer(invocation -> invocation.getArgument(1));
        when(refRepository.findAllByOrderBySortOrderAscLabelAsc()).thenReturn(List.of(office, paper));
        when(linkRepository.findAllByCategoryKindAndCategoryCodeIn(eq(SupplierCategoryKind.NATURE), eq(List.of(NATURE))))
                .thenReturn(List.of(link(1L, SupplierCategoryKind.NATURE, NATURE), link(4L, SupplierCategoryKind.NATURE, NATURE)));
        // La categorie Fournitures de bureau est reliee a la nature ; le fournisseur 2 est dans sa sous-categorie Papeterie.
        when(targetRepository.findAllByTargetKindAndTargetCode(SupplierCategoryKind.NATURE, NATURE))
                .thenReturn(List.of(SupplierCategoryRefTarget.builder().categoryId(10L).targetKind(SupplierCategoryKind.NATURE).targetCode(NATURE).build()));
        when(linkRepository.findAllByCategoryKindAndCategoryCodeIn(eq(SupplierCategoryKind.SUPPLIER_CATEGORY), anyCollection()))
                .thenAnswer(invocation -> invocation.<java.util.Collection<String>>getArgument(1).contains("PAPETERIE")
                        ? List.of(link(2L, SupplierCategoryKind.SUPPLIER_CATEGORY, "PAPETERIE")) : List.of());
        when(history.byNature(NATURE)).thenReturn(Map.of(3L, 4L, 1L, 1L));
        when(supplierRepository.findAllByStatusOrderByNameAsc(SupplierStatus.ACTIVE)).thenReturn(List.of(direct, historyOnly, viaCategory));
        when(categories.viewsBySupplier(anyCollection())).thenReturn(Map.of());
    }

    @Test
    @DisplayName("Rattache a la nature, puis categorie fournisseur reliee, puis historique seul ; fournisseur bloque ecarte")
    void ranksDeclaredMembershipBeforeHistory() {
        SuggestionResult result = service.suggest(new SuggestionQuery(NATURE, null, null, null, null, null, null));

        assertThat(result.natureCode()).isEqualTo(NATURE);
        assertThat(result.natureGuessed()).isFalse();
        assertThat(result.suggestions()).extracting(Suggestion::supplierCode).containsExactly("SUP-DIRECT", "SUP-PAPETERIE", "SUP-HISTO");
        Suggestion first = result.suggestions().get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.declared()).isTrue();
        assertThat(first.score()).isEqualTo(100 + 5 + 7);
        assertThat(first.reasons()).containsExactly("rattache a la nature « Achat de fournitures de bureau »",
                "deja retenu 1 fois pour cette nature", "delai moyen 3 j");
        assertThat(result.suggestions().get(1).reasons()).containsExactly("categorie « Papeterie »");
        Suggestion last = result.suggestions().get(2);
        assertThat(last.declared()).isFalse();
        assertThat(last.historyCount()).isEqualTo(4L);
        assertThat(last.score()).isEqualTo(20);
        assertThat(last.reasons()).containsExactly("deja retenu 4 fois pour cette nature");
    }

    @Test
    @DisplayName("L'historique est plafonne : il ne depasse jamais une appartenance declaree")
    void historyIsCapped() {
        when(history.byNature(NATURE)).thenReturn(Map.of(3L, 40L));

        SuggestionResult result = service.suggest(new SuggestionQuery(NATURE, null, null, null, null, null, null));

        assertThat(result.suggestions()).extracting(Suggestion::supplierCode).containsExactly("SUP-DIRECT", "SUP-PAPETERIE", "SUP-HISTO");
        assertThat(result.suggestions().get(2).score()).isEqualTo(SupplierSuggestionService.HISTORY_MAX);
    }

    @Test
    @DisplayName("La nature est devinee depuis la designation ; la limite demandee est respectee")
    void guessesNatureAndAppliesLimit() {
        SuggestionResult result = service.suggest(new SuggestionQuery(null, null, null, "Ramettes de papier A4", null, null, 2));

        assertThat(result.natureCode()).isEqualTo(NATURE);
        assertThat(result.natureGuessed()).isTrue();
        assertThat(result.limit()).isEqualTo(2);
        assertThat(result.suggestions()).hasSize(2);
    }

    @Test
    @DisplayName("Prix fournisseur de l'article : prix contractuel, moins cher et prefere comptent, le delai de l'article aussi")
    void itemPriceAndLeadTime() {
        InventoryItem item = InventoryItem.builder().id(50L).itemCode("ART-RAMETTE").build();
        when(itemRepository.findByItemCode("ART-RAMETTE")).thenReturn(Optional.of(item));
        when(supplierItemRepository.findActiveByItemOrderByPriceAndLeadTime(item)).thenReturn(List.of(
                SupplierItem.builder().supplier(historyOnly).item(item).unitPrice(2500L).leadTimeDays(2).preferred(true).active(true).build(),
                SupplierItem.builder().supplier(viaCategory).item(item).unitPrice(3000L).leadTimeDays(8).preferred(false).active(true).build()));

        SuggestionResult result = service.suggest(new SuggestionQuery(NATURE, null, null, null, "ART-RAMETTE", new BigDecimal("50000"), null));

        Suggestion cheapest = result.suggestions().stream().filter(s -> s.supplierCode().equals("SUP-HISTO")).findFirst().orElseThrow();
        assertThat(cheapest.contractUnitPrice()).isEqualTo(2500L);
        assertThat(cheapest.leadTimeDays()).isEqualTo(2);
        assertThat(cheapest.reasons()).contains("prix contractuel pour l'article : 2 500 XAF", "prix le plus bas pour l'article",
                "fournisseur prefere pour l'article", "delai moyen 2 j");
        assertThat(cheapest.score()).isEqualTo(20 + 20 + 10 + 5 + 8);
        assertThat(result.amount()).isEqualByComparingTo("50000");
        assertThat(result.itemCode()).isEqualTo("ART-RAMETTE");
    }

    @Test
    @DisplayName("Sans aucun critere exploitable, la demande est refusee ; une nature inconnue aussi")
    void rejectsMissingCriteria() {
        assertThatThrownBy(() -> service.suggest(new SuggestionQuery(null, null, null, "zzz", null, null, null)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.suggest(new SuggestionQuery("INCONNUE", null, null, null, null, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Nature d'operation inconnue");
    }

    @Test
    @DisplayName("Suggestions par ligne : une ligne sans nature ni designation reconnue rend une liste vide, sans erreur")
    void forLines() {
        List<LineSuggestion> lines = service.forLines(List.of(
                new LineInput(1, "Ramettes", NATURE, new BigDecimal("10000"), null, null),
                new LineInput(2, "zzz", null, BigDecimal.ONE, null, null)), 1);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).result().suggestions()).extracting(Suggestion::supplierCode).containsExactly("SUP-DIRECT");
        assertThat(lines.get(1).result().suggestions()).isEmpty();
    }

    private static Supplier supplier(Long id, String code, String name, SupplierStatus status, Integer leadTime) {
        return Supplier.builder().id(id).supplierCode(code).name(name).status(status).averageLeadTimeDays(leadTime).build();
    }

    private static SupplierCategoryLink link(Long supplierId, SupplierCategoryKind kind, String code) {
        return SupplierCategoryLink.builder().supplierId(supplierId).categoryKind(kind).categoryCode(code).build();
    }
}

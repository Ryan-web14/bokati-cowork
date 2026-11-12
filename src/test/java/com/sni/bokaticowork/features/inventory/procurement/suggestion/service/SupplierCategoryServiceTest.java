package com.sni.bokaticowork.features.inventory.procurement.suggestion.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryCategoryRepository;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.dto.SupplierSuggestionDtos.CategoryRef;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.dto.SupplierSuggestionDtos.RefCategoryRequest;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryLink;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryRef;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryLinkRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryRefRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryRefTargetRepository;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupplierCategoryServiceTest {

    @Mock private SupplierCategoryRefRepository refRepository;
    @Mock private SupplierCategoryRefTargetRepository targetRepository;
    @Mock private SupplierCategoryLinkRepository linkRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private CashOperationNatureRepository natureRepository;
    @Mock private InventoryCategoryRepository inventoryCategoryRepository;
    @Mock private CashActorResolver actors;
    @InjectMocks private SupplierCategoryService service;

    private final SupplierCategoryRef office = SupplierCategoryRef.builder().id(10L).code("FOURNITURES_BUREAU").label("Fournitures de bureau")
            .active(true).build();
    private final SupplierCategoryRef paper = SupplierCategoryRef.builder().id(11L).code("PAPETERIE").label("Papeterie").parentId(10L)
            .active(true).build();
    private final Supplier supplier = Supplier.builder().id(1L).supplierCode("SUP-1").name("Papeterie Centrale").status(SupplierStatus.ACTIVE).build();

    @BeforeEach
    void setUp() {
        when(actors.resolve(anyString())).thenReturn("acheteur@elleaose.com");
        when(refRepository.findByCode("FOURNITURES_BUREAU")).thenReturn(Optional.of(office));
        when(refRepository.findByCode("PAPETERIE")).thenReturn(Optional.of(paper));
        when(refRepository.findAllByOrderBySortOrderAscLabelAsc()).thenReturn(List.of(office, paper));
        when(refRepository.save(any())).thenAnswer(invocation -> {
            SupplierCategoryRef category = invocation.getArgument(0);
            if (category.getId() == null) {
                category.setId(99L);
            }
            return category;
        });
        when(supplierRepository.findBySupplierCode("SUP-1")).thenReturn(Optional.of(supplier));
        when(natureRepository.findByCode("ACHAT_FOURNITURES_BUREAU"))
                .thenReturn(Optional.of(CashOperationNature.builder().code("ACHAT_FOURNITURES_BUREAU").label("Achat de fournitures de bureau").build()));
    }

    @Test
    @DisplayName("Codes verifies a l'ecriture : nature, categorie de stock et categorie fournisseur inconnues refusees")
    void verifiesCodes() {
        assertThat(service.verifiedCode(SupplierCategoryKind.NATURE, " achat_fournitures_bureau ")).isEqualTo("ACHAT_FOURNITURES_BUREAU");
        assertThatThrownBy(() -> service.verifiedCode(SupplierCategoryKind.NATURE, "INCONNUE"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Nature d'operation inconnue");
        assertThatThrownBy(() -> service.verifiedCode(SupplierCategoryKind.INVENTORY_CATEGORY, "RIEN"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Categorie de stock inconnue");
        assertThatThrownBy(() -> service.verifiedCode(SupplierCategoryKind.SUPPLIER_CATEGORY, "RIEN"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Categorie fournisseur inconnue");
        paper.setActive(false);
        assertThatThrownBy(() -> service.verifiedCode(SupplierCategoryKind.SUPPLIER_CATEGORY, "papeterie"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("desactivee");
    }

    @Test
    @DisplayName("Remplacement des categories d'un fournisseur : les absentes sont retirees, les nouvelles ajoutees, les presentes gardees")
    void replacesSupplierCategories() {
        SupplierCategoryLink kept = SupplierCategoryLink.builder().id(100L).supplierId(1L).categoryKind(SupplierCategoryKind.SUPPLIER_CATEGORY)
                .categoryCode("PAPETERIE").build();
        SupplierCategoryLink removed = SupplierCategoryLink.builder().id(101L).supplierId(1L).categoryKind(SupplierCategoryKind.SUPPLIER_CATEGORY)
                .categoryCode("FOURNITURES_BUREAU").build();
        when(linkRepository.findAllBySupplierIdOrderByCategoryKindAscCategoryCodeAsc(1L)).thenReturn(List.of(kept, removed));

        service.replaceFor(supplier, List.of(new CategoryRef(SupplierCategoryKind.SUPPLIER_CATEGORY, "papeterie"),
                new CategoryRef(SupplierCategoryKind.NATURE, "ACHAT_FOURNITURES_BUREAU")));

        verify(linkRepository).delete(removed);
        verify(linkRepository, never()).delete(kept);
        ArgumentCaptor<SupplierCategoryLink> saved = ArgumentCaptor.forClass(SupplierCategoryLink.class);
        verify(linkRepository).save(saved.capture());
        assertThat(saved.getValue().getCategoryKind()).isEqualTo(SupplierCategoryKind.NATURE);
        assertThat(saved.getValue().getCategoryCode()).isEqualTo("ACHAT_FOURNITURES_BUREAU");
        assertThat(saved.getValue().getCreatedBy()).isEqualTo("acheteur@elleaose.com");
    }

    @Test
    @DisplayName("Une categorie utilisee ou parente ne se supprime pas ; une categorie libre, si")
    void deleteOnlyWhenUnused() {
        when(linkRepository.countByCategoryKindAndCategoryCode(SupplierCategoryKind.SUPPLIER_CATEGORY, "PAPETERIE")).thenReturn(2L);
        assertThatThrownBy(() -> service.delete("PAPETERIE")).isInstanceOf(ConflictException.class).hasMessageContaining("desactivez-la");

        when(refRepository.existsByParentId(10L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete("FOURNITURES_BUREAU")).isInstanceOf(ConflictException.class).hasMessageContaining("sous-categories");

        when(linkRepository.countByCategoryKindAndCategoryCode(SupplierCategoryKind.SUPPLIER_CATEGORY, "PAPETERIE")).thenReturn(0L);
        service.delete("PAPETERIE");
        verify(refRepository).delete(paper);
    }

    @Test
    @DisplayName("Une categorie ne devient pas la sous-categorie de sa propre descendante")
    void refusesParentCycle() {
        assertThatThrownBy(() -> service.update("FOURNITURES_BUREAU",
                new RefCategoryRequest(null, "Fournitures de bureau", null, "PAPETERIE", null, null, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("deja une sous-categorie");
    }

    @Test
    @DisplayName("Creation : code deduit du libelle sans accents, natures reliees verifiees")
    void createDerivesCode() {
        when(refRepository.existsByCode("ENTRETIEN_ET_HYGIENE")).thenReturn(false);
        when(refRepository.findByCode("ENTRETIEN_ET_HYGIENE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new RefCategoryRequest(null, "Entretien et hygiène", null, null, null, null,
                List.of("INCONNUE"), null))).isInstanceOf(BadRequestException.class);
        ArgumentCaptor<SupplierCategoryRef> saved = ArgumentCaptor.forClass(SupplierCategoryRef.class);
        verify(refRepository).save(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("ENTRETIEN_ET_HYGIENE");
    }

    @Test
    @DisplayName("Sous-categories a toute profondeur")
    void descendants() {
        SupplierCategoryRef ream = SupplierCategoryRef.builder().id(12L).code("RAMETTES").parentId(11L).build();
        assertThat(SupplierCategoryService.descendants(Set.of(10L), List.of(office, paper, ream))).containsExactlyInAnyOrder(10L, 11L, 12L);
        assertThat(SupplierCategoryService.descendants(Set.of(11L), List.of(office, paper, ream))).containsExactlyInAnyOrder(11L, 12L);
    }
}

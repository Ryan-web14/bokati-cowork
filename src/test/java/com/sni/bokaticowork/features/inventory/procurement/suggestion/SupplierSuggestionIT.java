package com.sni.bokaticowork.features.inventory.procurement.suggestion;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.fund.enums.FundNeedStatus;
import com.sni.bokaticowork.features.fund.model.FundNeedLine;
import com.sni.bokaticowork.features.fund.model.FundNeedStatement;
import com.sni.bokaticowork.features.fund.repository.FundNeedLineRepository;
import com.sni.bokaticowork.features.fund.repository.FundNeedStatementRepository;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.dto.SupplierSuggestionDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.SupplierCategoryService;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.SupplierSuggestionHistory;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.SupplierSuggestionService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Suggestion de fournisseurs sur le schema de production : referentiel de depart et pont vers les
 * natures pose par la migration, classement (nature directe, categorie fournisseur reliee,
 * historique sur les lignes d'etat de besoin), fournisseur inactif ecarte, filtre par categorie.
 */
@Import({SupplierCategoryService.class, SupplierSuggestionService.class, SupplierSuggestionHistory.class, CashActorResolver.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class SupplierSuggestionIT extends PostgresIntegrationTestBase {

    private static final String NATURE = "ACHAT_FOURNITURES_BUREAU";

    @Autowired private SupplierCategoryService categories;
    @Autowired private SupplierSuggestionService suggestions;
    @Autowired private SupplierRepository suppliers;
    @Autowired private FundNeedStatementRepository needs;
    @Autowired private FundNeedLineRepository needLines;
    @Autowired private EntityManager entityManager;
    @MockitoBean private PlatformSettings settings;

    private String code;

    @BeforeEach
    void setUp() {
        when(settings.number(anyString(), anyLong())).thenAnswer(invocation -> invocation.getArgument(1));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("acheteur@elleaose.com", null, List.of()));
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Referentiel de depart, classement explicable et filtre par categorie fournisseur")
    void suggestsSuppliersForANature() {
        // Referentiel pose par la migration, relie a la nature des fournitures de bureau.
        RefCategoryView office = categories.category("FOURNITURES_BUREAU");
        assertThat(office.label()).isEqualTo("Fournitures de bureau");
        assertThat(office.natures()).extracting(TargetView::code).contains(NATURE);
        assertThat(categories.categories(null, true, null)).hasSizeGreaterThanOrEqualTo(10);

        RefCategoryView paper = categories.create(new RefCategoryRequest("PAPETERIE_" + code, "Papeterie " + code, null, "FOURNITURES_BUREAU",
                null, 5, null, null));
        assertThat(paper.parentCode()).isEqualTo("FOURNITURES_BUREAU");

        Supplier direct = supplier("D", "Bureau Direct " + code, SupplierStatus.ACTIVE);
        Supplier viaCategory = supplier("C", "Papeterie Centrale " + code, SupplierStatus.ACTIVE);
        Supplier history = supplier("H", "Librairie " + code, SupplierStatus.ACTIVE);
        Supplier byName = supplier("N", "Ancien Libraire " + code, SupplierStatus.ACTIVE);
        Supplier inactive = supplier("I", "Inactif " + code, SupplierStatus.INACTIVE);
        categories.replaceSupplierCategories(direct.getSupplierCode(), List.of(new CategoryRef(SupplierCategoryKind.NATURE, NATURE)));
        categories.addSupplierCategory(viaCategory.getSupplierCode(), new CategoryRef(SupplierCategoryKind.SUPPLIER_CATEGORY, paper.code()));
        categories.addSupplierCategory(inactive.getSupplierCode(), new CategoryRef(SupplierCategoryKind.NATURE, NATURE));

        // Historique : deux lignes reliees a la fiche, une ligne ancienne en texte libre.
        FundNeedStatement need = needs.save(FundNeedStatement.builder().number("EDB-S-" + code).requester("agent@elleaose.com").subject("Fournitures")
                .status(FundNeedStatus.VALIDATED).totalAmount(new BigDecimal("30000")).build());
        needLine(need, 1, history.getSupplierCode(), history.getName());
        needLine(need, 2, history.getSupplierCode(), history.getName());
        needLine(need, 3, null, "  " + byName.getName().toUpperCase() + " ");
        entityManager.flush();

        SuggestionResult result = suggestions.suggest(new SuggestionQuery(NATURE, null, null, null, null, null, 50));
        List<String> ours = result.suggestions().stream().map(Suggestion::supplierCode)
                .filter(supplierCode -> supplierCode.endsWith(code)).toList();
        assertThat(ours).containsExactly(direct.getSupplierCode(), viaCategory.getSupplierCode(), history.getSupplierCode(), byName.getSupplierCode());
        Suggestion paperSupplier = result.suggestions().stream().filter(s -> s.supplierCode().equals(viaCategory.getSupplierCode())).findFirst().orElseThrow();
        assertThat(paperSupplier.reasons()).contains("categorie « Papeterie " + code + " »");
        assertThat(paperSupplier.categories()).extracting(SupplierCategoryView::label).containsExactly("Papeterie " + code);
        Suggestion historySupplier = result.suggestions().stream().filter(s -> s.supplierCode().equals(history.getSupplierCode())).findFirst().orElseThrow();
        assertThat(historySupplier.declared()).isFalse();
        assertThat(historySupplier.reasons()).contains("deja retenu 2 fois pour cette nature");

        // Nature devinee depuis la designation, suggestions par ligne.
        List<LineSuggestion> lines = suggestions.forLines(List.of(new LineInput(1, "Ramettes de papier", null, BigDecimal.TEN, null, null)), 50);
        assertThat(lines.get(0).result().natureGuessed()).isTrue();

        // Filtre de la liste : la categorie parente englobe ses sous-categories.
        assertThat(categories.supplierIdsIn(SupplierCategoryKind.SUPPLIER_CATEGORY, "FOURNITURES_BUREAU")).contains(viaCategory.getId());
        assertThat(suppliers.searchIn(code, categories.supplierIdsIn(SupplierCategoryKind.SUPPLIER_CATEGORY, paper.code()), PageRequest.of(0, 20))
                .getContent()).extracting(Supplier::getSupplierCode).containsExactly(viaCategory.getSupplierCode());

        // Une categorie utilisee ne se supprime pas ; elle se desactive.
        assertThatThrownBy(() -> categories.delete(paper.code())).isInstanceOf(ConflictException.class);
        assertThat(categories.setActive(paper.code(), false).active()).isFalse();
    }

    private Supplier supplier(String suffix, String name, SupplierStatus status) {
        return suppliers.saveAndFlush(Supplier.builder().supplierCode("SUP-" + suffix + "-" + code).name(name).status(status).build());
    }

    private void needLine(FundNeedStatement need, int lineNo, String supplierCode, String supplier) {
        needLines.save(FundNeedLine.builder().statementId(need.getId()).lineNo(lineNo).designation("Ramettes").quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("10000")).totalAmount(new BigDecimal("10000")).natureCode(NATURE).supplierCode(supplierCode)
                .supplier(supplier).build());
    }
}

package com.sni.bokaticowork.features.inventory.procurement.suggestion.dto;

import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Categories de fournisseurs et suggestion de fournisseurs (complement du lot F13). */
public final class SupplierSuggestionDtos {

    private SupplierSuggestionDtos() {
    }

    // ------------------------------------------------------------------ categories d'un fournisseur

    /** Une categorie a rattacher : sa sorte et son code. */
    public record CategoryRef(@NotNull SupplierCategoryKind kind, @NotBlank String code) {
    }

    /** Une categorie rattachee, avec son libelle. */
    public record SupplierCategoryView(SupplierCategoryKind kind, String code, String label, Boolean active) {
    }

    /** Remplacement de toutes les categories d'un fournisseur. */
    public record SupplierCategoriesRequest(@NotNull @Valid List<CategoryRef> categories) {
    }

    // ------------------------------------------------------------------ referentiel des categories fournisseurs

    public record RefCategoryRequest(
            /** Facultatif a la creation : deduit du libelle. Ignore a la modification. */
            String code,
            @NotBlank String label,
            String description,
            /** Categorie parente (sous-categorie) ; vide : categorie de premier niveau. */
            String parentCode,
            Boolean active,
            Integer sortOrder,
            /** Natures d'operation reliees ; null a la modification : inchangees. */
            List<String> natureCodes,
            /** Categories de stock reliees ; null a la modification : inchangees. */
            List<String> inventoryCategoryCodes) {
    }

    public record TargetView(String code, String label) {
    }

    public record RefCategoryView(String code, String label, String description, String parentCode, String parentLabel, Boolean active,
                                  Integer sortOrder, List<TargetView> natures, List<TargetView> inventoryCategories, Long supplierCount,
                                  Long childCount, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
    }

    // ------------------------------------------------------------------ suggestion

    /**
     * Ce qu'on cherche : une nature (categorie du besoin), une categorie de stock ou une categorie
     * fournisseur ; a defaut de nature, elle est devinee depuis la designation.
     */
    public record SuggestionQuery(String natureCode, String inventoryCategoryCode, String supplierCategoryCode, String designation,
                                  String itemCode, BigDecimal amount, Integer limit) {
    }

    public record Suggestion(Integer rank, String supplierCode, String supplierName, String phone, String email, Integer score,
                             List<String> reasons, Boolean declared, Long historyCount, Long contractUnitPrice, Integer leadTimeDays,
                             List<SupplierCategoryView> categories) {
    }

    public record SuggestionResult(String natureCode, String natureLabel, Boolean natureGuessed, String inventoryCategoryCode,
                                   String supplierCategoryCode, String itemCode, BigDecimal amount, Integer limit,
                                   List<Suggestion> suggestions) {
    }

    /** Suggestions pour une ligne d'etat de besoin ou de demande de fonds. */
    public record LineSuggestion(Integer lineNo, String designation, String natureCode, String supplierCode, String supplier,
                                 SuggestionResult result) {
    }

    /** Ligne a examiner : son numero, sa designation, sa nature, son montant, son fournisseur. */
    public record LineInput(Integer lineNo, String designation, String natureCode, BigDecimal amount, String supplierCode, String supplier) {
    }
}

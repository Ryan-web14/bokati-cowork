package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Lot C0 : requetes et reponses des listes de reference du catalogue (familles, unites, taxes). */
public final class CatalogReferenceDtos {

    private CatalogReferenceDtos() {
    }

    // ------------------------------------------------------------------ requetes

    /**
     * Famille ou sous-famille. Sans code, il est tire du libelle (majuscules, sans accents).
     * Le code ne change plus ensuite.
     */
    public record CategoryRequest(
            @Size(max = 100) String code,
            @NotBlank @Size(max = 150) String label,
            String description,
            @Size(max = 20) String revenueAccount,
            Integer displayOrder) {
    }

    public record CategoryUpdateRequest(
            @Size(max = 150) String label,
            String description,
            @Size(max = 20) String revenueAccount,
            Integer displayOrder) {
    }

    public record UnitRequest(
            @Size(max = 50) String code,
            @NotBlank @Size(max = 100) String label,
            @Size(max = 100) String pluralLabel,
            @Size(max = 20) String symbol,
            @Min(0) @Max(4) Integer quantityScale,
            @Size(max = 50) String inventoryUnitCode,
            Integer displayOrder) {
    }

    public record UnitUpdateRequest(
            @Size(max = 100) String label,
            @Size(max = 100) String pluralLabel,
            @Size(max = 20) String symbol,
            @Min(0) @Max(4) Integer quantityScale,
            @Size(max = 50) String inventoryUnitCode,
            Integer displayOrder) {
    }

    /**
     * Nouvelle regle de taxe. {@code taxType} : {@code VAT} (choisie sur un article) ou
     * {@code ADDITIONAL_CENT} (calculee sur la TVA). {@code treatment} : STANDARD, REDUCED, EXEMPT,
     * OUT_OF_SCOPE ; une exoneration demande son motif.
     */
    public record TaxRuleRequest(
            @NotBlank @Size(max = 120) String code,
            @NotBlank @Size(max = 255) String name,
            String taxType,
            @NotNull @DecimalMin("0") BigDecimal rate,
            String treatment,
            @Size(max = 255) String labelOnDocument,
            @Size(max = 500) String exemptionReason,
            @Size(max = 3) String countryCode,
            LocalDate validFrom,
            Integer displayOrder) {
    }

    /** Libelles et mentions seulement : un taux ne se modifie pas, il se remplace. */
    public record TaxRuleUpdateRequest(
            @Size(max = 255) String name,
            @Size(max = 255) String labelOnDocument,
            @Size(max = 500) String exemptionReason,
            Integer displayOrder) {
    }

    /** Changement de taux : nouvelle version a partir de {@code validFrom}, l'ancienne s'arrete la veille. */
    public record TaxRuleSuccessorRequest(
            @NotNull @DecimalMin("0") BigDecimal rate,
            @NotNull LocalDate validFrom,
            @Size(max = 255) String name,
            @Size(max = 255) String labelOnDocument) {
    }

    /**
     * Fusion de deux entrees (faute de frappe) : les articles passent sur {@code targetCode},
     * {@code sourceCode} est desactive. {@code list} : CATEGORY, SUBCATEGORY ou UNIT ;
     * {@code parentCode} pour une sous-famille.
     */
    public record MergeRequest(
            @NotBlank String list,
            String parentCode,
            @NotBlank String sourceCode,
            @NotBlank String targetCode) {
    }

    // ------------------------------------------------------------------ reponses

    public record CategoryResponse(
            Long id,
            String code,
            String label,
            String parentCode,
            String description,
            String revenueAccount,
            Integer displayOrder,
            Boolean active,
            Boolean createdFromItem,
            long itemCount,
            List<CategoryResponse> subcategories,
            Instant updatedAt) {
    }

    public record UnitResponse(
            Long id,
            String code,
            String label,
            String pluralLabel,
            String symbol,
            Integer quantityScale,
            String inventoryUnitCode,
            Integer displayOrder,
            Boolean active,
            Boolean createdFromItem,
            long itemCount,
            Instant updatedAt) {
    }

    /** Une version de regle de taxe. {@code code} est celui de la version, {@code baseCode} celui que l'article porte. */
    public record TaxRuleResponse(
            Long id,
            String code,
            String baseCode,
            String name,
            String taxType,
            BigDecimal rate,
            String appliesOn,
            String treatment,
            String labelOnDocument,
            String exemptionReason,
            String countryCode,
            LocalDate validFrom,
            LocalDate validUntil,
            Boolean active,
            Integer displayOrder,
            boolean currentlyValid,
            boolean selectableOnItem) {
    }

    /** Resume de la taxe d'un article, dans la reponse de l'article. */
    public record TaxRuleSummary(
            String code,
            String name,
            BigDecimal rate,
            String treatment,
            String labelOnDocument) {
    }

    /**
     * Tout ce qu'il faut pour remplir le formulaire d'un article, en un appel : familles avec
     * leurs sous-familles, unites, taxes choisissables aujourd'hui, et le mode de controle.
     */
    public record ReferencesResponse(
            List<CategoryResponse> categories,
            List<UnitResponse> units,
            List<TaxRuleResponse> taxRules,
            boolean strict) {
    }
}

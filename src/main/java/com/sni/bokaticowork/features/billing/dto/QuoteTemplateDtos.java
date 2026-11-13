package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Lot C14 : modeles de devis et articles composes. */
public final class QuoteTemplateDtos {

    private QuoteTemplateDtos() {
    }

    public record TemplateLine(@NotBlank String itemCode, @NotNull @DecimalMin("0.0001") BigDecimal quantity, @Size(max = 4000) String description) {
    }

    public record TemplateRequest(@NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 255) String name, String description,
                                  @Min(1) @Max(365) Integer validityDays, String terms, @NotEmpty @Valid List<TemplateLine> lines,
                                  Boolean active) {
    }

    public record TemplateView(String code, String name, String description, Integer validityDays, String terms, List<TemplateLine> lines,
                               boolean active, String createdBy, Instant updatedAt) {
    }

    /**
     * Devis tire d'un modele : client, segment de prix (MEMBER, COMPANY, VISITOR) ; les prix suivent
     * l'ordre contrat du client, segment, fiche.
     */
    public record FromTemplateRequest(@NotBlank String customerType, @NotBlank String customerCode, @NotBlank String customerName,
                                      String customerEmail, String customerPhone, String segment, @Size(max = 255) String title) {
    }

    public record Component(@NotBlank String componentCode, @NotBlank String componentSource, @NotNull @DecimalMin("0.0001") BigDecimal quantity) {
    }

    public record ComponentsRequest(@NotNull @Valid List<Component> components) {
    }
}

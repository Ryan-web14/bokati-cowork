package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Pose la meme regle tarifaire sur plusieurs ressources.
 *
 * <p>A ne pas confondre avec « plusieurs plans pour une ressource », qui fonctionne deja : rien
 * ne limite le nombre de regles par ressource, et {@code findApplicableRules} les arbitre par
 * priorite decroissante. Ce qui manquait etait de poser une meme grille sur tout un parc en une
 * fois.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BulkCreateResourcePricingRuleRequest {

    @NotEmpty(message = "Au moins un code ressource est requis")
    private List<String> resourceCodes;

    @NotBlank
    private String bookingUnit;

    @NotNull
    private Integer price;

    private String label;
    private Integer dayOfWeek;
    private LocalTime startsAt;
    private LocalTime endsAt;
    private String adjustmentType;
    private Integer adjustmentValue;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Integer lastMinuteMinutes;
    private Integer priority;
    private Boolean active;

    /** Requete unitaire equivalente · l'appel groupe delegue au meme chemin, validation comprise. */
    public CreateResourcePricingRuleRequest forResource(String resourceCode) {
        CreateResourcePricingRuleRequest request = new CreateResourcePricingRuleRequest();
        request.setResourceCode(resourceCode);
        request.setBookingUnit(bookingUnit);
        request.setPrice(price);
        request.setLabel(label);
        request.setDayOfWeek(dayOfWeek);
        request.setStartsAt(startsAt);
        request.setEndsAt(endsAt);
        request.setAdjustmentType(adjustmentType);
        request.setAdjustmentValue(adjustmentValue);
        request.setValidFrom(validFrom);
        request.setValidUntil(validUntil);
        request.setLastMinuteMinutes(lastMinuteMinutes);
        request.setPriority(priority);
        request.setActive(active);
        return request;
    }
}

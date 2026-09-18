package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.enums.PriceListAudienceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Une grille tarifaire.
 *
 * <p>Un tarif negocie n'est pas une remise. Il ne doit pas apparaitre comme telle sur la facture,
 * et il ne se cumule pas de la meme facon : il <b>remplace</b> le prix catalogue au lieu de le
 * reduire. Afficher « trente pour cent de remise » a un partenaire qui a signe un tarif au poste
 * revient a lui suggerer chaque mois qu'il pourrait obtenir mieux.</p>
 *
 * <p>Une seule grille s'applique a une ligne donnee. En cas de chevauchement, la plus prioritaire
 * gagne, et a priorite egale la plus specifique · un tarif nomme l'emporte sur un tarif de segment,
 * qui l'emporte sur un tarif public.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "price_list", indexes = {
        @Index(name = "idx_price_list_audience", columnList = "audience_type,audience_code,active"),
        @Index(name = "idx_price_list_validity", columnList = "active,valid_from,valid_until,priority")
})
public class PriceList {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 40)
    @Builder.Default
    private PriceListAudienceType audienceType = PriceListAudienceType.ALL;

    @Column(name = "audience_code", length = 120)
    private String audienceCode;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Rang de specificite, du plus precis au plus general. Sert a departager deux grilles de meme
     * priorite : celle qui vise une personne nommee doit l'emporter sur celle qui vise tout le
     * monde, faute de quoi le tarif negocie d'un partenaire serait ecrase par une grille publique.
     */
    public int specificity() {
        return switch (audienceType) {
            case SUBSCRIBER -> 0;
            case BUSINESS_ENTITY -> 1;
            case PARTNER -> 2;
            case SEGMENT -> 3;
            case ALL -> 4;
        };
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

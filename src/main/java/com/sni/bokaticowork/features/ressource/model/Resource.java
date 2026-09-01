package com.sni.bokaticowork.features.ressource.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource",
indexes = {
        @Index(name = "idx_resource_code", columnList = "code"),
        @Index(name = "idx_resource_group_id", columnList = "group_id"),
        @Index(name = "idx_resource_policy_id", columnList = "policy_id"),
        @Index(name = "idx_resource_type_id", columnList = "type_id")
})
@SQLDelete(sql = "UPDATE resource SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Resource {

    @Id
    @IdGeneration
    @Column  (name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", foreignKey = @ForeignKey(name = "fk_resource_type"), nullable = false)
    private ResourceType resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", foreignKey = @ForeignKey(name = "fk_resource_group"))
    private ResourceGroup resourceGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", foreignKey = @ForeignKey(name = "fk_resource_policy"))
    private ResourcePolicy resourcePolicy;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /**
     * Duree d'un creneau de reservation, en minutes. Doit diviser 60.
     *
     * <p>Portee par la ressource et non par chaque disponibilite : le moteur compose une fenetre
     * a partir de creneaux contigus, et des durees melangees sur une meme ressource fausseraient
     * ce decompte en silence. Voir {@code ResourceSlotPolicy}.
     */
    @Column(name = "slot_duration_minutes", nullable = false)
    @Builder.Default
    private Integer slotDurationMinutes = 30;

    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private Integer capacity = 1 ;

    @Column(name = "zone", length = 100)
    private String zone;

    @Column(name = "location_label", length = 150)
    private String locationLabel;

    @Column(name = "status", length = 60)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @Column(name = "booking_enabled", nullable = false)
    @Builder.Default
    private Boolean bookingEnabled = Boolean.TRUE;

    @Column(name = "portal_visible", nullable = false)
    @Builder.Default
    private Boolean portalVisible = Boolean.TRUE;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

    /**
     * Returns the maximum number of concurrent bookings allowed for this resource.
     * If the resource type declares a bookableSlots constraint (e.g. 1 for meeting rooms),
     * that value takes precedence over the person-capacity of the resource.
     */
    public int resolveBookableSlots() {
        if (resourceType != null && resourceType.getBookableSlots() != null) {
            return resourceType.getBookableSlots();
        }
        return capacity != null ? capacity : 1;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

}

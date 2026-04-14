package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.stock.enums.InventoryLocationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_location")
public class InventoryLocation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "location_code", nullable = false, length = 80, unique = true)
    private String locationCode;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 40)
    private InventoryLocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_location_id")
    private InventoryLocation parentLocation;

    @Column(name = "business_code", length = 80)
    private String businessCode;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (active == null) active = Boolean.TRUE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

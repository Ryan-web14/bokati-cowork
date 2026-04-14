package com.sni.bokaticowork.features.inventory.asset.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asset_maintenance")
public class AssetMaintenance {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "maintenance_code", nullable = false, length = 90, unique = true)
    private String maintenanceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 40)
    private AssetMaintenanceType maintenanceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AssetMaintenanceStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "provider_name", length = 180)
    private String providerName;

    @Column(name = "cost")
    private Long cost;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @PrePersist
    void prePersist() {
        if (status == null) status = AssetMaintenanceStatus.PLANNED;
    }
}

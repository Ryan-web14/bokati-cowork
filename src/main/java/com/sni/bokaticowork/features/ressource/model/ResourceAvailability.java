package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(
        name = "resource_availability",
        uniqueConstraints = @UniqueConstraint(name = "uk_resource_availability_slot", columnNames = {"resource_id", "start_at", "end_at"})
)
public class ResourceAvailability {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "resource_availability_resource_fk"))
    private Resource resource;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available =Boolean.TRUE;

    @Column(name = "slot_duration_minutes", nullable = false)
    @Builder.Default
    private Integer slotDurationMinutes = 30;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "remaining_capacity", nullable = false)
    private Integer remainingCapacity;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

}

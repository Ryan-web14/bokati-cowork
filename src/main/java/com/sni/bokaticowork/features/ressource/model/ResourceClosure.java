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
@Table(name = "resource_closure")
public class ResourceClosure {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "resource_availability_resource_fk"))
    private Resource resource;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "reason")
    private String reason;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = Boolean.TRUE;

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

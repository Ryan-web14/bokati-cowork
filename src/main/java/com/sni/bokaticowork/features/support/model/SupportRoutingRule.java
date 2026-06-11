package com.sni.bokaticowork.features.support.model;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "support_routing_rule")
public class SupportRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 40)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 40)
    private TicketPriority priority;

    @Column(name = "owner_type", length = 40)
    private String ownerType;

    @Column(name = "related_type", length = 40)
    private String relatedType;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "team_code", length = 60)
    private String teamCode;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}

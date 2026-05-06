package com.sni.bokaticowork.features.crm.model;

import com.sni.bokaticowork.features.crm.enums.OpportunityStage;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "crm_opportunity", indexes = {
        @Index(name = "idx_crm_opp_lead",     columnList = "lead_id"),
        @Index(name = "idx_crm_opp_stage",    columnList = "stage"),
        @Index(name = "idx_crm_opp_assigned", columnList = "assigned_to")
})
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opportunity_number", nullable = false, unique = true, length = 80)
    private String opportunityNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "estimated_amount", precision = 19, scale = 4)
    private BigDecimal estimatedAmount;

    private Integer probability;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 60)
    @Builder.Default
    private OpportunityStage stage = OpportunityStage.OPEN;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "won_at")
    private Instant wonAt;

    @Column(name = "lost_at")
    private Instant lostAt;

    @Column(name = "lost_reason", columnDefinition = "TEXT")
    private String lostReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = Instant.now();
        if (opportunityNumber == null) {
            opportunityNumber = "OPP-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}

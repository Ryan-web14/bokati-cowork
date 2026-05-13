package com.sni.bokaticowork.features.crm.model;

import com.sni.bokaticowork.features.crm.enums.LeadActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "crm_lead_activity")
public class LeadActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;
    @Enumerated(EnumType.STRING)
    private LeadActivityType activityType;
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String performedBy;
    private Instant performedAt;

    @PrePersist
    public void prePersist() {
        if (performedAt == null) {
            performedAt = Instant.now();
        }
    }
}

package com.sni.bokaticowork.features.crm.model;

import com.sni.bokaticowork.features.crm.enums.LeadInterest;
import com.sni.bokaticowork.features.crm.enums.LeadSource;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
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
@Table(name = "crm_lead", indexes = {
        @Index(name = "idx_crm_lead_number",        columnList = "lead_number"),
        @Index(name = "idx_crm_lead_stage",         columnList = "stage"),
        @Index(name = "idx_crm_lead_assigned",      columnList = "assigned_to"),
        @Index(name = "idx_crm_lead_last_activity", columnList = "last_activity_at")
})
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_number", unique = true, length = 80)
    private String leadNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;
    private String phone;
    private String company;
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 60)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest", length = 60)
    private LeadInterest interest;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 40)
    private LeadStage stage;

    @Column(name = "estimated_amount", precision = 19, scale = 4)
    private BigDecimal estimatedAmount;

    private Integer probability;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "converted_owner_type", length = 60)
    private String convertedOwnerType;

    @Column(name = "converted_owner_code", length = 120)
    private String convertedOwnerCode;

    @Column(name = "lost_reason")
    private String lostReason;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "dormant_alert_sent_at")
    private Instant dormantAlertSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = Instant.now();
        if (leadNumber == null) {
            leadNumber = "LDN-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}

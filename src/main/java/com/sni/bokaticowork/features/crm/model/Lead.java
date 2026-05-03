package com.sni.bokaticowork.features.crm.model;

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
@Table(name = "crm_lead")
public class Lead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    private String email;
    private String phone;
    private String company;
    private String source;
    private String interest;
    @Enumerated(EnumType.STRING)
    private LeadStage stage;
    private BigDecimal estimatedAmount;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private Long assignedTo;
    private String convertedOwnerType;
    private String convertedOwnerCode;
    private String lostReason;
    private Instant createdAt;
    private Instant updatedAt;

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

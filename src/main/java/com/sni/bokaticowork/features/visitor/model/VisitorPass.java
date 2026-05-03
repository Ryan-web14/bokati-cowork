package com.sni.bokaticowork.features.visitor.model;

import com.sni.bokaticowork.features.visitor.enums.VisitorPassStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "visitor_pass")
public class VisitorPass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "pass_number", nullable = false, unique = true)
    private String passNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;
    private String hostMemberCode;
    private String hostName;
    private Instant validFrom;
    private Instant validUntil;
    private String purpose;
    @Enumerated(EnumType.STRING)
    private VisitorPassStatus status;
    private String qrValue;
    private String createdBy;
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

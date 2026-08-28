package com.sni.bokaticowork.features.support.model;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "support_ticket")
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 80)
    private String ticketNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TicketCategory category;

    private String ownerType;
    private String ownerCode;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private Long assignedTo;
    private String relatedType;
    private String relatedCode;
    private Instant firstResponseDueAt;
    private Instant resolutionDueAt;
    private Instant firstRespondedAt;
    private Instant resolvedAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer csatScore;
    private String csatComment;
    private Instant csatSubmittedAt;
    private Instant csatEmailSentAt;

    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private Integer escalationLevel = 0;
    private Instant escalatedAt;
    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;
    private Instant lastSlaAlertSentAt;

    /**
     * One breach alert per ticket per SLA type. These sat outside the escalation cooldown, so a
     * ticket left open past its SLA mailed every hour for as long as it stayed open. Continued
     * nagging is the escalation alert's job, and that one is already paced.
     */
    private Instant firstResponseAlertSentAt;
    private Instant resolutionAlertSentAt;

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketMessage> messages = new ArrayList<>();

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

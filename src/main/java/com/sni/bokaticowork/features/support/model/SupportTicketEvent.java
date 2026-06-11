package com.sni.bokaticowork.features.support.model;

import com.sni.bokaticowork.features.support.enums.TicketEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "support_ticket_event")
public class SupportTicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private TicketEventType eventType;

    @Column(name = "actor_type", length = 20)
    private String actorType;

    @Column(name = "actor_id", length = 120)
    private String actorId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() { createdAt = Instant.now(); }
}

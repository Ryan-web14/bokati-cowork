package com.sni.bokaticowork.features.support.model;

import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "ticket_message")
public class TicketMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private TicketSenderType senderType;

    private String senderId;
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "internal", nullable = false)
    @Builder.Default
    private Boolean internal = Boolean.FALSE;

    @Column(name = "external_message_id", length = 512, unique = true)
    private String externalMessageId;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}

package com.sni.bokaticowork.features.domiciliation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Un passage du pli · qui, quand, quoi. C'est le journal qui fait la preuve de remise. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "mail_item_event")
public class MailItemEvent {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mail_item_id", nullable = false)
    private MailItem mailItem;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    public void prePersist() {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

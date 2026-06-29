package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_event", indexes = {
        @Index(name = "idx_pass_event_pass", columnList = "pass_id"),
        @Index(name = "idx_pass_event_type", columnList = "event_type")
})
public class PassEvent {

    @Id
    @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pass_event_pass"))
    private Pass pass;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    public void prePersist() { occurredAt = Instant.now(); }
}

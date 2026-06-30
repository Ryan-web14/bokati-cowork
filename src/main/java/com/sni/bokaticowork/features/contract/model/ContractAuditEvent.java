package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contract_audit_event",
        indexes = {
                @Index(name = "idx_cae_contract_code", columnList = "contract_code"),
                @Index(name = "idx_cae_occurred_at", columnList = "occurred_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAuditEvent {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "contract_code", nullable = false, length = 120)
    private String contractCode;

    @Column(name = "amendment_code", length = 120)
    private String amendmentCode;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_type", nullable = false, length = 30)
    private String actorType;

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "justification")
    private String justification;
}

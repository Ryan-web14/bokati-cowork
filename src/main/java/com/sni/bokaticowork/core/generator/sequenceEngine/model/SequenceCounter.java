package com.sni.bokaticowork.core.generator.sequenceEngine.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "sequence_counter")
public class SequenceCounter {


    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "sequence_definition_id", nullable = false)
    private Long sequenceDefinitionId;

    @Column(name = "sequence_code", nullable = false, length = 100)
    private String sequenceCode;

    //ex == 2026 // 2026-03
    @Column(name = "period_key", nullable = false)
    private String periodKey;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}

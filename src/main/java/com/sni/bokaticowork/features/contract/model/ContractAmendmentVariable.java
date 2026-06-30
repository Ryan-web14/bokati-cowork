package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contract_amendment_variable",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_amvar_amendment_key",
                columnNames = {"amendment_code", "variable_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAmendmentVariable {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amendment_id", foreignKey = @ForeignKey(name = "fk_amvar_amendment"))
    private ContractAmendment amendment;

    @Column(name = "amendment_code", nullable = false, length = 120)
    private String amendmentCode;

    @Column(name = "variable_key", nullable = false, length = 120)
    private String variableKey;

    @Column(name = "previous_value")
    private String previousValue;

    @Column(name = "new_value", nullable = false)
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

package com.sni.bokaticowork.features.payment.compliance.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Un gel sur instruction · distinct d'une suspension commerciale.
 *
 * <p>Une suspension est une decision de l'etablissement, qu'il peut lever seul. Un gel sur
 * instruction vient d'une autorite, porte sa reference, et ne se leve que sur une autre reference.
 * Les deux figent le portefeuille de la meme facon ; ce qui differe est qui repond de quoi, et
 * c'est precisement ce que la trace doit dire.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "compliance_freeze")
public class ComplianceFreeze {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "freeze_number", nullable = false, unique = true, length = 100)
    private String freezeNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletAccount wallet;

    @Column(name = "authority", nullable = false)
    private String authority;

    @Column(name = "instruction_reference", nullable = false)
    private String instructionReference;

    @Column(name = "instruction_date")
    private LocalDate instructionDate;

    @Column(name = "rationale", nullable = false, columnDefinition = "text")
    private String rationale;

    @Column(name = "frozen_by", nullable = false, length = 120)
    private String frozenBy;

    @Column(name = "frozen_at", nullable = false)
    private Instant frozenAt;

    @Column(name = "lifted_by", length = 120)
    private String liftedBy;

    @Column(name = "lifted_at")
    private Instant liftedAt;

    @Column(name = "lift_reference")
    private String liftReference;

    @Column(name = "lift_rationale", columnDefinition = "text")
    private String liftRationale;

    @Column(name = "case_number", length = 100)
    private String caseNumber;

    public boolean active() {
        return liftedAt == null;
    }

    @PrePersist
    public void prePersist() {
        frozenAt = frozenAt == null ? Instant.now() : frozenAt;
    }
}

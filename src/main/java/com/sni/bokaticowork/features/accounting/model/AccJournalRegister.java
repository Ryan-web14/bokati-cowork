package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Caisse autorisee a ecrire dans un journal (lot F3, plan section 7.3). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_journal_register")
public class AccJournalRegister {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "journal_code", nullable = false, length = 30)
    private String journalCode;

    @Column(name = "cash_register_code", nullable = false, length = 100)
    private String cashRegisterCode;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

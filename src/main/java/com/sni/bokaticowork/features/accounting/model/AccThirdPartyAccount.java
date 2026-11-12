package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Compte auxiliaire d'un tiers sous un compte collectif (411, 401, 421...). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_third_party_account")
public class AccThirdPartyAccount {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.ThirdPartyType partyType;

    @Column(name = "party_code", nullable = false, length = 120)
    private String partyCode;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

package com.sni.bokaticowork.features.domiciliation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sni.bokaticowork.core.baseClasses.model.Address;

import java.time.Instant;

/**
 * Une adresse attribuable · le registre.
 *
 * <p>Deux societes peuvent partager une adresse, le complement distinctif les separe. Le registre
 * dit lesquelles existent, lesquelles peuvent porter une qualite fiscale, et combien de domicilies
 * chacune accueille.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "domiciliation_address")
public class DomiciliationAddress {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    /** Une adresse qui ne peut pas porter de qualite fiscale ne la donne a personne, quel que soit l'engagement. */
    @Column(name = "fiscal_capable", nullable = false)
    @Builder.Default
    private Boolean fiscalCapable = Boolean.TRUE;

    @Column(name = "max_occupants")
    private Integer maxOccupants;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

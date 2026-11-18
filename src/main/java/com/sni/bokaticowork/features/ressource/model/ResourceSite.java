package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Lot R5b : site (adresse, horaires affiches, fuseau) auquel des ressources sont rattachees. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_site")
public class ResourceSite {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "time_zone", nullable = false, length = 60)
    @Builder.Default
    private String timeZone = "Africa/Brazzaville";

    /** Horaires affiches (texte libre) ; les horaires appliques restent ceux des ressources et de la plateforme. */
    @Column(name = "opening_hours_note", length = 300)
    private String openingHoursNote;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

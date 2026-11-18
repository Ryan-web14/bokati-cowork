package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Lot R5a : jour ferie, horaires exceptionnels ou fermeture d'une date, pour toutes les ressources
 * (ressource vide) ou pour une seule. Le jour propre a la ressource l'emporte sur le jour general.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_calendar_day")
public class ResourceCalendarDay {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "day_date", nullable = false)
    private LocalDate dayDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private ResourceCalendarDayKind kind;

    @Column(name = "label", length = 160)
    private String label;

    /** Plages du jour, {@code HH:mm-HH:mm} separees par des virgules ; vide = ferme. */
    @Column(name = "opening_hours", length = 200)
    private String openingHours;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

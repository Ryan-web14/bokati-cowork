package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_closure")
public class ResourceClosure {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "resource_availability_resource_fk"))
    private Resource resource;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "reason")
    private String reason;

    /** Lot R5a : nature du blocage (fermeture, maintenance, menage, evenement prive, autre). */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    @Builder.Default
    private ResourceClosureKind kind = ResourceClosureKind.CLOSURE;

    /** Lot R5a : repetition (une fois, chaque semaine, chaque annee) a partir de la premiere occurrence. */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence", nullable = false, length = 10)
    @Builder.Default
    private ResourceClosureRecurrence recurrence = ResourceClosureRecurrence.NONE;

    /** Lot R5a : derniere date d'une repetition (vide : sans fin). */
    @Column(name = "recurrence_until")
    private java.time.LocalDate recurrenceUntil;

    /**
     * Lot R5a : occurrences qui chevauchent [from, to[ (debut, fin), 400 au plus.
     */
    public java.util.List<LocalDateTime[]> occurrences(LocalDateTime from, LocalDateTime to) {
        java.util.List<LocalDateTime[]> result = new java.util.ArrayList<>();
        ResourceClosureRecurrence mode = recurrence == null ? ResourceClosureRecurrence.NONE : recurrence;
        for (int k = 0; k < 2000 && result.size() < 400; k++) {
            LocalDateTime start = switch (mode) {
                case NONE -> startedAt;
                case WEEKLY -> startedAt.plusWeeks(k);
                case YEARLY -> startedAt.plusYears(k);
            };
            LocalDateTime end = switch (mode) {
                case NONE -> endedAt;
                case WEEKLY -> endedAt.plusWeeks(k);
                case YEARLY -> endedAt.plusYears(k);
            };
            if (recurrenceUntil != null && start.toLocalDate().isAfter(recurrenceUntil)) {
                break;
            }
            if (to != null && !start.isBefore(to)) {
                break;
            }
            if (from == null || end.isAfter(from)) {
                result.add(new LocalDateTime[]{start, end});
            }
            if (mode == ResourceClosureRecurrence.NONE) {
                break;
            }
        }
        return result;
    }

    public boolean occursWithin(LocalDateTime from, LocalDateTime to) {
        return !occurrences(from, to).isEmpty();
    }

    @Column(name = "active")
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = com.sni.bokaticowork.core.utils.time.AppTime.now();
        updatedAt = com.sni.bokaticowork.core.utils.time.AppTime.now();
    }
}

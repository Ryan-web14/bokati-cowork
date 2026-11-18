package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Lot R5d : modele de tache cree a chaque reservation confirmee (preparer la salle, remettre en
 * etat), ou a chaque service vendu (servir le cafe). Portee : une ressource, un service du
 * catalogue, ou toutes les ressources (ni l'un ni l'autre).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_task_template")
public class ResourceTaskTemplate {

    /** Moment de la tache : avant le debut, ou apres la fin de la reservation. */
    public enum Moment { BEFORE_START, AFTER_END }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    /** Service du catalogue qui declenche la tache (vide : la reservation elle-meme). */
    @Column(name = "service_item_code", length = 80)
    private String serviceItemCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "moment", nullable = false, length = 20)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Builder.Default
    private Moment moment = Moment.BEFORE_START;

    /** Minutes avant le debut (ou apres la fin). */
    @Column(name = "offset_minutes", nullable = false)
    @Builder.Default
    private Integer offsetMinutes = 30;

    /** LOW, MEDIUM, HIGH ou URGENT. */
    @Column(name = "priority", length = 20)
    private String priority;

    /** Utilisateur de l'equipe a qui la tache revient (vide : non attribuee). */
    @Column(name = "assigned_to")
    private Long assignedTo;

    /** Liste de verification de la tache, une ligne par point. */
    @Column(name = "checklist", length = 2000)
    private String checklist;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

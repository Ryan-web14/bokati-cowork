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
 * Lot R5d : incident signale sur une ressource (par un membre ou par l'equipe), avec blocage
 * eventuel de la ressource (maintenance), ticket au support et resolution : l'historique de
 * maintenance de la ressource.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_incident")
public class ResourceIncident {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "booking_number", length = 60)
    private String bookingNumber;

    /** MEMBER ou TEAM. */
    @Column(name = "reported_by_type", nullable = false, length = 10)
    private String reportedByType;

    @Column(name = "reporter_code", length = 80)
    private String reporterCode;

    @Column(name = "reporter_name", length = 160)
    private String reporterName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    /** LOW, MEDIUM, HIGH ou CRITICAL. */
    @Column(name = "severity", nullable = false, length = 10)
    @Builder.Default
    private String severity = "MEDIUM";

    /** OPEN, IN_PROGRESS, RESOLVED. */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    /** La ressource a ete mise en maintenance a cause de cet incident. */
    @Column(name = "blocked", nullable = false)
    @Builder.Default
    private Boolean blocked = Boolean.FALSE;

    /** Documents de la GED (photos) joints au signalement. */
    @Column(name = "photo_document_codes", length = 1000)
    private String photoDocumentCodes;

    @Column(name = "ticket_number", length = 60)
    private String ticketNumber;

    @Column(name = "resolution", length = 2000)
    private String resolution;

    @Column(name = "resolved_by", length = 150)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

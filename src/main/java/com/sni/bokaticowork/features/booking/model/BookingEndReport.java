package com.sni.bokaticowork.features.booking.model;

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

/** Lot R5d : etat des lieux de fin d'utilisation (liste de verification, photos, etat). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_end_report")
public class BookingEndReport {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** GOOD ou DAMAGED. */
    @Column(name = "condition", nullable = false, length = 10)
    private String condition;

    /** Points verifies, « point|oui » ou « point|non », un par ligne. */
    @Column(name = "checklist", length = 4000)
    private String checklist;

    /** Documents de la GED (photos), separes par des virgules. */
    @Column(name = "photo_document_codes", length = 1000)
    private String photoDocumentCodes;

    @Column(name = "notes", length = 2000)
    private String notes;

    /** MEMBER ou TEAM. */
    @Column(name = "reported_by_type", nullable = false, length = 10)
    private String reportedByType;

    @Column(name = "reported_by", length = 150)
    private String reportedBy;

    /** Incident ouvert si l'etat est DAMAGED. */
    @Column(name = "incident_code", length = 40)
    private String incidentCode;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

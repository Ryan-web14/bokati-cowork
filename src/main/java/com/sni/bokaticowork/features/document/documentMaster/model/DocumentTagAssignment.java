package com.sni.bokaticowork.features.document.documentMaster.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_tag_assignment")
public class DocumentTagAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private DocumentTag tag;

    @Column(name = "tagged_by")
    private Long taggedBy;

    @Column(name = "tagged_at", nullable = false)
    private Instant taggedAt;

    @PrePersist
    public void prePersist() {
        if (taggedAt == null) taggedAt = Instant.now();
    }
}

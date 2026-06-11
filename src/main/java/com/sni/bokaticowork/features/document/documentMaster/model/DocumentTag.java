package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
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
@Table(name = "document_tag")
public class DocumentTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "space", length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentSpace space;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

package com.sni.bokaticowork.features.billing.model;

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

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_sequence")
public class DocumentSequence {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

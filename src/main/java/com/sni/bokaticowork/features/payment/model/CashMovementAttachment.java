package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_movement_attachment")
public class CashMovementAttachment {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_movement_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cash_movement_attachment_movement"))
    private CashMovement cashMovement;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "uploaded_by", length = 120)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @PrePersist
    public void prePersist() {
        uploadedAt = Instant.now();
    }
}

package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_permission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_permission",
                columnNames = {"target_type", "target_id", "grantee_type", "grantee_id", "permission"})
})
public class DocumentPermission {
    @Id @IdGeneration @Column(name = "id") private Long id;
    @Column(name = "target_type", nullable = false, length = 20) private String targetType;
    @Column(name = "target_id", nullable = false) private Long targetId;
    @Column(name = "grantee_type", nullable = false, length = 20) private String granteeType;
    @Column(name = "grantee_id", nullable = false) private Long granteeId;
    @Column(name = "permission", nullable = false, length = 20) private String permission;
    @Column(name = "granted_by") private Long grantedBy;
    @Column(name = "granted_at", nullable = false) private Instant grantedAt;

    @PrePersist public void prePersist() { grantedAt = grantedAt == null ? Instant.now() : grantedAt; }
}

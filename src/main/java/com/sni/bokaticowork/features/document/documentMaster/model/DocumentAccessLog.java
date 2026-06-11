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
@Table(name = "document_access_log")
public class DocumentAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @PrePersist
    public void prePersist() {
        if (accessedAt == null) accessedAt = Instant.now();
    }
}

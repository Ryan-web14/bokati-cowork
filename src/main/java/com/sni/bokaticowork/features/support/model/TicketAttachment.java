package com.sni.bokaticowork.features.support.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "ticket_attachment")
public class TicketAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private TicketMessage message;

    @Column(name = "document_code", nullable = false)
    private String documentCode;

    private String fileName;

    @Column(name = "storage_provider", length = 20)
    private String storageProvider;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "internal", nullable = false)
    @Builder.Default
    private Boolean internal = Boolean.FALSE;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}

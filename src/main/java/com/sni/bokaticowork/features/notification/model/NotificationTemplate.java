package com.sni.bokaticowork.features.notification.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "notification_template", indexes = {
        @Index(name = "idx_notification_template_code", columnList = "template_code"),
        @Index(name = "idx_notification_template_channel", columnList = "channel,active")
})
public class NotificationTemplate {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "template_code", nullable = false, unique = true, length = 120)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 40)
    private NotificationChannel channel;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "template_name", length = 180)
    private String templateName;

    @Column(name = "body_template", columnDefinition = "text")
    private String bodyTemplate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "admin_only", nullable = false)
    @Builder.Default
    private Boolean adminOnly = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (active == null) {
            active = true;
        }
        if (adminOnly == null) {
            adminOnly = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

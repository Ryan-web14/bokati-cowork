package com.sni.bokaticowork.core.communication.whatsapp;

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

/** Journal d'un envoi WhatsApp. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "whatsapp_message")
public class WhatsAppMessage {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "to_phone")
    private String toPhone;

    /** TEXT ou TEMPLATE. */
    @Column(name = "kind")
    private String kind;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "content")
    private String content;

    @Column(name = "reference")
    private String reference;

    @Column(name = "status_group")
    private String statusGroup;

    @Column(name = "status_name")
    private String statusName;

    @Column(name = "error")
    private String error;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}

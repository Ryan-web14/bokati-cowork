package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Destinataires et canaux habituels des pieces d'un client (lot B4). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_recipient_preference")
public class BillingRecipientPreference {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_type", nullable = false, length = 30)
    private String customerType;

    @Column(name = "customer_code", nullable = false, length = 80)
    private String customerCode;

    @Column(name = "to_addresses", columnDefinition = "text")
    private String toAddresses;

    @Column(name = "cc_addresses", columnDefinition = "text")
    private String ccAddresses;

    @Column(name = "bcc_addresses", columnDefinition = "text")
    private String bccAddresses;

    @Column(name = "whatsapp_numbers", columnDefinition = "text")
    private String whatsappNumbers;

    /** Canaux par defaut, separes par des virgules (EMAIL, WHATSAPP). */
    @Column(name = "channels", length = 60)
    private String channels;

    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}

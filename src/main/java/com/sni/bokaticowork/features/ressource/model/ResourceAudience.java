package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lot B10 : client ou groupe de clients designe pour une ressource reservee ou sur demande.
 * Types : MEMBER, CUSTOMER (client ou organisation), BUSINESS_ENTITY, SEGMENT (PUBLIC, MEMBER,
 * SUBSCRIBER, COMPANY, PARTNER), PLAN (code d'un plan d'abonnement).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_audience")
public class ResourceAudience {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "audience_type", nullable = false, length = 20)
    private String audienceType;

    @Column(name = "audience_code", nullable = false, length = 80)
    private String audienceCode;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public String key() {
        return audienceType + ":" + audienceCode;
    }
}

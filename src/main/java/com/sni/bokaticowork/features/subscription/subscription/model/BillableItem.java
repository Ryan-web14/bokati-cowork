package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billable_item", indexes = {
        @Index(name = "idx_billable_item_status", columnList = "status"),
        @Index(name = "idx_billable_item_source", columnList = "source_type,source_id"),
        @Index(name = "idx_billable_item_subscriber", columnList = "subscriber_type,subscriber_code")
})
public class BillableItem {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "billable_number", nullable = false, unique = true, length = 100)
    private String billableNumber;

    @Column(name = "source_type", nullable = false, length = 80)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 120)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type", nullable = false, length = 60)
    private SubscriberType subscriberType;

    @Column(name = "subscriber_code", nullable = false, length = 120)
    private String subscriberCode;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "tax_code", length = 80)
    private String taxCode;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private BillableItemStatus status = BillableItemStatus.PENDING;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

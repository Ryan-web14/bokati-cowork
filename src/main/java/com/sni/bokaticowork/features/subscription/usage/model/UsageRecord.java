package com.sni.bokaticowork.features.subscription.usage.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.usage.enums.UsageRecordStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "usage_record", indexes = {
        @Index(name = "idx_usage_record_owner", columnList = "owner_type,owner_code,status"),
        @Index(name = "idx_usage_record_reference", columnList = "reference_type,reference_id"),
        @Index(name = "idx_usage_record_entitlement", columnList = "entitlement_code")
})
public class UsageRecord {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "usage_number", nullable = false, unique = true, length = 100)
    private String usageNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Column(name = "entitlement_code", nullable = false, length = 100)
    private String entitlementCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 40)
    private EntitlementUnit unit;

    @Column(name = "reference_type", nullable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @Column(name = "billable", nullable = false)
    @Builder.Default
    private Boolean billable = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billable_item_id", foreignKey = @ForeignKey(name = "fk_usage_record_billable_item"))
    private BillableItem billableItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private UsageRecordStatus status = UsageRecordStatus.RECORDED;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @PrePersist
    public void prePersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}

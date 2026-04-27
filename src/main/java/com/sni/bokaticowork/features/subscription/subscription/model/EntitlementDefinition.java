package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.subscription.subscription.enums.ConsumptionMode;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementResetPolicy;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementType;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "entitlement_definition", indexes = {
        @Index(name = "idx_entitlement_definition_code", columnList = "code")
})
@SQLDelete(sql = "UPDATE entitlement_definition SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class EntitlementDefinition {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_type", nullable = false, length = 60)
    private EntitlementType entitlementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 40)
    private EntitlementUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_mode", nullable = false, length = 60)
    private ConsumptionMode consumptionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_policy", nullable = false, length = 60)
    private EntitlementResetPolicy resetPolicy;

    @Column(name = "stackable", nullable = false)
    @Builder.Default
    private Boolean stackable = Boolean.TRUE;

    @Column(name = "transferable", nullable = false)
    @Builder.Default
    private Boolean transferable = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_type_id", foreignKey = @ForeignKey(name = "fk_entitlement_resource_type"))
    private ResourceType resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_group_id", foreignKey = @ForeignKey(name = "fk_entitlement_resource_group"))
    private ResourceGroup resourceGroup;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

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

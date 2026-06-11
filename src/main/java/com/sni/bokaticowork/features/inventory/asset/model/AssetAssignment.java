package com.sni.bokaticowork.features.inventory.asset.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssignmentStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asset_assignment")
public class AssetAssignment {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type", nullable = false, length = 40)
    private AssetAssigneeType assigneeType;

    @Column(name = "assignee_code", nullable = false, length = 120)
    private String assigneeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AssetAssignmentStatus status;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "expected_return_at")
    private Instant expectedReturnAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "assigned_by", length = 120)
    private String assignedBy;

    @Column(name = "returned_by", length = 120)
    private String returnedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkout_condition", length = 40)
    private AssetCondition checkoutCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_condition", length = 40)
    private AssetCondition returnCondition;

    @Column(name = "checkout_photo_url", length = 500)
    private String checkoutPhotoUrl;

    @Column(name = "return_photo_url", length = 500)
    private String returnPhotoUrl;

    @Column(name = "receiver_signature_url", length = 500)
    private String receiverSignatureUrl;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    void prePersist() {
        if (status == null) status = AssetAssignmentStatus.ACTIVE;
        if (startAt == null) startAt = Instant.now();
    }
}

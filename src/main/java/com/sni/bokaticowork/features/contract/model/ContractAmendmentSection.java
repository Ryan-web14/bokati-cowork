package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.contract.enums.ContractAmendmentSectionAction;
import com.sni.bokaticowork.features.contract.enums.ContractSectionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contract_amendment_section")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAmendmentSection {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amendment_id", foreignKey = @ForeignKey(name = "fk_amsec_amendment"))
    private ContractAmendment amendment;

    @Column(name = "amendment_code", nullable = false, length = 120)
    private String amendmentCode;

    @Column(name = "action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ContractAmendmentSectionAction action;

    /** Titre ou référence de la section originale visée (pour MODIFY / REMOVE) */
    @Column(name = "target_section_ref", length = 500)
    private String targetSectionRef;

    @Column(name = "section_type", length = 40)
    @Enumerated(EnumType.STRING)
    private ContractSectionType sectionType;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        sectionOrder = sectionOrder == null ? 0 : sectionOrder;
        sectionType = sectionType == null ? ContractSectionType.ARTICLE : sectionType;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

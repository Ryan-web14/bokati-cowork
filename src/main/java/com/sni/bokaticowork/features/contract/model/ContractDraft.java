package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.contract.enums.ContractDraftStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "contract_draft")
public class ContractDraft {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 120)
    private String code;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_code", length = 120)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 50)
    private DocumentOwnerType ownerType;

    @Column(name = "owner_code", length = 120)
    private String ownerCode;

    @Column(name = "business_code", length = 120)
    private String businessCode;

    @Column(name = "css_content", columnDefinition = "TEXT")
    private String cssContent;

    @Column(name = "header_html", columnDefinition = "TEXT")
    private String headerHtml;

    @Column(name = "footer_html", columnDefinition = "TEXT")
    private String footerHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ContractDraftStatus status = ContractDraftStatus.DRAFTING;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sectionOrder ASC")
    @Builder.Default
    private List<ContractDraftSection> sections = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

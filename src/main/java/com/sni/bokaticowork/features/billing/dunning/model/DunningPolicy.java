package com.sni.bokaticowork.features.billing.dunning.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Une politique de relance · un segment, un ton, des paliers.
 *
 * <p>Un grand compte et un particulier ne se relancent pas au meme rythme ni sur le meme ton. Les
 * paliers sont des donnees : les changer ne demande pas de livrer.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "dunning_policy")
public class DunningPolicy {

    /** Le segment vise · DEFAULT pour ceux qui n'ont pas la leur. */
    public enum Segment {
        DEFAULT, MEMBER, CUSTOMER, BUSINESS_ENTITY
    }

    public enum Tone {
        SOFT, STANDARD, FIRM
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "policy_code", nullable = false, unique = true, length = 40)
    private String policyCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false, length = 40)
    @Builder.Default
    private Segment segment = Segment.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", nullable = false, length = 20)
    @Builder.Default
    private Tone tone = Tone.STANDARD;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<DunningStep> steps = new ArrayList<>();

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

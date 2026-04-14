package com.sni.bokaticowork.core.generator.sequenceEngine.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.core.generator.sequenceEngine.enums.ResetPolicy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "sequence_definition")
public class SequenceDefinition {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    //Example == INVOICE / INV / DEVIS / DVb
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "prefix", length = 100)
    private String prefix;

    @Column(name = "suffix", length = 50)
    private String suffix;

    /**
     * Examples:
     * {PREFIX}-{YYYY}-{SEQ}
     * {PREFIX}-{YYYY}{MM}-{SEQ}
     * {PREFIX}-{SEQ}
     */
    @Column(name = "pattern", nullable = false, length = 255)
    private String pattern;


    @Column(name = "padding", nullable = false)
    private Integer padding;

    @Column(name = "initial_value", nullable = false)
    private Long initialValue;

    @Column(name = "increment_step", nullable = false)
    private Integer incrementStep;

    @Column(name = "reset_policy", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResetPolicy resetPolicy;

    @Column(name  = "enabled")
    private Boolean enabled;

    @Column(name = "system_managed", nullable = false)
    private Boolean systemManaged;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();

        if (this.initialValue == null) {
            this.initialValue = 1L;
        }
        if (this.incrementStep == null) {
            this.incrementStep = 1;
        }
        if (this.padding == null) {
            this.padding = 6;
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
        if (this.systemManaged == null) {
            this.systemManaged = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }



}

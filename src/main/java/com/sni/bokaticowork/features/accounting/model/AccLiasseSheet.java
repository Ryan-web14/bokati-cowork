package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Feuillet du modele de liasse fiscale, versionne (lot F15). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_liasse_sheet")
public class AccLiasseSheet {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "code")
    private String code;

    @Column(name = "title")
    private String title;

    @Column(name = "kind")
    private String kind;

    @Column(name = "source")
    private String source;

    @Column(name = "position")
    private Integer position;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}

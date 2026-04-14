package com.sni.bokaticowork.core.baseClasses.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    //Currency code needs to respect the ISO 4217:2013 norm
    @Column(name = "currency_code", nullable = false, unique = true)
    @Size(min = 3, max = 3)
    private String currencyCode;

    @Column(name = "currency_name", nullable = false)
    private String currencyName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

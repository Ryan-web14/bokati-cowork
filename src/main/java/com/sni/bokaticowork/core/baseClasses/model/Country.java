package com.sni.bokaticowork.core.baseClasses.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "country")
@SQLDelete(sql = "UPDATE country SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Country {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country_code")
    @Size(min = 2, max = 3)
    private String countryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_currency_code", foreignKey = @ForeignKey(name = "country_default_currency_code_fk"))
    private Currency defaultCurrency;

    @Column(name = "phone_code", nullable = false)
    private String phoneCode;

    @Column(name = "is_ohada_member", nullable = false)
    private Boolean isOhadaMember;

    @Builder.Default
    @Column(name = "deleted")
    private boolean deleted = Boolean.FALSE;

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

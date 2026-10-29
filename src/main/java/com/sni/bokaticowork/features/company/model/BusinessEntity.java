package com.sni.bokaticowork.features.company.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.company.enums.Status;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

//TODO: add in the sql script a delete column
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "business_entity")
@SQLDelete(sql = "UPDATE business_entity SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class BusinessEntity {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entity_code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "legal_form", nullable = false)
    @NotBlank(message = "the legal form of the business is required")
    private String legalForm;

    /** Facultatif · imprime sur la facture seulement s'il est saisi. */
    @Column(name = "niu_number")
    @Size(max = 18, message = "Niu number must be at most 18 characters")
    private String niuNumber;

    /** Facultatif · une association ou une institution publique n'en a pas. */
    @Column(name = "rccm_number")
    private String rccmNumber;

    /** Ce qu'est l'organisation · decide des identifiants proposes. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false, length = 40)
    private com.sni.bokaticowork.features.company.enums.OrganizationType organizationType =
            com.sni.bokaticowork.features.company.enums.OrganizationType.SOCIETE;

    @Column(name = "legal_representative_name", length = 200)
    private String legalRepresentativeName;

    @Column(name = "legal_representative_title", length = 120)
    private String legalRepresentativeTitle;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_regime", nullable = false, length = 20)
    private com.sni.bokaticowork.features.company.enums.VatRegime vatRegime =
            com.sni.bokaticowork.features.company.enums.VatRegime.SUBJECT;

    /** Le texte imprime sur la facture d'une organisation exoneree (article, decision). */
    @Column(name = "vat_exemption_note", length = 500)
    private String vatExemptionNote;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "activity")
    private String activity;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", foreignKey = @ForeignKey(name = "business_entity_address_fk"))
    private Address address;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    @Email(message = "Email is not valid")
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_id", nullable = false,
            foreignKey = @ForeignKey(name = "base_currency_id_fk"))
    private Currency baseCurrency;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Builder.Default
    @Column(name = "deleted")
    private boolean deleted = Boolean.FALSE;

    /**
     * Vrai pour l entite qui exploite l espace · la partie qui signe face au client.
     *
     * <p>Cette table porte deux roles : l espace lui-meme et les clients entreprises. La
     * generation de contrat prenait « la seule ligne active » pour l exploitant, ce qui ne tient
     * que tant qu il n y a qu une ligne au total. Un index unique partiel garantit qu au plus
     * une ligne vivante porte ce drapeau.</p>
     */
    @Builder.Default
    @Column(name = "is_operator", nullable = false)
    private boolean operator = Boolean.FALSE;

    @Column(name = "created_by", nullable = false)
    private String created_by;

    @Column(name = "updated_by")
    private String updated_by;

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

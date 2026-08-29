package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_document_edit_history")
public class BillingDocumentEditHistory {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_edit_history_document"))
    private BillingDocument document;

    @Column(name = "edit_type", nullable = false, length = 50)
    private String editType;

    @Column(name = "changed_by", nullable = false, length = 120)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /**
     * Etat complet du document <b>avant</b> la modification · en-tete, lignes, remises. C'est
     * cette colonne qui fait la difference entre savoir qu'une modification a eu lieu et savoir
     * ce qu'elle a change.
     */
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "snapshot_json", columnDefinition = "jsonb")
    private String snapshotJson;

    /** Numero croissant par document, a partir de 1. */
    @Column(name = "version_number")
    private Integer versionNumber;

    /**
     * Horodatage de l'envoi de cette version au client. Distingue un brouillon retouche d'une
     * proposition reellement transmise · seules les versions envoyees ont une valeur probante.
     */
    @Column(name = "sent_to_customer_at")
    private Instant sentToCustomerAt;

    /** Resume lisible des champs modifies, calcule par comparaison avec la version precedente. */
    @Column(name = "change_summary", columnDefinition = "text")
    private String changeSummary;

    @PrePersist
    public void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}

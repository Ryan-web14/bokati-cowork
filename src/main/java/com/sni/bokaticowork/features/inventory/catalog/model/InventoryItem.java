package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.enums.ItemLifecycleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "item_code", nullable = false, length = 80, unique = true)
    private String itemCode;

    @Column(name = "name", nullable = false, length = 220)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "psku", length = 120, unique = true)
    private String psku;

    @Column(name = "short_code", length = 80, unique = true)
    private String shortCode;

    @Column(name = "display_code", length = 120, unique = true)
    private String displayCode;

    @Column(name = "identification_code", length = 120, unique = true)
    private String identificationCode;

    @Column(name = "specification", columnDefinition = "TEXT")
    private String specification;

    @Column(name = "search_text", columnDefinition = "TEXT")
    private String searchText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private InventoryCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private InventoryUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private InventoryItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type", nullable = false, length = 40)
    private InventoryTrackingType trackingType;

    @Column(name = "default_cost")
    private Long defaultCost;

    @Column(name = "sale_price")
    private Long salePrice;

    @Builder.Default
    @Column(name = "taxable", nullable = false)
    private Boolean taxable = Boolean.FALSE;

    @Builder.Default
    @Column(name = "allow_negative_stock", nullable = false)
    private Boolean allowNegativeStock = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_expiry_date", nullable = false)
    private Boolean requiresExpiryDate = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_lot_number", nullable = false)
    private Boolean requiresLotNumber = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_serial_number", nullable = false)
    private Boolean requiresSerialNumber = Boolean.FALSE;

    /**
     * Source de verite du cycle de vie. Le booleen {@link #active} en est derive, et n'est conserve
     * que pour ne pas casser les clients existants.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 30)
    private ItemLifecycleStatus lifecycleStatus = ItemLifecycleStatus.ACTIVE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    /** Revision courante de la specification technique. */
    @Column(name = "revision", length = 40)
    private String revision;

    @Column(name = "weight_kg", precision = 19, scale = 4)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", precision = 19, scale = 6)
    private BigDecimal volumeM3;

    @Column(name = "length_mm")
    private Integer lengthMm;

    @Column(name = "width_mm")
    private Integer widthMm;

    @Column(name = "height_mm")
    private Integer heightMm;

    /** Faux lorsque l'article ne supporte pas d'etre gerbe, ce qui contraint le rangement. */
    @Builder.Default
    @Column(name = "stackable", nullable = false)
    private Boolean stackable = Boolean.TRUE;

    /** Modele d'origine lorsque l'article est une variante generee. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private InventoryItemTemplate template;

    /** Combinaison d'axes de la variante, par exemple {@code TAILLE=M;COULEUR=ROUGE}. */
    @Column(name = "variant_signature", length = 255)
    private String variantSignature;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        applyDefaults();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        applyDefaults();
    }

    private void applyDefaults() {
        if (taxable == null) taxable = Boolean.FALSE;
        if (allowNegativeStock == null) allowNegativeStock = Boolean.FALSE;
        if (requiresExpiryDate == null) requiresExpiryDate = Boolean.FALSE;
        if (requiresLotNumber == null) requiresLotNumber = Boolean.FALSE;
        if (requiresSerialNumber == null) requiresSerialNumber = Boolean.FALSE;
        if (stackable == null) stackable = Boolean.TRUE;
        syncLifecycle();
    }

    /**
     * Derive le booleen historique depuis le statut, qui est la seule source de verite.
     *
     * <p>Faire l'inverse, laisser une ecriture sur {@code active} redefinir le statut, produirait un
     * conflit : passer un article en {@code PHASE_OUT} sans toucher au booleen le ferait aussitot
     * retomber en {@code OBSOLETE}. La traduction d'un {@code active} recu en entree est donc faite
     * une seule fois, dans le service, via {@link ItemLifecycleStatus#fromActiveFlag}.</p>
     */
    private void syncLifecycle() {
        if (lifecycleStatus == null) {
            lifecycleStatus = ItemLifecycleStatus.fromActiveFlag(active);
        }
        active = lifecycleStatus.isActive();
    }

    /**
     * Vrai si l'article peut encore entrer en stock. Un article en fin de serie ne se
     * reapprovisionne plus, un article obsolete ou bloque ne bouge plus du tout.
     */
    public boolean canReceiveStock() {
        return lifecycleStatus != null && lifecycleStatus.isReceivable();
    }

    /** Vrai si l'article peut encore sortir du stock. */
    public boolean canIssueStock() {
        return lifecycleStatus != null && lifecycleStatus.isIssuable();
    }
}

package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.stock.enums.StockJournalDirection;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Ecriture comptable de stock, generee a chaque mouvement valorise.
 *
 * <p>Immuable comme le mouvement dont elle decoule : une correction se fait par contre-passation,
 * qui produit sa propre ecriture inverse.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_journal_entry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_journal_movement", columnNames = "movement_code")
})
public class StockJournalEntry {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Mouvement a l'origine de l'ecriture. Un mouvement produit au plus une ecriture. */
    @Column(name = "movement_code", nullable = false, length = 90)
    private String movementCode;

    @Column(name = "item_code", nullable = false, length = 80)
    private String itemCode;

    @Column(name = "location_code", length = 80)
    private String locationCode;

    /** Compte de stock mouvemente. */
    @Column(name = "stock_account", nullable = false, length = 40)
    private String stockAccount;

    /** Compte de contrepartie, determine par le type de mouvement et son motif. */
    @Column(name = "counterpart_account", nullable = false, length = 40)
    private String counterpartAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private StockJournalDirection direction;

    /** Montant en entier XAF, toujours positif. Le sens est porte par direction. */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** Date comptable, qui rattache l'ecriture a une periode. */
    @Column(name = "accounting_date", nullable = false)
    private LocalDate accountingDate;

    @Column(name = "period_code", length = 40)
    private String periodCode;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}

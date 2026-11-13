package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Lot C4 : ligne d'un bon de livraison, rattachee a la ligne de la piece d'origine. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_delivery_note_line")
public class DeliveryNoteLine {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "source_line_id", nullable = false)
    private Long sourceLineId;

    @Column(name = "line_order", nullable = false)
    private Integer lineOrder;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "line_type", length = 30)
    private String lineType;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;
}

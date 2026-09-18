package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_document_discount")
public class BillingDocumentDiscount {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_billing_discount_document"))
    private BillingDocument document;

    @Column(name = "discount_code", length = 120)
    private String discountCode;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 40)
    private BillingDiscountType discountType;

    @Column(name = "value", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * D'ou vient cette remise · PROMOTION, COUPON, PRICE_LIST, REFERRAL ou MANUAL.
     *
     * <p>{@code discountCode} etait un texte libre sans clef vers quoi que ce soit. On pouvait donc
     * lire une remise sur une facture sans jamais pouvoir dire quelle campagne l'avait produite, ce
     * qui rendait le cout des promotions incalculable autrement qu'a la main.</p>
     */
    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;
}

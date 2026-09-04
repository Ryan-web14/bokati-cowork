package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;

import java.math.BigDecimal;

/**
 * Une remise de document porte deux montants qui ne sont pas le meme chiffre.
 *
 * <p>{@code amount} est ce qui est accorde au client : la remise se deduit du TTC, c'est de ce
 * montant que sa facture baisse. {@code baseAmount} est la part correspondante en hors taxes, une
 * fois la TVA et le centime additionnel retires. Sur une remise de 25 000 TTC, la reduction
 * d'assiette est de 21 026.
 *
 * <p>La colonne des totaux se lit en HT jusqu'a la base taxable : c'est {@code baseAmount} qui doit
 * y figurer pour que le brut moins les remises tombe sur la base taxable. {@code amount} reste
 * l'information que le client reconnait, et s'affiche a cote.
 */
public record BillingDocumentDiscountResponse(
        String discountCode,
        String description,
        BillingDiscountType discountType,
        BigDecimal value,
        BigDecimal amount,
        BigDecimal baseAmount
) {
}

package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;

/** Apercu de la marge d'un article (lot B10). */
public record CatalogMarginResponse(String itemCode, String name, String category, String subcategory, BigDecimal unitPrice, BigDecimal costPrice,
                                    BigDecimal margin, BigDecimal marginRate, BigDecimal floorPrice, BigDecimal maxDiscountRate,
                                    String inventoryItemCode, String revenueAccount, boolean active) {
}

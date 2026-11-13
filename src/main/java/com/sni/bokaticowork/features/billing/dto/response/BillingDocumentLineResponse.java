package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingLineType;

import java.math.BigDecimal;

public record BillingDocumentLineResponse(
        Integer lineOrder,
        BillingLineType lineType,
        String itemCode,
        String category,
        String description,
        String detailedDescription,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        Boolean taxable,
        Boolean taxIncluded,
        BigDecimal vatRate,
        BigDecimal additionalCentRate,
        BigDecimal subtotalAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal additionalCentAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String sourceType,
        String sourceCode,
        String externalReference,
        String notes,
        Boolean optional,
        java.time.LocalDate serviceStart,
        java.time.LocalDate serviceEnd,
        /** Periode de service imprimee sur la facture : « du 15/10/2026 au 14/10/2027 », ou null. */
        String servicePeriod,
        /** Lot C1 : regle de taxe de l'article appliquee a la ligne (code de base), ou null. */
        String taxRuleCode
) {
    /** Sans regle de taxe d'article (pieces et tests anterieurs au lot C1). */
    public BillingDocumentLineResponse(Integer lineOrder, BillingLineType lineType, String itemCode, String category, String description,
                                       String detailedDescription, BigDecimal quantity, String unit, BigDecimal unitPrice, BigDecimal discountRate,
                                       BigDecimal discountAmount, Boolean taxable, Boolean taxIncluded, BigDecimal vatRate, BigDecimal additionalCentRate,
                                       BigDecimal subtotalAmount, BigDecimal taxableAmount, BigDecimal vatAmount, BigDecimal additionalCentAmount,
                                       BigDecimal taxAmount, BigDecimal totalAmount, String sourceType, String sourceCode, String externalReference,
                                       String notes, Boolean optional, java.time.LocalDate serviceStart, java.time.LocalDate serviceEnd,
                                       String servicePeriod) {
        this(lineOrder, lineType, itemCode, category, description, detailedDescription, quantity, unit, unitPrice, discountRate, discountAmount,
                taxable, taxIncluded, vatRate, additionalCentRate, subtotalAmount, taxableAmount, vatAmount, additionalCentAmount, taxAmount,
                totalAmount, sourceType, sourceCode, externalReference, notes, optional, serviceStart, serviceEnd, servicePeriod, null);
    }

    /** Sans periode de service (lignes ponctuelles, tests anterieurs au lot F20). */
    public BillingDocumentLineResponse(Integer lineOrder, BillingLineType lineType, String itemCode, String category, String description,
                                       String detailedDescription, BigDecimal quantity, String unit, BigDecimal unitPrice, BigDecimal discountRate,
                                       BigDecimal discountAmount, Boolean taxable, Boolean taxIncluded, BigDecimal vatRate, BigDecimal additionalCentRate,
                                       BigDecimal subtotalAmount, BigDecimal taxableAmount, BigDecimal vatAmount, BigDecimal additionalCentAmount,
                                       BigDecimal taxAmount, BigDecimal totalAmount, String sourceType, String sourceCode, String externalReference,
                                       String notes, Boolean optional) {
        this(lineOrder, lineType, itemCode, category, description, detailedDescription, quantity, unit, unitPrice, discountRate, discountAmount,
                taxable, taxIncluded, vatRate, additionalCentRate, subtotalAmount, taxableAmount, vatAmount, additionalCentAmount, taxAmount,
                totalAmount, sourceType, sourceCode, externalReference, notes, optional, null, null, null, null);
    }

    /** « du 15/10/2026 au 14/10/2027 », ou « le 10/01/2027 » pour un service d'un jour. */
    public static String period(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null) {
            return null;
        }
        java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (end == null || end.equals(start)) {
            return "le " + start.format(format);
        }
        return "du " + start.format(format) + " au " + end.format(format);
    }
}

package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BillingDocumentResponse(
        String documentNumber,
        BillingDocumentType documentType,
        BillingDocumentStatus status,
        String customerType,
        String customerCode,
        String customerName,
        String customerEmail,
        String customerPhone,
        String billingAddressJson,
        Boolean customerRegistered,
        String sourceType,
        String sourceCode,
        String resolvedSourceType,
        String resolvedSourceCode,
        String resolvedSourceLabel,
        Boolean resolvedSourceRegistered,
        String title,
        String description,
        String terms,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal additionalCentAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        /** Sous-total des lignes optionnelles non encore sélectionnées. */
        BigDecimal optionsTotal,
        LocalDate issueDate,
        LocalDate dueDate,
        Instant issuedAt,
        Instant sentAt,
        Instant paidAt,
        String customerReference,
        String poNumber,
        String projectCode,
        String salespersonCode,
        String deliveryAddressJson,
        String language,
        BigDecimal exchangeRate,
        String paymentReference,
        String paymentInstructions,
        String bankDetailsJson,
        String metadataJson,
        List<BillingDocumentLineResponse> lines,
        List<BillingDocumentDiscountResponse> discounts,
        List<BillingDocumentTaxResponse> taxes,
        List<BillingDocumentClauseResponse> clauses,
        BillingDocumentAdvanceResponse advance,
        EarlyPaymentDiscountResponse earlyPaymentDiscount,
        BillingDocumentSignatureResponse signature,
        String internalNotes,
        List<BillingRecoverableResponse> recoverables,
        // ── Champs SEFC ──────────────────────────────────────────────────────────
        Boolean locked,
        String fiscalNumber,
        LocalDate fiscalDate,
        Instant validatedAt,
        String sellerName,
        String sellerNiu,
        String sellerPhone,
        String sellerEmail,
        String customerNiu,
        String customerCategory,
        // ── Phase 2 : chaînage + signature ───────────────────────────────────────
        String previousHash,
        String currentHash,
        String fiscalSignature,
        Instant signedAt,
        // ── Lien document-correctif ────────────────────────────────────────────
        String originalDocumentNumber,
        String originalDocumentType,
        String creditNoteReason,
        // -- Ce que ce document pese sur le solde du client --------------------------
        /**
         * Ce document constitue-t-il une creance en cours ?
         *
         * <p>Faux pour un brouillon, un document annule, une facture deja reglee, et pour un
         * avoir. C'est le seul critere a utiliser pour decider si un montant est reclamable.</p>
         */
        Boolean receivable,
        /**
         * Ce que ce document change au solde du client · positif s'il doit, negatif si on lui doit.
         *
         * <p>Additionner ce champ sur une liste de documents donne le solde juste, quels que
         * soient les types et les etats presents. Additionner {@code balanceDue} ne le donne pas :
         * un brouillon et un avoir y portent un montant qui n'est pas une dette.</p>
         */
        BigDecimal customerImpact
) {
}

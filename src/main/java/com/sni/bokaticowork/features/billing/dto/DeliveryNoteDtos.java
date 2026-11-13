package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Lot C4 : requetes et reponses des bons de livraison. */
public final class DeliveryNoteDtos {

    private DeliveryNoteDtos() {
    }

    /** Quantite a livrer pour une ligne de la piece d'origine (son numero de ligne). */
    public record LineQuantity(Integer lineOrder, BigDecimal quantity) {
    }

    /**
     * Nouveau bon. Sans {@code lines}, tout le reste a livrer ; {@code showPrices} imprime les prix
     * sur le bon (sans prix par defaut).
     */
    public record CreateRequest(List<LineQuantity> lines, @Size(max = 2000) String deliveryAddress, @Size(max = 4000) String notes,
                                Boolean showPrices) {
    }

    public record DeliverRequest(@Size(max = 255) String receivedBy) {
    }

    public record CancelRequest(@Size(max = 500) String reason) {
    }

    public record SignedCopyRequest(@Size(max = 120) String reference) {
    }

    public record InvoiceRequest(@NotEmpty List<String> noteNumbers) {
    }

    public record NoteLine(Integer lineOrder, String itemCode, String description, String unit, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record NoteView(String noteNumber, String sourceDocumentNumber, String sourceDocumentType, String customerType, String customerCode,
                           String customerName, String status, String deliveryAddress, String notes, boolean showPrices, boolean stockIssued,
                           Instant deliveredAt, String deliveredBy, String receivedBy, String signedCopyReference, String invoiceNumber,
                           Instant cancelledAt, String cancelReason, String createdBy, Instant createdAt, List<NoteLine> lines) {
    }

    public record NoteSummary(String noteNumber, String sourceDocumentNumber, String sourceDocumentType, String customerName, String status,
                              Instant deliveredAt, String invoiceNumber, Instant createdAt) {
    }

    /**
     * Reste a livrer d'une ligne : {@code delivered} (bons livres ou factures), {@code inDraft}
     * (bons prepares, pas encore livres), {@code remaining} (ni l'un ni l'autre).
     */
    public record LineStatus(Integer lineOrder, String itemCode, String description, String unit, BigDecimal ordered, BigDecimal delivered,
                             BigDecimal inDraft, BigDecimal remaining) {
    }

    public record DeliveryStatus(String documentNumber, String documentType, boolean complete, List<LineStatus> lines,
                                 List<NoteSummary> notes) {
    }
}

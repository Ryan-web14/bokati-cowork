package com.sni.bokaticowork.features.marketplace.dto;

import com.sni.bokaticowork.features.marketplace.model.MarketplaceOffer;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * La marketplace · offres, panier et commandes.
 */
public final class MarketplaceDtos {

    private MarketplaceDtos() {
    }

    /**
     * Une offre nouvelle, non publiee · un article de stock et son emplacement, ou une prestation.
     *
     * @param price vide, le prix de l'article ou de la prestation
     */
    public record OfferRequest(@NotNull MarketplaceOffer.Kind kind,
                               String inventoryItemCode,
                               String locationCode,
                               String serviceItemCode,
                               @Size(max = 200) String title,
                               String description,
                               @Size(max = 500) String imageUrl,
                               @Size(max = 120) String category,
                               @PositiveOrZero BigDecimal price,
                               @Size(min = 3, max = 3) String currency,
                               @Positive Integer maxPerOrder,
                               Integer displayOrder) {
    }

    /**
     * Ne change que ce qui est donne · {@code resetPrice} rend a l'offre le prix de l'article ou de
     * la prestation. Ce que l'offre vend, et d'ou, ne change pas : une autre chose est une autre offre.
     */
    public record OfferUpdateRequest(@Size(max = 200) String title,
                                     String description,
                                     @Size(max = 500) String imageUrl,
                                     @Size(max = 120) String category,
                                     @PositiveOrZero BigDecimal price,
                                     Boolean resetPrice,
                                     @Positive Integer maxPerOrder,
                                     Boolean clearMaxPerOrder,
                                     Integer displayOrder) {
    }

    /**
     * @param price     le prix demande, celui de l'offre ou a defaut celui de l'article
     * @param ownPrice  le prix propre a l'offre · vide, elle suit l'article
     * @param available la quantite disponible a l'emplacement · vide pour une prestation
     */
    public record OfferView(Long id,
                            String offerCode,
                            MarketplaceOffer.Kind kind,
                            String title,
                            String description,
                            String imageUrl,
                            String category,
                            BigDecimal price,
                            BigDecimal ownPrice,
                            String currency,
                            BigDecimal available,
                            Integer maxPerOrder,
                            boolean published,
                            int displayOrder,
                            String inventoryItemCode,
                            String locationCode,
                            String serviceItemCode) {
    }

    public record CartLineRequest(@NotNull Long offerId, @NotNull @Min(1) Integer quantity) {
    }

    /** Zero retire la ligne. */
    public record CartQuantityRequest(@NotNull @Min(0) Integer quantity) {
    }

    /**
     * @param billingCompanyCode l'organisation qui paie · {@code NONE} le membre seul, vide celle de
     *                           son lien automatique s'il en a une
     */
    public record CheckoutRequest(String billingCompanyCode) {
    }

    /** @param collectedBy qui retire la commande, s'il n'est pas l'acheteur */
    public record FulfilRequest(@Size(max = 200) String collectedBy) {
    }

    /**
     * @param returnToStock une commande deja remise revient-elle en stock · une commande payee et
     *                      non remise y revient toujours
     */
    public record CancelRequest(@NotBlank @Size(max = 500) String reason, Boolean returnToStock) {
    }

    public record MemberCancelRequest(@Size(max = 500) String reason) {
    }

    public record LineView(Long id,
                           Long offerId,
                           MarketplaceOffer.Kind kind,
                           String title,
                           String imageUrl,
                           int quantity,
                           BigDecimal unitPrice,
                           BigDecimal amount,
                           BigDecimal available) {
    }

    /**
     * @param invoiceBalanceDue ce qu'il reste a regler sur la facture · vide pour un panier
     */
    public record OrderView(Long id,
                            String orderNumber,
                            MarketplaceOrder.Status status,
                            String buyerMemberCode,
                            String billingCompanyCode,
                            String currency,
                            BigDecimal totalAmount,
                            String invoiceNumber,
                            BigDecimal invoiceBalanceDue,
                            Instant expiresAt,
                            Instant placedAt,
                            Instant paidAt,
                            Instant fulfilledAt,
                            String fulfilledBy,
                            String collectedBy,
                            Instant cancelledAt,
                            String cancelledBy,
                            String cancellationReason,
                            String creditNoteNumber,
                            List<LineView> lines) {
    }
}

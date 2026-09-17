package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponResponse;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CreateCouponBatchRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponBatchService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponBatchService batchService;

    /**
     * Genere un lot de codes. Mille codes pour un partenariat se creent d'un appel, et se suivent
     * ensuite par leur lot plutot qu'un a un.
     */
    @PostMapping("/batches")
    public ResponseEntity<Map<String, Object>> createBatch(@Valid @RequestBody CreateCouponBatchRequest request,
                                                           @RequestParam(required = false) String createdBy) {
        CouponBatch batch = batchService.generate(
                request.promotionCode(),
                request.name(),
                request.quantity(),
                request.couponKind(),
                request.codePrefix(),
                request.codeLengthOrDefault(),
                request.maxRedemptionsPerCoupon(),
                request.validFrom(),
                request.validUntil(),
                request.channel(),
                createdBy);
        return ResponseEntity.ok(Map.of(
                "batchCode", batch.getBatchCode(),
                "generated", batch.getGeneratedQuantity(),
                "promotionCode", request.promotionCode()));
    }

    /**
     * Codes dont un abonne dispose et qu'il peut encore utiliser.
     *
     * <p>Un coupon nominatif n'apparait qu'a son titulaire · c'est la raison d'etre du ciblage, et
     * la requete le garantit plutot que de compter sur l'interface.</p>
     */
    @GetMapping("/available")
    public ResponseEntity<List<CouponResponse>> available(@RequestParam String subscriberType,
                                                          @RequestParam String subscriberCode) {
        return ResponseEntity.ok(couponService.availableFor(subscriberType, subscriberCode).stream()
                .map(CouponResponse::from)
                .toList());
    }

    /**
     * Retient un code pour un panier. Rien n'est consomme ici : la consommation a lieu au paiement,
     * et la retenue tombe d'elle-meme si le panier est abandonne.
     */
    @PostMapping("/{code}/reserve")
    public ResponseEntity<Map<String, Object>> reserve(@PathVariable String code,
                                                       @RequestParam String cartReference,
                                                       @RequestParam(required = false) String subscriberType,
                                                       @RequestParam(required = false) String subscriberCode,
                                                       @RequestParam(required = false) String reservedBy) {
        var reservation = couponService.reserve(code, cartReference, subscriberType, subscriberCode,
                null, null, reservedBy);
        return ResponseEntity.ok(Map.of(
                "reservationNumber", reservation.getReservationNumber(),
                "expiresAt", reservation.getExpiresAt().toString()));
    }

    /** Rend les codes retenus par un panier que le client a modifie ou abandonne. */
    @DeleteMapping("/reservations")
    public ResponseEntity<Map<String, Integer>> release(@RequestParam String cartReference,
                                                        @RequestParam(required = false) String reason) {
        int released = couponService.release(cartReference, reason == null ? "Panier modifié" : reason);
        return ResponseEntity.ok(Map.of("released", released));
    }

    @PostMapping("/{code}/revoke")
    public ResponseEntity<CouponResponse> revoke(@PathVariable String code,
                                                 @RequestParam String reason,
                                                 @RequestParam(required = false) String revokedBy) {
        return ResponseEntity.ok(CouponResponse.from(batchService.revoke(code, reason, revokedBy)));
    }
}

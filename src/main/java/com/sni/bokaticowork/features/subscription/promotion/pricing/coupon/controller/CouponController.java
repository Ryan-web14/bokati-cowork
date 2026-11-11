package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponBatchRevocation;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponBatchView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponLineView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CouponResponse;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto.CreateCouponBatchRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponBatchService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/coupons")
@RequiredArgsConstructor
public class CouponController {

    private static final int MAX_BATCH_PAGE = 100;
    private static final int MAX_COUPON_PAGE = 500;

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
        // Les trois cles d'origine restent pour les ecrans deja branches ; la vue complete s'y ajoute
        return ResponseEntity.ok(Map.of(
                "batchCode", batch.getBatchCode(),
                "generated", batch.getGeneratedQuantity(),
                "promotionCode", request.promotionCode(),
                "batch", batchService.view(batch.getBatchCode())));
    }

    /** Les lots, du plus recent au plus ancien, eventuellement ceux d'une seule promotion. */
    @GetMapping("/batches")
    public ResponseEntity<PaginatedResponse<CouponBatchView>> batches(@RequestParam(required = false) String promotionCode,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(batchService.list(promotionCode, pageOf(page, size, MAX_BATCH_PAGE))));
    }

    /** Un lot et ou en est sa campagne : codes par statut, utilisations et retenues en cours. */
    @GetMapping("/batches/{batchCode}")
    public ResponseEntity<CouponBatchView> batch(@PathVariable String batchCode) {
        return ResponseEntity.ok(batchService.view(batchCode));
    }

    /** Les codes d'un lot, page par page, par exemple ceux qui restent a distribuer. */
    @GetMapping("/batches/{batchCode}/coupons")
    public ResponseEntity<PaginatedResponse<CouponLineView>> batchCoupons(@PathVariable String batchCode,
                                                                          @RequestParam(required = false) CouponStatus status,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(
                batchService.coupons(batchCode, status, pageOf(page, size, MAX_COUPON_PAGE))));
    }

    /** Tous les codes du lot en CSV, a remettre au partenaire ou a l'imprimeur. */
    @GetMapping("/batches/{batchCode}/export")
    public ResponseEntity<String> exportBatch(@PathVariable String batchCode) {
        String csv = batchService.exportCsv(batchCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"coupons-" + fileSafe(batchCode) + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    /**
     * Revoque les codes encore utilisables du lot. Ceux qui ont deja servi ou deja ete revoques
     * sont laisses tels quels et comptes a part dans la reponse.
     */
    @PostMapping("/batches/{batchCode}/revoke")
    public ResponseEntity<CouponBatchRevocation> revokeBatch(@PathVariable String batchCode,
                                                             @RequestParam String reason,
                                                             @RequestParam(required = false) String revokedBy) {
        return ResponseEntity.ok(batchService.revokeBatch(batchCode, reason, revokedBy));
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

    /** Un code, avec sa promotion et son lot. Declare apres les chemins fixes, qui priment de toute facon. */
    @GetMapping("/{code}")
    public ResponseEntity<CouponResponse> coupon(@PathVariable String code) {
        return ResponseEntity.ok(couponService.view(code));
    }

    /** Une page bornee : un lot de cinquante mille codes ne se renvoie pas d'un bloc en JSON. */
    private static PageRequest pageOf(int page, int size, int max) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), max));
    }

    /** Le code du lot sert de nom de fichier ; rien qui puisse casser l'en-tete. */
    private static String fileSafe(String value) {
        return value == null ? "lot" : value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}

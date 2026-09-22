package com.sni.bokaticowork.features.subscription.promotion.pricing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.service.PricingRequestResolver;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.ClientPromotionView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationResponse;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.EffectivePrice;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PriceableRef;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.service.ClientPromotionService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.service.PricingSimulationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Le parcours client · voir les prix promotionnels, et poser un code avant de payer.
 *
 * <p>Deux principes gouvernent ce contrat.</p>
 *
 * <p><b>Rien n'est consomme avant le paiement.</b> Poser un code le retient, il ne le depense pas.
 * Un code a usage unique consomme des la saisie serait perdu si le panier etait abandonne, et le
 * client n'aurait plus rien a montrer.</p>
 *
 * <p><b>Un refus dit pourquoi.</b> En francais, jamais un code technique · un client qui comprend
 * pourquoi son code est refuse n'ecrit pas au support.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client")
@RequiredArgsConstructor
public class ClientPricingController {

    private final PricingEngine pricingEngine;
    private final PricingSimulationService simulationService;
    private final ClientPromotionService clientPromotionService;
    private final CouponService couponService;
    private final PricingRequestResolver requestResolver;
    private final ClientContextService clientContext;

    public record CatalogueRequest(
            @Valid PricingSimulationRequest context,
            @NotEmpty List<PriceableRef> items
    ) {
    }

    /**
     * Prix effectifs d'une liste d'objets, avec leur prix barre.
     *
     * <p>Toute liste de prix vue par un client doit passer par ici plutot que d'afficher le tarif
     * catalogue brut. Une promotion visible seulement au moment de payer ne vend rien · ce qui
     * declenche l'achat est le prix barre vu au moment de choisir.</p>
     */
    @PostMapping("/catalogue/prices")
    public ResponseEntity<List<EffectivePrice>> cataloguePrices(@Valid @RequestBody CatalogueRequest request) {
        return ResponseEntity.ok(pricingEngine.priceCatalogue(
                simulationService.toContext(forMember(request.context())), request.items()));
    }

    /**
     * Ce a quoi le client a droit maintenant, et ce a quoi il n'a pas droit, avec la raison.
     */
    @PostMapping("/promotions/available")
    public ResponseEntity<List<ClientPromotionView>> availablePromotions(
            @Valid @RequestBody PricingSimulationRequest request) {
        return ResponseEntity.ok(clientPromotionService.availableFor(simulationService.toContext(forMember(request))));
    }

    /**
     * Recalcule le panier sans rien engager.
     *
     * <p>Le client peut le consulter autant de fois qu'il veut : aucun budget n'est consomme, aucun
     * code n'est retenu.</p>
     */
    @PostMapping("/cart/preview")
    public ResponseEntity<PricingSimulationResponse> previewCart(@Valid @RequestBody PricingSimulationRequest request) {
        return ResponseEntity.ok(simulationService.simulate(forMember(request)));
    }

    /**
     * Pose un code sur le panier · il est retenu, pas depense.
     *
     * <p>La reponse rend le panier recalcule, avec le detail regle par regle : quelle campagne a
     * joue, pour quel montant. Un refus arrive avec son motif.</p>
     */
    @PostMapping("/cart/coupons")
    public ResponseEntity<PricingSimulationResponse> applyCoupon(@RequestParam String cartReference,
                                                                 @RequestParam String code,
                                                                 @Valid @RequestBody PricingSimulationRequest request) {
        PricingSimulationRequest resolved = forMember(request);
        var context = simulationService.toContext(resolved);
        // Le code est d'abord evalue sur le panier, puis retenu avec le montant annonce · retenir
        // avant d'evaluer immobiliserait un code qui ne s'applique meme pas.
        PricingSimulationResponse preview = simulationService.simulate(withCoupon(resolved, code));
        couponService.reserve(code, cartReference, context.subscriberType(), context.subscriberCode(),
                preview.discountTotal(), preview.currency(), context.subscriberCode());
        return ResponseEntity.ok(preview);
    }

    /** Retire un code du panier et rend la retenue. */
    @DeleteMapping("/cart/coupons/{code}")
    public ResponseEntity<Map<String, Integer>> removeCoupon(@PathVariable String code,
                                                             @RequestParam String cartReference) {
        return ResponseEntity.ok(Map.of(
                "released", couponService.release(cartReference, "Code retiré du panier " + code)));
    }

    /**
     * La demande telle que le serveur la comprend · l'abonne est celui de la session, la devise
     * vient du catalogue quand elle n'est pas donnee.
     */
    private PricingSimulationRequest forMember(PricingSimulationRequest request) {
        Member member = clientContext.getAuthenticatedMember();
        return requestResolver.resolveFor(request, SubscriberType.MEMBER.name(), member.getMemberId());
    }

    private PricingSimulationRequest withCoupon(PricingSimulationRequest request, String code) {
        List<String> codes = request.couponCodes() == null
                ? List.of(code)
                : java.util.stream.Stream.concat(request.couponCodes().stream(), java.util.stream.Stream.of(code))
                .distinct().toList();
        return new PricingSimulationRequest(
                request.subscriberType(), request.subscriberCode(), request.subscriberSegment(),
                request.kycLevel(), request.tenureMonths(), request.firstPurchase(), request.lines(),
                request.billingCycle(), request.channel(), request.paymentMethod(), request.locationCode(),
                request.currency(), codes, request.evaluationDate(), request.promotionsAllowed());
    }
}

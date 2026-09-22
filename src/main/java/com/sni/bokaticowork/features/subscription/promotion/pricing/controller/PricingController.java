package com.sni.bokaticowork.features.subscription.promotion.pricing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationResponse;
import com.sni.bokaticowork.features.subscription.promotion.pricing.service.PricingSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingSimulationService simulationService;
    private final com.sni.bokaticowork.features.subscription.promotion.pricing.service.PricingRequestResolver requestResolver;

    /**
     * Simule un panier sans aucun effet de bord. Aucun budget n'est consomme, aucun coupon n'est
     * reserve : le client peut regarder son panier autant de fois qu'il veut.
     */
    @PostMapping("/simulate")
    public ResponseEntity<PricingSimulationResponse> simulate(@Valid @RequestBody PricingSimulationRequest request) {
        return ResponseEntity.ok(simulationService.simulate(requestResolver.resolve(request)));
    }
}

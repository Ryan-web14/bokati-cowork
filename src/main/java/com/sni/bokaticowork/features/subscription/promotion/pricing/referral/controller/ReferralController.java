package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.Referral;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    public record RegisterReferralRequest(
            @NotBlank String linkCode,
            @NotBlank String refereeType,
            @NotBlank String refereeCode
    ) {
    }

    /** Rend le lien du parrain, en le creant au besoin. Un parrain n'en a qu'un par programme. */
    @GetMapping("/links")
    public ResponseEntity<Map<String, Object>> link(@RequestParam String programCode,
                                                    @RequestParam String referrerType,
                                                    @RequestParam String referrerCode) {
        ReferralLink link = referralService.linkFor(programCode, referrerType, referrerCode);
        return ResponseEntity.ok(Map.of(
                "code", link.getCode(),
                "clickCount", link.getClickCount(),
                "signupCount", link.getSignupCount()));
    }

    @PostMapping("/links/{linkCode}/clicks")
    public ResponseEntity<Void> click(@PathVariable String linkCode) {
        referralService.recordClick(linkCode);
        return ResponseEntity.noContent().build();
    }

    /**
     * Enregistre un parrainage. Rien n'est accorde a ce stade : un parrainage se qualifie, il ne se
     * declare pas.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterReferralRequest request) {
        Referral referral = referralService.register(
                request.linkCode(), request.refereeType(), request.refereeCode());
        return ResponseEntity.ok(Map.of(
                "referralNumber", referral.getReferralNumber(),
                "status", referral.getStatus().name()));
    }

    @PostMapping("/{referralNumber}/reject")
    public ResponseEntity<Map<String, String>> reject(@PathVariable String referralNumber,
                                                      @RequestParam String reason) {
        Referral referral = referralService.reject(referralNumber, reason);
        return ResponseEntity.ok(Map.of("status", referral.getStatus().name()));
    }
}

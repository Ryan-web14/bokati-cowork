package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.PublicLinkView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralProgramService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La page d'arrivee d'un lien de parrainage, sans connexion : la personne invitee voit le
 * programme et ce qu'elle y gagne avant de s'inscrire, et la visite est comptee.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/public/referrals")
@RequiredArgsConstructor
public class PublicReferralController {

    private final ReferralProgramService programService;
    private final ReferralService referralService;

    /** Toujours 200 : {@code valid} dit si le code peut servir a l'inscription, {@code reason} sinon pourquoi. */
    @GetMapping("/{code}")
    public ResponseEntity<PublicLinkView> link(@PathVariable String code) {
        return ResponseEntity.ok(programService.publicLink(code));
    }

    /** Compte une visite du lien ; un code inconnu est ignore. */
    @PostMapping("/{code}/clicks")
    public ResponseEntity<Void> click(@PathVariable String code) {
        referralService.recordClick(code);
        return ResponseEntity.noContent().build();
    }
}

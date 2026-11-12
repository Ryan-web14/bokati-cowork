package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ClientLinkRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.LinkView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.MyReferralView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.OpenProgramView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le parrainage dans l'espace client : le membre connecte voit les programmes ouverts, obtient son
 * lien a partager et suit ses parrainages. Il est toujours le parrain : le corps ne designe
 * personne d'autre.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/referrals")
@RequiredArgsConstructor
public class ClientReferralController {

    private final ReferralProgramService programService;
    private final ClientContextService clientContext;

    /** Les programmes ouverts, ce que gagnent le parrain et l'ami, et le lien du membre s'il l'a deja. */
    @GetMapping("/programs")
    public ResponseEntity<List<OpenProgramView>> programs() {
        return ResponseEntity.ok(programService.openPrograms(clientContext.getAuthenticatedMember()));
    }

    /** Le lien du membre sur un programme, cree au premier appel ; le meme ensuite. */
    @PostMapping("/links")
    public ResponseEntity<LinkView> link(@Valid @RequestBody ClientLinkRequest request) {
        return ResponseEntity.ok(programService.myLink(clientContext.getAuthenticatedMember(), request.programCode()));
    }

    @GetMapping("/links")
    public ResponseEntity<List<LinkView>> links() {
        return ResponseEntity.ok(programService.myLinks(clientContext.getAuthenticatedMember()));
    }

    /** Les amis parraines, leur avancement et la recompense du membre. */
    @GetMapping
    public ResponseEntity<List<MyReferralView>> referrals() {
        return ResponseEntity.ok(programService.myReferrals(clientContext.getAuthenticatedMember()));
    }
}

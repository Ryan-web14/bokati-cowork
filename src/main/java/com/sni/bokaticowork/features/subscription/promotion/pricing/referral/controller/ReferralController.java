package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.Referral;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.LinkRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.LinkView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ProgramRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ProgramView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ReferralView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralProgramService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;
    private final ReferralProgramService programService;

    public record RegisterReferralRequest(
            @NotBlank String linkCode,
            @NotBlank String refereeType,
            @NotBlank String refereeCode
    ) {
    }

    // Programmes ---------------------------------------------------------------------------------

    @PostMapping("/programs")
    public ResponseEntity<ProgramView> createProgram(@Valid @RequestBody ProgramRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programService.create(request, actor(authentication)));
    }

    @GetMapping("/programs")
    public ResponseEntity<List<ProgramView>> programs() {
        return ResponseEntity.ok(programService.programs());
    }

    @GetMapping("/programs/{code}")
    public ResponseEntity<ProgramView> program(@PathVariable String code) {
        return ResponseEntity.ok(programService.get(code));
    }

    /** Le code ne change pas ; regle et recompenses seulement tant que le programme est en brouillon. */
    @PutMapping("/programs/{code}")
    public ResponseEntity<ProgramView> updateProgram(@PathVariable String code, @Valid @RequestBody ProgramRequest request) {
        return ResponseEntity.ok(programService.update(code, request));
    }

    @PostMapping("/programs/{code}/activate")
    public ResponseEntity<ProgramView> activate(@PathVariable String code) {
        return ResponseEntity.ok(programService.activate(code));
    }

    @PostMapping("/programs/{code}/pause")
    public ResponseEntity<ProgramView> pause(@PathVariable String code) {
        return ResponseEntity.ok(programService.pause(code));
    }

    @PostMapping("/programs/{code}/end")
    public ResponseEntity<ProgramView> end(@PathVariable String code) {
        return ResponseEntity.ok(programService.end(code));
    }

    @GetMapping("/programs/{code}/links")
    public ResponseEntity<PaginatedResponse<LinkView>> programLinks(@PathVariable String code,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(programService.links(code, page, size));
    }

    // Liens --------------------------------------------------------------------------------------

    /** Cree le lien d'un parrain (ou rend celui qu'il a deja) : un seul par parrain et par programme. */
    @PostMapping("/links")
    public ResponseEntity<LinkView> createLink(@Valid @RequestBody LinkRequest request) {
        return ResponseEntity.ok(programService.link(request.programCode(), request.referrerType(), request.referrerCode()));
    }

    /** Ancienne forme de {@code POST /links}, gardee pour les ecrans existants. */
    @GetMapping("/links")
    public ResponseEntity<LinkView> link(@RequestParam String programCode,
                                         @RequestParam String referrerType,
                                         @RequestParam String referrerCode) {
        return ResponseEntity.ok(programService.link(programCode, referrerType, referrerCode));
    }

    /** Coupe un lien (abus) ; les parrainages deja enregistres suivent leur cours. */
    @PostMapping("/links/{linkCode}/deactivate")
    public ResponseEntity<LinkView> deactivateLink(@PathVariable String linkCode) {
        return ResponseEntity.ok(programService.setLinkActive(linkCode, false));
    }

    @PostMapping("/links/{linkCode}/activate")
    public ResponseEntity<LinkView> activateLink(@PathVariable String linkCode) {
        return ResponseEntity.ok(programService.setLinkActive(linkCode, true));
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

    // Parrainages ---------------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<PaginatedResponse<ReferralView>> referrals(@RequestParam(required = false) String programCode,
                                                                     @RequestParam(required = false) ReferralStatus status,
                                                                     @RequestParam(required = false) String referrerCode,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(programService.referrals(programCode, status, referrerCode, page, size));
    }

    @GetMapping("/{referralNumber}")
    public ResponseEntity<ReferralView> referral(@PathVariable String referralNumber) {
        return ResponseEntity.ok(programService.referral(referralNumber));
    }

    @PostMapping("/{referralNumber}/reject")
    public ResponseEntity<Map<String, String>> reject(@PathVariable String referralNumber,
                                                      @RequestParam String reason) {
        Referral referral = referralService.reject(referralNumber, reason);
        return ResponseEntity.ok(Map.of("status", referral.getStatus().name()));
    }

    private static String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "STAFF" : authentication.getName();
    }
}

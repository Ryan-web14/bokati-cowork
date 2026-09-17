package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service.PromotionBeneficiaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gestion de la liste nominative d'une promotion.
 *
 * <p>Aucun point d'entree ne rend la liste a un client : ces operations sont administratives. Une
 * promotion nominative ne se decouvre que par le panier de son beneficiaire, jamais par une
 * consultation.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/promotions/{promotionCode}/beneficiaries")
@RequiredArgsConstructor
public class PromotionBeneficiaryController {

    private final PromotionBeneficiaryService beneficiaryService;

    public record AddBeneficiaryRequest(
            @NotBlank String subscriberType,
            @NotBlank String subscriberCode,
            String reason
    ) {
    }

    public record ImportBeneficiariesRequest(
            @NotEmpty List<@Valid AddBeneficiaryRequest> beneficiaries,
            String reason
    ) {
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> add(@PathVariable String promotionCode,
                                                   @Valid @RequestBody AddBeneficiaryRequest request,
                                                   @RequestParam(required = false) String addedBy) {
        var beneficiary = beneficiaryService.add(promotionCode, request.subscriberType(),
                request.subscriberCode(), request.reason(), addedBy);
        return ResponseEntity.ok(Map.of(
                "subscriberCode", beneficiary.getSubscriberCode(),
                "addedAt", beneficiary.getAddedAt().toString()));
    }

    /** Import de masse, avec rapport ligne a ligne : lignes acceptees, lignes rejetees et pourquoi. */
    @PostMapping("/import")
    public ResponseEntity<PromotionBeneficiaryService.ImportReport> importAll(
            @PathVariable String promotionCode,
            @Valid @RequestBody ImportBeneficiariesRequest request,
            @RequestParam(required = false) String addedBy) {
        List<String[]> rows = request.beneficiaries().stream()
                .map(entry -> new String[]{entry.subscriberType(), entry.subscriberCode()})
                .toList();
        return ResponseEntity.ok(beneficiaryService.importAll(promotionCode, rows, request.reason(), addedBy));
    }

    @DeleteMapping
    public ResponseEntity<Void> revoke(@PathVariable String promotionCode,
                                       @RequestParam String subscriberType,
                                       @RequestParam String subscriberCode,
                                       @RequestParam String reason,
                                       @RequestParam(required = false) String revokedBy) {
        beneficiaryService.revoke(promotionCode, subscriberType, subscriberCode, reason, revokedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notified")
    public ResponseEntity<Void> markNotified(@PathVariable String promotionCode,
                                             @RequestParam String subscriberType,
                                             @RequestParam String subscriberCode,
                                             @RequestParam String channel) {
        beneficiaryService.markNotified(promotionCode, subscriberType, subscriberCode, channel);
        return ResponseEntity.noContent().build();
    }

    /** Taux d'utilisation · ce que le partenariat ou le geste commercial a reellement produit. */
    @GetMapping("/usage")
    public ResponseEntity<PromotionBeneficiaryService.Usage> usage(@PathVariable String promotionCode) {
        return ResponseEntity.ok(beneficiaryService.usage(promotionCode));
    }
}

package com.sni.bokaticowork.features.subscription.subscription.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionBillingLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Rattachement d'une facture ou d'un paiement deja faits a un abonnement, et sa validation
 * (activation) quand la facture est soldee. La route est sous {@code /activate} : elle demande le
 * droit d'activer un abonnement.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
public class SubscriptionBillingLinkController {

    private final SubscriptionBillingLinkService linkService;

    /**
     * @param invoiceNumber facture existante (ou seulement le paiement, s'il regle une seule facture)
     * @param activate      valider l'abonnement si la facture est soldee (vrai par defaut)
     * @param force         administrateur seulement : valider meme si la facture n'est pas soldee
     */
    public record LinkRequest(String invoiceNumber, String paymentNumber, Boolean activate, Boolean force) {
    }

    @PostMapping("/{subscriptionNumber}/activate-with-billing")
    public ResponseEntity<SubscriptionBillingLinkService.LinkResult> link(@PathVariable String subscriptionNumber, @RequestBody LinkRequest request,
                                                                          Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_SUPER_ADMIN") || a.equals("ADMIN") || a.equals("SUPER_ADMIN"));
        String actor = authentication == null || authentication.getName() == null ? "STAFF" : authentication.getName();
        return ResponseEntity.ok(linkService.linkAndValidate(subscriptionNumber, request.invoiceNumber(), request.paymentNumber(),
                request.activate() == null || request.activate(), Boolean.TRUE.equals(request.force()), admin, actor));
    }
}

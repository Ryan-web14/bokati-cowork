package com.sni.bokaticowork.features.payment.integrity.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.integrity.service.WalletIntegrityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verification d'integrite d'un portefeuille, a la demande.
 *
 * <p>Reserve a l'administration : le rapport dit ou la chaine se rompt, ce qui est exactement ce
 * qu'il faudrait savoir pour la reparer proprement apres l'avoir rompue.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/wallets")
@RequiredArgsConstructor
public class WalletIntegrityController {

    private final WalletIntegrityService integrityService;

    @GetMapping("/{walletNumber}/integrity")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<WalletIntegrityService.IntegrityReport> verify(@PathVariable String walletNumber) {
        return ResponseEntity.ok(integrityService.verify(walletNumber));
    }
}

package com.sni.bokaticowork.features.payment.security.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.limit.service.WalletKycLevelResolver;
import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le code secret vu du guichet.
 *
 * <p>Une seule action y est offerte, et c'est deliberement la seule possible : <b>reinitialiser</b>.
 * Aucun point d'entree ne permet de lire un code ni d'en poser un a la place du titulaire · un
 * administrateur qui pourrait choisir le code de quelqu'un pourrait s'en servir, et le titulaire
 * n'aurait aucun moyen de le savoir.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/wallets")
@RequiredArgsConstructor
public class WalletSecurityAdminController {

    private final WalletAccountRepository walletRepository;
    private final WalletSecurityService securityService;
    private final WalletLimitService limitService;
    private final WalletKycLevelResolver kycLevelResolver;

    /**
     * Retire le code · le titulaire en posera un nouveau a sa prochaine operation.
     *
     * <p>Leve aussi le blocage : quelqu'un qui appelle parce qu'il s'est bloque doit repartir avec
     * un portefeuille utilisable, pas avec l'attente d'une temporisation.</p>
     */
    @PostMapping("/{walletNumber}/security/pin/reset")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> resetPin(@PathVariable String walletNumber, Authentication authentication) {
        WalletAccount wallet = wallet(walletNumber);
        securityService.resetPin(wallet, authentication == null ? "ADMIN" : authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** Plafonds applicables et consommation du jour · ce que le guichet doit pouvoir expliquer. */
    @GetMapping("/{walletNumber}/limits")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<WalletLimitService.LimitSnapshot> limits(@PathVariable String walletNumber) {
        WalletAccount wallet = wallet(walletNumber);
        return ResponseEntity.ok(limitService.snapshot(wallet, kycLevelResolver.levelOf(wallet)));
    }

    private WalletAccount wallet(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
    }
}

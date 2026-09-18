package com.sni.bokaticowork.features.payment.security.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Ce qu'un paiement du titulaire vers l'etablissement doit verifier.
 *
 * <p>Moins qu'un transfert, et c'est normal : il y a une facture en face, et une prestation que
 * l'annulation peut rattraper. Le code n'est exige que si la politique le demande pour ce type
 * d'operation · par defaut, elle ne le demande pas.</p>
 *
 * <p>Le verrou du titulaire, lui, s'applique toujours. Un portefeuille verrouille ne paie rien,
 * meme une facture : c'est le titulaire qui l'a ferme, et il l'a fait parce qu'il ne voulait pas
 * qu'on y touche.</p>
 */
@Component
@RequiredArgsConstructor
public class WalletMerchantPaymentGuard {

    private final WalletSecurityService securityService;

    /**
     * @param operation {@code MERCHANT_PAYMENT} pour un achat, {@code BILL_PAYMENT} pour une facture d'abonnement
     * @param pin       le code saisi, ou nul si l'interface ne l'a pas demande
     */
    public void assertAllowed(WalletAccount wallet, WalletOperationType operation, BigDecimal amount, String pin) {
        if (wallet.getLockedByOwnerAt() != null) {
            throw new ConflictException("wallet", "votre portefeuille est verrouillé · déverrouillez-le pour payer");
        }
        if (!wallet.spendable()) {
            throw new ConflictException("wallet", "votre portefeuille ne peut pas payer pour le moment");
        }
        WalletSecurityService.SecurityVerdict verdict = securityService.evaluate(wallet, operation, amount);
        if (!verdict.pinRequired()) {
            return;
        }
        if (verdict.pinMissing()) {
            throw new BadRequestException(verdict.reason());
        }
        if (pin == null || pin.isBlank()) {
            // L'interface n'a pas demande le code · on le dit clairement pour qu'elle le demande,
            // plutot que de repondre « code incorrect » a un code jamais saisi.
            throw new BadRequestException("Ce paiement exige votre code secret");
        }
        securityService.verifyPin(wallet, pin);
    }
}

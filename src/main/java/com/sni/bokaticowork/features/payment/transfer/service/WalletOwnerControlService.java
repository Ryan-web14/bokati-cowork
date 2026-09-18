package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.security.repository.WalletCredentialRepository;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Ce que le titulaire decide seul de son portefeuille.
 *
 * <p>Le verrouillage est asymetrique, et c'est voulu. <b>Verrouiller</b> ne demande rien : quelqu'un
 * qui vient de perdre son telephone doit pouvoir fermer son portefeuille a la minute, depuis
 * n'importe quel navigateur, sans chercher un code. <b>Deverrouiller</b> exige le code s'il en
 * existe un : c'est le sens qui interesse un compte vole.</p>
 *
 * <p>Le verrou du titulaire n'est pas le gel de l'etablissement. Un portefeuille verrouille recoit
 * encore, et son titulaire le rouvre seul. Un portefeuille gele ne fait ni l'un ni l'autre.</p>
 */
@Service
@RequiredArgsConstructor
public class WalletOwnerControlService {

    private final WalletAccountRepository walletRepository;
    private final WalletCredentialRepository credentialRepository;
    private final WalletSecurityService securityService;
    private final WalletNotifier notifier;

    @Transactional
    public WalletAccount lock(WalletAccount wallet, String ipAddress, String deviceId) {
        WalletAccount managed = walletRepository.findByIdForUpdate(wallet.getId()).orElseThrow();
        if (managed.getLockedByOwnerAt() == null) {
            managed.setLockedByOwnerAt(java.time.Instant.now());
            walletRepository.save(managed);
            notifier.securityEvent(managed, "WALLET_LOCKED_BY_OWNER",
                    "Votre portefeuille a été verrouillé",
                    Map.of("ipAddress", nullSafe(ipAddress), "deviceId", nullSafe(deviceId)));
        }
        return managed;
    }

    @Transactional
    public WalletAccount unlock(WalletAccount wallet, String pin, String ipAddress, String deviceId) {
        if (wallet.getLockedByOwnerAt() == null) {
            return wallet;
        }
        // Le code est exige des qu'il existe. Un titulaire sans code a accepte ce niveau de
        // protection · on ne lui invente pas une exigence qu'il ne peut pas satisfaire.
        // Verifie avant le verrou : un blocage pour codes faux leve un signalement, et ce
        // signalement ne peut pas s'inserer tant que le compte est verrouille en exclusif.
        if (credentialRepository.findByWalletId(wallet.getId()).isPresent()) {
            securityService.verifyPin(wallet, pin);
        }
        WalletAccount managed = walletRepository.findByIdForUpdate(wallet.getId()).orElseThrow();
        if (managed.getLockedByOwnerAt() == null) {
            return managed;
        }
        managed.setLockedByOwnerAt(null);
        walletRepository.save(managed);
        notifier.securityEvent(managed, "WALLET_UNLOCKED_BY_OWNER",
                "Votre portefeuille a été déverrouillé",
                Map.of("ipAddress", nullSafe(ipAddress), "deviceId", nullSafe(deviceId)));
        return managed;
    }

    /**
     * Preferences de notification et seuil d'alerte.
     *
     * <p>Un seuil nul retire l'alerte. Un seuil a zero n'a pas de sens · le solde ne descend pas
     * en dessous · et serait une facon deguisee de la retirer ; on le refuse pour que l'intention
     * soit explicite.</p>
     */
    @Transactional
    public WalletAccount updatePreferences(WalletAccount wallet, BigDecimal lowBalanceThreshold,
                                           Boolean notifyOnCredit, Boolean notifyOnDebit) {
        WalletAccount managed = walletRepository.findByIdForUpdate(wallet.getId()).orElseThrow();
        if (lowBalanceThreshold != null && lowBalanceThreshold.signum() <= 0) {
            throw new BadRequestException("Le seuil d'alerte doit être positif, ou absent pour ne plus être alerté");
        }
        managed.setLowBalanceThreshold(lowBalanceThreshold);
        if (lowBalanceThreshold == null
                || (managed.getAvailableBalance() != null && managed.getAvailableBalance().compareTo(lowBalanceThreshold) >= 0)) {
            // Nouveau seuil, nouvelle chance d'alerter · l'ancienne alerte ne compte plus.
            managed.setLowBalanceAlertedAt(null);
        }
        if (notifyOnCredit != null) {
            managed.setNotifyOnCredit(notifyOnCredit);
        }
        if (notifyOnDebit != null) {
            managed.setNotifyOnDebit(notifyOnDebit);
        }
        return walletRepository.save(managed);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

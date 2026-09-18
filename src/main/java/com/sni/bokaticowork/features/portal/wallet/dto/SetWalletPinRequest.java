package com.sni.bokaticowork.features.portal.wallet.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Pose ou remplace le code secret.
 *
 * <p>{@code currentPin} est vide a la premiere pose, et apres une reinitialisation administrative ·
 * dans ce cas le titulaire n'a plus d'ancien code a fournir, et c'est precisement le but. Le
 * service tranche, pas le contrat : exiger l'ancien ici rendrait la reinitialisation inutilisable.</p>
 */
public record SetWalletPinRequest(
        String currentPin,
        @NotBlank(message = "Le nouveau code secret est requis") String newPin
) {
}

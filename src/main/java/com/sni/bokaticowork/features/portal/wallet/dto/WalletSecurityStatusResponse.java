package com.sni.bokaticowork.features.portal.wallet.dto;

import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;

import java.util.List;

/**
 * Ce que le titulaire doit savoir de la securite de son portefeuille.
 *
 * <p>Tout y est dit avant l'operation plutot qu'au moment du refus : les operations qui exigeront
 * son code, s'il en a un, ses plafonds et ce qu'il en reste. Une interface qui dispose de cela peut
 * prevenir ; une interface qui ne l'a pas ne peut que constater.</p>
 */
public record WalletSecurityStatusResponse(
        String walletNumber,
        boolean pinDefined,
        boolean pinMustChange,
        boolean pinExpired,
        boolean locked,
        Long lockedForMinutes,
        int pinLength,
        List<String> operationsRequiringPin,
        WalletLimitService.LimitSnapshot limits
) {
}

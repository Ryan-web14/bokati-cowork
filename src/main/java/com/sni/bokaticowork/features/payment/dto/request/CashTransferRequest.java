package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Envoi d'un transfert interne. La destination est deduite (caisse mere) si elle est absente ; le
 * montant peut venir d'un billetage.
 */
public record CashTransferRequest(
        @NotBlank String fromRegisterCode,
        String toRegisterCode,
        BigDecimal amount,
        @Valid CashCountRequest count,
        String carrierEmail,
        String note
) {
}

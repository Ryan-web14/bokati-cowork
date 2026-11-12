package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Reception d'un transfert : le destinataire compte ce qu'il recoit ; un ecart doit etre explique. */
public record CashTransferReceiveRequest(@NotNull @Valid CashCountRequest count, String reason) {
}

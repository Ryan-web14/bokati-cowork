package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Echange de monnaie : coupures donnees et recues, de meme total. */
public record CashExchangeRequest(
        @NotEmpty List<CashCountRequest.Line> given,
        @NotEmpty List<CashCountRequest.Line> received,
        String note
) {
}

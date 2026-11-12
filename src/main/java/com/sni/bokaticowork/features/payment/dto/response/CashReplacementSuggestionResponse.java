package com.sni.bokaticowork.features.payment.dto.response;

/** Remplacant possible, dans l'ordre de preference. */
public record CashReplacementSuggestionResponse(String userEmail, String origin, Integer rank, String registerCode) {
}

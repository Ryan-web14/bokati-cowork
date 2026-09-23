package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MobileMoneyDepositResponse(
        String depositId,
        String clientReferenceId,
        String customerMessage,
        List<Map<String, Object>> metadata,
        PayerResponse payer,
        String provider,
        BigDecimal amount,
        String currency,
        String status,
        /**
         * Ou en est le depot, pour l'ecran · {@code WAITING_FOR_PAYER} la demande est sur le
         * telephone, {@code CONFIRMING} on attend la reponse de l'operateur, {@code COMPLETED},
         * {@code FAILED}, {@code UNRESOLVED} sans reponse definitive.
         */
        String phase,
        String providerMessage,
        String failureReason,
        /** Le code d'echec de l'operateur · ce qui permet de compter par cause. */
        String failureCode,
        /** Ce qu'on dit au client, en francais · jamais un code technique. */
        String userMessage,
        /** Reessayer avec le meme numero a-t-il un sens ? */
        Boolean retryable,
        /** Prochaine verification aupres de l'operateur · nulle quand il n'y a plus rien a attendre. */
        Instant nextStatusCheckAt,
        String intentNumber,
        String transactionNumber,
        String customerType,
        String customerCode,
        String callbackUrl,
        String requestPayloadJson,
        String providerResponseJson,
        Instant lastStatusCheckedAt,
        Instant completedAt,
        Instant failedAt,
        Instant createdAt,
        Instant updatedAt,
        PaymentTransactionResponse transaction
) {
    public record PayerResponse(
            String type,
            AccountDetailsResponse accountDetails
    ) {
    }

    public record AccountDetailsResponse(
            String phoneNumber,
            String provider
    ) {
    }
}

package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record EntitlementOperationRequest(
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,
        @NotBlank String entitlementCode,
        @NotNull BigDecimal quantity,
        String referenceType,
        String referenceId,
        String idempotencyKey,
        String reason,
        /**
         * Jusqu'a quand une retenue reste acquise · vide, quinze minutes, le temps d'un paiement.
         *
         * <p>Une reservation confirmee pour dans trois jours retenait son droit quinze minutes : il
         * revenait ensuite au solde, le membre pouvait le depenser ailleurs, et la cloture echouait
         * faute de solde.</p>
         */
        Instant holdUntil,
        /** Qui profite du droit quand il n'est pas au titulaire · le collaborateur d'une organisation. */
        String beneficiaryType,
        String beneficiaryCode,
        /**
         * Limite l'operation aux droits d'un abonnement ({@code SUBSCRIPTION}) ou d'un pass
         * ({@code PASS}) · vide, tous les droits du titulaire sur ce code.
         *
         * <p>Une organisation peut detenir plusieurs abonnements et passes portant le meme droit :
         * la place d'un collaborateur, ou son acces a un pass, ne vaut que sur celui-la.</p>
         */
        String scopeType,
        String scopeNumber
) {
    public static final String SCOPE_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String SCOPE_PASS = "PASS";

    public EntitlementOperationRequest(SubscriberType ownerType, String ownerCode, String entitlementCode,
                                       BigDecimal quantity, String referenceType, String referenceId,
                                       String idempotencyKey, String reason) {
        this(ownerType, ownerCode, entitlementCode, quantity, referenceType, referenceId, idempotencyKey, reason, null);
    }

    public EntitlementOperationRequest(SubscriberType ownerType, String ownerCode, String entitlementCode,
                                       BigDecimal quantity, String referenceType, String referenceId,
                                       String idempotencyKey, String reason, Instant holdUntil) {
        this(ownerType, ownerCode, entitlementCode, quantity, referenceType, referenceId, idempotencyKey, reason, holdUntil,
                null, null, null, null);
    }
}

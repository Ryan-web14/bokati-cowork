package com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement;

import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une operation ciblee ne touche que les droits de l'abonnement, ou du pass, qu'elle vise.
 */
class EntitlementScopeTest {

    private static EntitlementGrant subscriptionGrant(String number) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionNumber(number);
        return EntitlementGrant.builder().subscription(subscription).build();
    }

    private static EntitlementGrant passGrant(String number, Subscription parent) {
        Pass pass = new Pass();
        pass.setPassNumber(number);
        return EntitlementGrant.builder().pass(pass).subscription(parent).build();
    }

    @Test
    @DisplayName("Abonnement vise : ses droits seulement, pas ceux d'un autre abonnement ni d'un pass")
    void subscriptionScope() {
        String scope = EntitlementOperationRequest.SCOPE_SUBSCRIPTION;
        assertThat(EntitlementBalanceReader.inScope(subscriptionGrant("SUB-ACME"), scope, "SUB-ACME")).isTrue();
        assertThat(EntitlementBalanceReader.inScope(subscriptionGrant("SUB-AUTRE"), scope, "SUB-ACME")).isFalse();
        Subscription parent = new Subscription();
        parent.setSubscriptionNumber("SUB-ACME");
        assertThat(EntitlementBalanceReader.inScope(passGrant("PAS-1", parent), scope, "SUB-ACME")).isFalse();
    }

    @Test
    @DisplayName("Pass vise : ses droits seulement")
    void passScope() {
        String scope = EntitlementOperationRequest.SCOPE_PASS;
        assertThat(EntitlementBalanceReader.inScope(passGrant("PAS-1", null), scope, "pas-1")).isTrue();
        assertThat(EntitlementBalanceReader.inScope(passGrant("PAS-2", null), scope, "PAS-1")).isFalse();
        assertThat(EntitlementBalanceReader.inScope(subscriptionGrant("SUB-ACME"), scope, "PAS-1")).isFalse();
    }
}

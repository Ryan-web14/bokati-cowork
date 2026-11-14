package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.util.List;

/** Acces de test aux variables du modele abonnement_actif. */
public final class SubscriptionWhatsAppNotifierProbe {

    private SubscriptionWhatsAppNotifierProbe() {
    }

    public static List<String> values(Subscription subscription, String name) {
        return SubscriptionWhatsAppNotifier.values(subscription, name);
    }
}

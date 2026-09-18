package com.sni.bokaticowork.features.subscription.subscription.pass.enums;

/**
 * Par ou la validation est arrivee.
 *
 * <p>Le canal ne change jamais la decision · c'est precisement le point. Il est conserve pour
 * savoir d'ou viennent les usages et pour enqueter sur un refus conteste.</p>
 */
public enum PassValidationChannel {
    QR_SCAN,
    MANUAL,
    KIOSK,
    API
}

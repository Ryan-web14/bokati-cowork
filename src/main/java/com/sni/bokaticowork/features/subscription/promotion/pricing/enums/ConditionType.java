package com.sni.bokaticowork.features.subscription.promotion.pricing.enums;

/**
 * Ce qu'une condition peut interroger.
 *
 * <p>La liste est volontairement longue et fermee. Une condition exprimee en donnees se regle sans
 * deploiement ; une condition exprimee en code fait de chaque idee commerciale une livraison.</p>
 */
public enum ConditionType {
    SUBSCRIBER_TYPE,
    SUBSCRIBER_SEGMENT,
    FIRST_PURCHASE,
    PLAN_CODE,
    PLAN_TYPE,
    PASS_TYPE,
    BILLING_CYCLE,
    MIN_AMOUNT,
    MIN_QUANTITY,
    CHANNEL,
    PAYMENT_METHOD,
    KYC_LEVEL,
    DAY_OF_WEEK,
    TIME_RANGE,
    LOCATION_CODE,
    TENURE_MONTHS,
    REFERRAL_CODE
}

package com.sni.bokaticowork.features.subscription.promotion.pricing.enums;

/**
 * Sur quoi porte une recompense.
 *
 * <p>La portee couvre plusieurs modules a dessein. Le moteur ne sait rien du module d'origine : il
 * manipule un objet tarifable designe par un couple portee et code. C'est ce qui evite d'ecrire un
 * moteur de promotion par module.</p>
 */
public enum TargetScope {
    WHOLE_ORDER,
    LINE,
    PLAN,
    ADDON,
    PASS,
    ENTITLEMENT,
    SERVICE,
    RESOURCE,
    INVENTORY_ITEM,
    CATEGORY
}

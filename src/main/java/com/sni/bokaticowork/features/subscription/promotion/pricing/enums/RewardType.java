package com.sni.bokaticowork.features.subscription.promotion.pricing.enums;

/**
 * Ce qu'une promotion accorde.
 *
 * <p>Trois de ces types etaient declares dans {@code DiscountType} sans aucun traitement, et
 * {@code FREE_TRIAL_DAYS} etait meme explicitement refuse a la creation. Ils trouvent ici leur
 * place, avec la distinction qui manquait : certains reduisent un montant, d'autres accordent
 * quelque chose qui ne se soustrait pas d'une ligne.</p>
 */
public enum RewardType {

    PERCENTAGE_OFF(true),
    FIXED_AMOUNT_OFF(true),
    WAIVE_SETUP_FEE(true),

    /** Accorde autre chose qu'une reduction de montant · periodes, droits, credit. */
    FREE_PERIODS(false),
    FREE_TRIAL_DAYS(false),
    FREE_ENTITLEMENT(false),
    EXTRA_ENTITLEMENT_UNITS(false),
    FREE_ADDON(false),
    UPGRADE_TIER(false),
    WALLET_CREDIT(false);

    private final boolean monetary;

    RewardType(boolean monetary) {
        this.monetary = monetary;
    }

    /**
     * Vrai lorsque la recompense se traduit par un montant retire du prix. Les autres se
     * materialisent ailleurs, et les confondre produirait une facture fausse.
     */
    public boolean isMonetary() {
        return monetary;
    }
}

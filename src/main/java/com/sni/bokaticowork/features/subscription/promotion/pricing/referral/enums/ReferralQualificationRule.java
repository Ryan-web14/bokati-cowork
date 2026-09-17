package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums;

/**
 * Ce qui declenche reellement la recompense.
 *
 * <p>Le choix n'est pas anodin. Un programme qui paie a l'inscription paie pour des comptes crees et
 * jamais utilises, et c'est exactement ce qu'il attire. Payer au premier reglement aligne la
 * recompense sur ce que le parrainage etait cense produire.</p>
 */
public enum ReferralQualificationRule {

    /** Des l'inscription du filleul. Le plus genereux, et le plus expose. */
    SIGNUP,

    /** Au premier reglement du filleul. Valeur par defaut, et la seule qui aligne cout et benefice. */
    FIRST_PAYMENT,

    /** Apres une anciennete donnee, exprimee par {@code qualificationDelayDays}. */
    TENURE_REACHED
}

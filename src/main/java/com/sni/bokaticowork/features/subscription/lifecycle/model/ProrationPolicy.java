package com.sni.bokaticowork.features.subscription.lifecycle.model;

/**
 * Comment on compte une periode entamee · le meme choix pour l'entree, la sortie et le changement.
 *
 * <p>Trois politiques et pas une de plus : au jour pres, au mois entame (tout mois commence est du),
 * ou aucun prorata (la periode entiere est due, rien n'est rendu).</p>
 */
public enum ProrationPolicy {
    DAILY, MONTH_STARTED, NONE
}

package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Methode de valorisation du stock d'un article.
 *
 * <p>Choisie par article, avec repli sur la categorie puis sur {@link #WEIGHTED_AVERAGE}. Le defaut
 * reproduit exactement le comportement historique du module : un article qui ne declare rien
 * continue de fonctionner comme avant.</p>
 */
public enum ValuationMethod {

    /**
     * Cout moyen pondere. Un seul cout par couple article et emplacement, recalcule a chaque entree.
     * Aucune couche de cout n'est creee ni consommee.
     */
    WEIGHTED_AVERAGE(false),

    /**
     * Premier entre, premier sorti. Chaque entree garde son cout dans une couche, les sorties
     * consomment les couches les plus anciennes.
     */
    FIFO(true);

    private final boolean layered;

    ValuationMethod(boolean layered) {
        this.layered = layered;
    }

    /** Vrai lorsque la methode s'appuie sur des couches de cout. */
    public boolean isLayered() {
        return layered;
    }
}

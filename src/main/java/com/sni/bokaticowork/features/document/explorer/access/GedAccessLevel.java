package com.sni.bokaticowork.features.document.explorer.access;

import java.util.Locale;

/**
 * Le niveau d'acces a un dossier restreint. Les niveaux sont cumulatifs : chacun comprend ceux
 * qui le precedent.
 */
public enum GedAccessLevel {
    /** Aucun : le dossier et son contenu n'existent pas pour cette personne. */
    NONE("aucun acces"),
    /** Voir le dossier et la liste de ses fichiers. */
    READ("voir"),
    /** Ouvrir, telecharger, lire le texte d'un fichier. */
    DOWNLOAD("ouvrir et télécharger"),
    /** Deposer, numeriser, renommer, deplacer, copier, verrouiller. */
    EDIT("modifier"),
    /** Mettre a la corbeille et restaurer. */
    DELETE("supprimer"),
    /** Gerer l'acces : restreindre, ouvrir, donner et retirer des droits. */
    ADMIN("gérer l'accès");

    private final String label;

    GedAccessLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean atLeast(GedAccessLevel other) {
        return ordinal() >= other.ordinal();
    }

    /**
     * Un droit enregistre. {@code REVIEW}, valeur de l'ancienne gestion des permissions, vaut
     * ouverture ; une valeur inconnue ne donne rien.
     */
    public static GedAccessLevel of(String permission) {
        if (permission == null) {
            return NONE;
        }
        String value = permission.trim().toUpperCase(Locale.ROOT);
        if ("REVIEW".equals(value)) {
            return DOWNLOAD;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}

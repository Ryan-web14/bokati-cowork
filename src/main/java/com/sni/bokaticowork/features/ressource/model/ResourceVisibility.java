package com.sni.bokaticowork.features.ressource.model;

/** Lot B10 : qui voit et reserve une ressource au portail. */
public enum ResourceVisibility {
    /** Visible et reservable par tous (comportement d'avant). */
    PUBLIC,
    /** Visible et reservable seulement par l'audience designee ; invisible des autres. */
    RESTRICTED,
    /** Visible de tous ; l'audience designee reserve directement, les autres font une demande approuvee par l'equipe. */
    ON_REQUEST
}

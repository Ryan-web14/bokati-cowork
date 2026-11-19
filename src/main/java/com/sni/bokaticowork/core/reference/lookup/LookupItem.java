package com.sni.bokaticowork.core.reference.lookup;

/**
 * Element d'une recherche de choix (convention du lot L0) : ce qu'une liste deroulante a besoin
 * d'afficher et de renvoyer, rien de plus.
 *
 * <p>Convention des routes : {@code GET /{module}/lookup/{type}?q=&page=&size=&includeInactive=},
 * reponse paginee ({@code PaginatedResponse}) de {@code LookupItem}. Une recherche de choix est
 * ouverte au droit du formulaire qui s'en sert, pas au droit d'administration de la donnee.</p>
 *
 * @param type   nature de l'element quand la recherche en melange plusieurs ({@code MEMBER}, {@code CUSTOMER}...), sinon nul
 * @param code   ce que l'ecran renvoie
 * @param label  ce que l'ecran affiche
 * @param hint   precision affichee en second (courriel, telephone, ville...)
 * @param active faux pour un element desactive, montre seulement avec {@code includeInactive}
 */
public record LookupItem(String type, String code, String label, String hint, boolean active) {
}

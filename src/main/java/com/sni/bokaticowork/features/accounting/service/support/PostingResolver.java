package com.sni.bokaticowork.features.accounting.service.support;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resolveur de variable d'un schema d'ecriture (lot F2) : {@code {NOM}} ou {@code {NOM:chemin}}.
 * Le nom est en majuscules ; l'argument, s'il y en a un, est la valeur du chemin dans l'evenement
 * (ou dans l'element courant d'une ligne repetee).
 *
 * <p>Exemples : {@code {CLIENT_ACCOUNT:customerCode}} donne le compte auxiliaire du client,
 * {@code {REGISTER_JOURNAL:registerCode}} le journal d'une caisse. Un resolveur qui ne trouve rien
 * renvoie {@code null} : l'evenement passe en echec avec un motif lisible.</p>
 */
public interface PostingResolver {

    /** Nom de la variable, en majuscules. */
    String name();

    /** Description affichee dans l'editeur de schemas. */
    String description();

    /**
     * @param argument valeur de l'argument (peut etre nulle)
     * @param event    l'evenement complet
     * @return la valeur, ou {@code null} si elle ne peut pas etre resolue
     */
    String resolve(String argument, JsonNode event);
}

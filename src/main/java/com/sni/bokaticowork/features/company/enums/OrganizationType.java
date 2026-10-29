package com.sni.bokaticowork.features.company.enums;

import java.util.List;

/**
 * Ce qu'est l'organisation cliente · decide des identifiants qu'on lui propose.
 *
 * <p>Seules les societes et les etablissements portaient une fiche : une association, une ONG ou
 * un ministere n'a pas de RCCM, et la fiche l'exigeait. Aucun identifiant n'est obligatoire ;
 * chacun n'est imprime sur la facture que s'il est saisi.</p>
 */
public enum OrganizationType {

    SOCIETE("Société", List.of(IdentifierType.NIU, IdentifierType.RCCM)),
    /** Forme d'entreprise courante au Congo · « Ets », immatricule au RCCM. */
    ETABLISSEMENT("Établissement", List.of(IdentifierType.NIU, IdentifierType.RCCM)),
    ENTREPRENEUR_INDIVIDUEL("Entrepreneur individuel", List.of(IdentifierType.NIU, IdentifierType.RCCM)),
    PROFESSION_LIBERALE("Profession libérale", List.of(IdentifierType.NIU, IdentifierType.ORDRE_PROFESSIONNEL)),
    ASSOCIATION("Association", List.of(IdentifierType.NIU, IdentifierType.RECEPISSE)),
    ONG("ONG", List.of(IdentifierType.NIU, IdentifierType.AGREMENT)),
    INSTITUTION_PUBLIQUE("Institution publique",
            List.of(IdentifierType.NIU, IdentifierType.ACTE_CREATION, IdentifierType.CODE_ADMINISTRATIF));

    private final String label;
    private final List<IdentifierType> suggestedIdentifiers;

    OrganizationType(String label, List<IdentifierType> suggestedIdentifiers) {
        this.label = label;
        this.suggestedIdentifiers = suggestedIdentifiers;
    }

    public String getLabel() {
        return label;
    }

    /** Les identifiants proposes a la saisie · tous facultatifs, {@code AUTRE} toujours possible. */
    public List<IdentifierType> getSuggestedIdentifiers() {
        return suggestedIdentifiers;
    }
}

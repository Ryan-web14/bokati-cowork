package com.sni.bokaticowork.features.company.enums;

/**
 * Les identifiants d'une organisation · chacun avec le libelle imprime sur la facture.
 *
 * <p>Seuls le NIU et le RCCM ont un format verifie ; les autres sont pris tels quels.</p>
 */
public enum IdentifierType {

    NIU("NIU"),
    RCCM("RCCM"),
    ORDRE_PROFESSIONNEL("N° d'inscription à l'ordre"),
    RECEPISSE("N° de récépissé"),
    AGREMENT("N° d'agrément"),
    ACTE_CREATION("Acte de création"),
    CODE_ADMINISTRATIF("Code administratif"),
    /** Ce que la liste ne prevoit pas · le libelle est alors saisi avec la valeur. */
    AUTRE("Identifiant");

    private final String defaultLabel;

    IdentifierType(String defaultLabel) {
        this.defaultLabel = defaultLabel;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }
}

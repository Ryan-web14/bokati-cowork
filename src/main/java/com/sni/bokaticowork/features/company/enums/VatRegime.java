package com.sni.bokaticowork.features.company.enums;

/**
 * Le regime de TVA de l'organisation cliente.
 *
 * <p>{@code EXEMPT} : ses factures sont emises sans TVA ni centime additionnel, et la mention
 * d'exoneration de la fiche y est imprimee.</p>
 */
public enum VatRegime {
    SUBJECT,
    EXEMPT
}

package com.sni.bokaticowork.features.company.enums;

/** Ce qu'un membre est pour l'organisation · decide de ce qu'il voit en son nom. */
public enum MemberLinkRole {
    /** Gerant ou proprietaire · voit le compte de l'organisation et regle ses factures. */
    OWNER,
    /** Collaborateur · ne voit que son propre compte. */
    EMPLOYEE
}

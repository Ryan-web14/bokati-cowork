package com.sni.bokaticowork.features.document.documentMaster.enums;

public enum DocumentOwnerType {
    CUSTOMER,
    MEMBER,
    BUSINESS,
    CONTRACT,
    INVOICE,
    PAYMENT,
    PROPOSAL,
    ASSET,
    /** Document de l'organisation elle-meme (courrier numerise, piece administrative) : aucun proprietaire. */
    INTERNAL
}

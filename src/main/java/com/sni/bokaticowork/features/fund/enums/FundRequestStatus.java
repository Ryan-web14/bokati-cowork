package com.sni.bokaticowork.features.fund.enums;

/** Cycle de vie d'une demande de fonds (plan, 4.5). */
public enum FundRequestStatus {
    DRAFT,
    SUBMITTED,
    IN_APPROVAL,
    APPROVED,
    PARTIALLY_DISBURSED,
    DISBURSED,
    TO_JUSTIFY,
    PARTIALLY_JUSTIFIED,
    JUSTIFIED,
    SETTLED,
    REJECTED,
    CANCELLED,
    EXPIRED
}

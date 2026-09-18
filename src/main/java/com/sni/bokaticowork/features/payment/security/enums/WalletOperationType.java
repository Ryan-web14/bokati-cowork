package com.sni.bokaticowork.features.payment.security.enums;

/**
 * Les operations que la politique de securite peut soumettre au code.
 *
 * <p>{@code TRANSFER} y figure toujours et ne peut en etre retire · c'est la seule operation qui
 * fait sortir de l'argent vers quelqu'un d'autre, sans facture en face et sans annulation possible.
 * C'est aussi, pour cette raison exacte, celle qui interesse un compte vole.</p>
 */
public enum WalletOperationType {
    TRANSFER,
    MERCHANT_PAYMENT,
    BILL_PAYMENT,
    WITHDRAWAL_REFUND,
    PIN_CHANGE
}

package com.sni.bokaticowork.features.payment.enums;

/**
 * Operateur d'un mobile money recu hors de la plateforme, sur le numero marchand.
 *
 * <p>Le code est range dans {@code payment_transaction.provider} ; le libelle est celui imprime sur
 * le recu et la facture.</p>
 */
public enum OfflineMobileMoneyOperator {
    MTN_MOMO("MTN MoMo Pay"),
    AIRTEL_MONEY("Airtel Money");

    private final String label;

    OfflineMobileMoneyOperator(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Le libelle d'un code range en base · le code tel quel s'il n'est pas reconnu. */
    public static String labelOf(String code) {
        if (code == null) {
            return null;
        }
        for (OfflineMobileMoneyOperator operator : values()) {
            if (operator.name().equalsIgnoreCase(code.trim())) {
                return operator.label;
            }
        }
        return code;
    }
}

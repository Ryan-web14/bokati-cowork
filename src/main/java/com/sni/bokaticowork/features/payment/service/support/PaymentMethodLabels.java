package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.OfflineMobileMoneyOperator;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;

/** Le moyen de paiement tel qu'il s'imprime sur un recu, une facture ou un ecran de caisse. */
public final class PaymentMethodLabels {

    private PaymentMethodLabels() {
    }

    public static String label(PaymentMethod method) {
        if (method == null) {
            return "Paiement";
        }
        return switch (method) {
            case CASH -> "Espèces";
            case WALLET -> "Portefeuille";
            case MOBILE_MONEY -> "Mobile money";
            case MOBILE_MONEY_OFFLINE -> "Mobile money hors plateforme";
            case BANK_TRANSFER -> "Virement bancaire";
            case CARD -> "Carte";
            case CHEQUE -> "Chèque";
            case MANUAL_ADJUSTMENT -> "Ajustement manuel";
            case CREDIT_NOTE -> "Avoir";
        };
    }

    /** Avec l'operateur pour un mobile money hors plateforme : « Mobile money hors plateforme (MTN MoMo Pay) ». */
    public static String label(PaymentMethod method, String provider) {
        if (method == PaymentMethod.MOBILE_MONEY_OFFLINE && provider != null && !provider.isBlank()) {
            return label(method) + " (" + OfflineMobileMoneyOperator.labelOf(provider) + ")";
        }
        return label(method);
    }
}

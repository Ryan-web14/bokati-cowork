package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.CashCapBehavior;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Valeurs heritees du type de caisse a sa creation (plan des caisses, sections 4.1 et 18).
 *
 * <p>Ce sont des valeurs initiales : la fiche de la caisse les garde et on les change ensuite
 * caisse par caisse. Le plafond de sortie de 50 000 XAF est celui de la decision 7b du plan.</p>
 */
public final class CashRegisterTypeDefaults {

    public static final BigDecimal DEFAULT_MAX_OUT_PER_OPERATION = new BigDecimal("50000");

    private CashRegisterTypeDefaults() {
    }

    public record Defaults(
            CashCapBehavior capBehavior,
            BigDecimal maxOutPerOperation,
            BigDecimal maxOutPerDay,
            List<PaymentMethod> acceptedMethods,
            List<String> acceptedCurrencies,
            boolean sessions,
            int maxHolders
    ) {
    }

    public static Defaults of(CashRegisterType type) {
        CashRegisterType resolved = type == null ? CashRegisterType.COUNTER : type;
        return switch (resolved) {
            case COUNTER -> new Defaults(CashCapBehavior.ALERT, DEFAULT_MAX_OUT_PER_OPERATION, null,
                    List.of(PaymentMethod.CASH, PaymentMethod.CHEQUE, PaymentMethod.BANK_TRANSFER,
                            PaymentMethod.MOBILE_MONEY_OFFLINE, PaymentMethod.CARD),
                    List.of("XAF"), true, 1);
            case MAIN -> new Defaults(CashCapBehavior.ALERT, null, null,
                    List.of(PaymentMethod.CASH, PaymentMethod.CHEQUE, PaymentMethod.BANK_TRANSFER),
                    List.of("XAF"), true, 1);
            case PETTY_CASH -> new Defaults(CashCapBehavior.BLOCK, DEFAULT_MAX_OUT_PER_OPERATION, null,
                    List.of(PaymentMethod.CASH), List.of("XAF"), true, 1);
            case SAFE -> new Defaults(CashCapBehavior.ALERT, null, null,
                    List.of(PaymentMethod.CASH), List.of("XAF"), false, 2);
            case AUTOMATIC -> new Defaults(null, null, null, List.of(), List.of("XAF"), true, 0);
        };
    }
}

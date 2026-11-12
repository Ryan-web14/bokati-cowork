package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.enums.OfflineMobileMoneyOperator;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Paiements recus hors de la plateforme puis saisis au guichet : cheque, virement, mobile money
 * hors plateforme (MTN MoMo Pay, Airtel Money sur le numero marchand).
 *
 * <p>Rien n'est verifie aupres d'un tiers : la reference saisie est tout ce qui permettra de
 * rapprocher le paiement du releve bancaire ou operateur. Elle est donc obligatoire, et une meme
 * reference ne peut pas etre saisie deux fois pour le meme moyen.</p>
 */
@Component
@RequiredArgsConstructor
public class ManualPaymentDetailsSupport {

    public static final Set<PaymentMethod> OFFLINE_METHODS =
            Set.of(PaymentMethod.CHEQUE, PaymentMethod.BANK_TRANSFER, PaymentMethod.MOBILE_MONEY_OFFLINE);

    private final PaymentTransactionRepository transactionRepository;

    public static boolean isOffline(PaymentMethod method) {
        return method != null && OFFLINE_METHODS.contains(method);
    }

    /**
     * Controle et normalise ce qui a ete saisi.
     *
     * @return le detail a porter sur la transaction ; {@code provider} est l'operateur pour un mobile
     * money hors plateforme, le nom du moyen sinon
     */
    public ManualPaymentDetails validate(PaymentMethod method,
                                         String reference,
                                         String bankName,
                                         String payerName,
                                         String payerPhone,
                                         OfflineMobileMoneyOperator operator) {
        if (!isOffline(method)) {
            throw new BadRequestException("Moyen de paiement hors plateforme attendu : CHEQUE, BANK_TRANSFER ou MOBILE_MONEY_OFFLINE");
        }
        String ref = clean(reference);
        String bank = clean(bankName);
        String payer = clean(payerName);
        String phone = clean(payerPhone);
        String provider = method.name();
        switch (method) {
            case CHEQUE -> {
                if (ref == null) {
                    throw new BadRequestException("Le numéro du chèque est obligatoire");
                }
                if (bank == null) {
                    throw new BadRequestException("La banque du chèque est obligatoire");
                }
            }
            case BANK_TRANSFER -> {
                if (ref == null) {
                    throw new BadRequestException("La référence du virement est obligatoire");
                }
            }
            case MOBILE_MONEY_OFFLINE -> {
                if (operator == null) {
                    throw new BadRequestException("L'opérateur mobile money est obligatoire (MTN_MOMO ou AIRTEL_MONEY)");
                }
                if (ref == null) {
                    throw new BadRequestException("L'identifiant de la transaction chez l'opérateur est obligatoire");
                }
                provider = operator.name();
            }
            default -> throw new IllegalStateException();
        }
        assertNotAlreadyRecorded(method, provider, ref, bank);
        return new ManualPaymentDetails(method, provider, ref, bank, payer,
                method == PaymentMethod.MOBILE_MONEY_OFFLINE ? phone : null);
    }

    private void assertNotAlreadyRecorded(PaymentMethod method, String provider, String reference, String bankName) {
        boolean exists = transactionRepository
                .findAllByPaymentMethodAndProviderAndProviderReferenceIgnoreCaseAndStatus(
                        method, provider, reference, PaymentTransactionStatus.SUCCEEDED)
                .stream()
                // Deux banques peuvent emettre le meme numero de cheque.
                .anyMatch(existing -> method != PaymentMethod.CHEQUE
                        || existing.getBankName() == null
                        || existing.getBankName().equalsIgnoreCase(bankName));
        if (exists) {
            throw new ConflictException(what(method) + " " + reference
                    + " est déjà enregistré" + (method == PaymentMethod.MOBILE_MONEY_OFFLINE ? "e" : ""));
        }
    }

    private static String what(PaymentMethod method) {
        return switch (method) {
            case CHEQUE -> "Le chèque";
            case BANK_TRANSFER -> "Le virement";
            default -> "La transaction mobile money";
        };
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record ManualPaymentDetails(
            PaymentMethod method,
            String provider,
            String reference,
            String bankName,
            String payerName,
            String payerPhone
    ) {
    }
}

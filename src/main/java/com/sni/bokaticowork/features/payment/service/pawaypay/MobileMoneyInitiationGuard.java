package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.phone.PhoneNumbers;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ce qui se verifie avant d'appeler l'operateur · et se dit en francais.
 *
 * <p>Un refus de l'operateur arrive deux a trois secondes plus tard, dans sa langue et avec son
 * vocabulaire. Ce qui peut se savoir ici se dit ici : une devise qui ne correspond pas, un montant
 * impossible, un numero qui n'en est pas un. Le client corrige au lieu d'attendre un echec.</p>
 *
 * <p>Le prefixe par operateur est une donnee, pas une regle du code · les plages changent, et une
 * regle fausse refuserait un paiement valable. Sans configuration, aucun controle de prefixe.</p>
 */
@Slf4j
@Component
public class MobileMoneyInitiationGuard {

    /** {@code MTN_MOMO_COG=06,05;AIRTEL_COG=04,05} · vide, on ne controle rien. */
    @Value("${bokati.payment.pawaypay.operator-prefixes:}")
    private String operatorPrefixes;

    /** Les operateurs actives sur le compte PawaPay · vide, tous ceux de la liste sont acceptes. */
    @Value("${bokati.payment.pawaypay.enabled-providers:}")
    private String enabledProviders;

    /** L'operateur est-il active sur le compte · la page de paiement ne propose que ceux-la. */
    public boolean isEnabled(CongoCorrespondent operator) {
        if (operator == null) {
            return false;
        }
        if (!StringUtils.hasText(enabledProviders)) {
            return true;
        }
        return java.util.Arrays.stream(enabledProviders.split(","))
                .map(String::trim)
                .anyMatch(code -> code.equalsIgnoreCase(operator.name()) || code.equalsIgnoreCase(operator.providerCode()));
    }

    public void check(PaymentIntent intent, InitiateMobileMoneyDepositRequest request, BigDecimal amount) {
        CongoCorrespondent operator = request.correspondent();
        if (operator == null) {
            throw new BadRequestException("Choisissez l'opérateur mobile money.");
        }
        if (!isEnabled(operator)) {
            throw new BadRequestException("Le paiement par " + operator.getDisplayName() + " n'est pas disponible.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Le montant du paiement doit être positif.");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            // L'operateur n'accepte pas les centimes · arrondir en silence changerait le montant du.
            throw new BadRequestException("Le montant doit être un nombre entier de " + intent.getCurrency() + ".");
        }
        if (StringUtils.hasText(intent.getCurrency()) && !intent.getCurrency().equalsIgnoreCase(operator.getCurrency())) {
            throw new BadRequestException("L'opérateur " + operator.getDisplayName() + " encaisse en "
                    + operator.getCurrency() + ", or cette facture est en " + intent.getCurrency() + ".");
        }
        String msisdn = PhoneNumbers.toE164(request.phoneNumber(), operator.dialCode(), operator.keepsTrunkZero())
                .orElseThrow(() -> new BadRequestException("Le numéro de téléphone n'est pas valide pour "
                        + operator.getDisplayName() + "."));
        // Un numero d'un autre pays passait · un +243 avec un operateur du Congo-Brazzaville partait
        // chez l'operateur, qui le refusait apres coup.
        if (operator.dialCode() != null && !msisdn.startsWith(operator.dialCode())) {
            throw new BadRequestException("Ce numéro n'est pas un numéro " + operator.getDisplayName()
                    + " · il doit commencer par " + operator.dialCode() + ".");
        }
        checkPrefix(operator, msisdn);
    }

    /**
     * Le numero appartient-il a une plage connue de cet operateur ?
     *
     * <p>Envoyer un numero MTN a Airtel donne un refus tardif et incomprehensible pour le client.
     * Mais une plage mal recopiee bloquerait des paiements valables · d'ou une liste qui se
     * configure, et qui ne controle rien tant qu'elle est vide.</p>
     */
    private void checkPrefix(CongoCorrespondent operator, String msisdn) {
        List<String> prefixes = prefixes().getOrDefault(operator.name(), List.of());
        if (prefixes.isEmpty()) {
            return;
        }
        String dialCode = operator.dialCode() == null ? "" : operator.dialCode().replace("+", "");
        String local = msisdn.replace("+", "");
        if (local.startsWith(dialCode)) {
            local = local.substring(dialCode.length());
        }
        String national = operator.keepsTrunkZero() || local.startsWith("0") ? local : "0" + local;
        for (String prefix : prefixes) {
            if (national.startsWith(prefix)) {
                return;
            }
        }
        throw new BadRequestException("Ce numéro ne semble pas être un compte " + operator.getDisplayName()
                + ". Vérifiez le numéro, ou choisissez l'opérateur correspondant.");
    }

    private Map<String, List<String>> prefixes() {
        if (!StringUtils.hasText(operatorPrefixes)) {
            return Map.of();
        }
        return Arrays.stream(operatorPrefixes.split(";"))
                .map(String::trim)
                .filter(entry -> entry.contains("="))
                .collect(Collectors.toMap(
                        entry -> entry.substring(0, entry.indexOf('=')).trim().toUpperCase(Locale.ROOT),
                        entry -> Arrays.stream(entry.substring(entry.indexOf('=') + 1).split(","))
                                .map(String::trim).filter(StringUtils::hasText).toList(),
                        (first, second) -> first));
    }
}

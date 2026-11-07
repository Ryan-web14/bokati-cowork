package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingCatalog;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingCatalog.Definition;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingCatalog.Effect;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingCatalog.Type;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingProvider;
import com.sni.bokaticowork.core.settings.platform.SettingDomain;
import com.sni.bokaticowork.features.subscription.lifecycle.model.EarlyTerminationFormula;
import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * La politique d'abonnement dans les parametres · elle garde sa table et ses regles de controle,
 * elle rejoint l'ecran, les droits et le journal.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionPolicySettings implements PlatformSettingProvider {

    private static final String PREFIX = "subscription.policy.";
    private static final String PRORATION = PREFIX + "proration-policy";
    private static final String NOTICE = PREFIX + "default-notice-days";
    private static final String TERMINATION_FORMULA = PREFIX + "early-termination-formula";
    private static final String TERMINATION_PERCENT = PREFIX + "early-termination-percent";
    private static final String TERMINATION_FEE = PREFIX + "early-termination-fixed-fee";
    private static final String FREEZE_PER_YEAR = PREFIX + "freeze-max-per-year";
    private static final String FREEZE_DAYS = PREFIX + "freeze-max-days";
    private static final String FREEZE_NOTICE = PREFIX + "freeze-notice-days";
    private static final String FREEZE_FEE = PREFIX + "freeze-fee-percent";
    private static final String GRACE = PREFIX + "grace-period-days";
    private static final String SUSPENSION = PREFIX + "suspension-after-grace-days";
    private static final String RENEWAL_EXPIRY = PREFIX + "renewal-expiry-days";
    private static final String QUOTE_VALIDITY = PREFIX + "quote-validity-days";

    private record Field(String key, Type type, String label, Function<SubscriptionPolicy, Object> reader, List<String> options) {
    }

    private static final List<Field> FIELDS = List.of(
            new Field(PRORATION, Type.ENUM, "Calcul du prorata", SubscriptionPolicy::getProrationPolicy, names(ProrationPolicy.values())),
            new Field(NOTICE, Type.INTEGER, "Préavis de résiliation (jours)", SubscriptionPolicy::getDefaultNoticeDays, List.of()),
            new Field(TERMINATION_FORMULA, Type.ENUM, "Formule de rupture d'engagement", SubscriptionPolicy::getEarlyTerminationFormula, names(EarlyTerminationFormula.values())),
            new Field(TERMINATION_PERCENT, Type.DECIMAL, "Pénalité de rupture (%)", SubscriptionPolicy::getEarlyTerminationPercent, List.of()),
            new Field(TERMINATION_FEE, Type.DECIMAL, "Pénalité de rupture fixe", SubscriptionPolicy::getEarlyTerminationFixedFee, List.of()),
            new Field(FREEZE_PER_YEAR, Type.INTEGER, "Gels par an", SubscriptionPolicy::getFreezeMaxPerYear, List.of()),
            new Field(FREEZE_DAYS, Type.INTEGER, "Durée maximale d'un gel (jours)", SubscriptionPolicy::getFreezeMaxDays, List.of()),
            new Field(FREEZE_NOTICE, Type.INTEGER, "Préavis de gel (jours)", SubscriptionPolicy::getFreezeNoticeDays, List.of()),
            new Field(FREEZE_FEE, Type.DECIMAL, "Part facturée pendant un gel (%)", SubscriptionPolicy::getFreezeFeePercent, List.of()),
            new Field(GRACE, Type.INTEGER, "Tolérance après échéance (jours)", SubscriptionPolicy::getGracePeriodDays, List.of()),
            new Field(SUSPENSION, Type.INTEGER, "Suspension après la tolérance (jours)", SubscriptionPolicy::getSuspensionAfterGraceDays, List.of()),
            new Field(RENEWAL_EXPIRY, Type.INTEGER, "Expiration après la fin de période (jours)", SubscriptionPolicy::getRenewalExpiryDays, List.of()),
            new Field(QUOTE_VALIDITY, Type.INTEGER, "Validité d'un devis (jours)", SubscriptionPolicy::getQuoteValidityDays, List.of()));

    private final SubscriptionPolicyService policyService;

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    @Override
    public List<ProvidedSetting> settings() {
        SubscriptionPolicy current = policyService.current();
        SubscriptionPolicy defaults = SubscriptionPolicy.builder().build();
        return FIELDS.stream()
                .map(field -> new ProvidedSetting(
                        new Definition(field.key(), SettingDomain.SUBSCRIPTIONS, field.type(), Effect.LIVE,
                                text(field.reader().apply(defaults)), field.label()),
                        text(field.reader().apply(current)), field.options()))
                .toList();
    }

    @Override
    public void update(String key, String value) {
        Field field = FIELDS.stream().filter(candidate -> candidate.key().equals(key)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Paramètre inconnu : " + key));
        policyService.update(new SubscriptionPolicyService.Update(
                PRORATION.equals(key) ? choice(ProrationPolicy.class, field, value) : null,
                NOTICE.equals(key) ? integer(field, value) : null,
                TERMINATION_FORMULA.equals(key) ? choice(EarlyTerminationFormula.class, field, value) : null,
                TERMINATION_PERCENT.equals(key) ? decimal(field, value) : null,
                TERMINATION_FEE.equals(key) ? fixedFee(field, value) : null,
                FREEZE_PER_YEAR.equals(key) ? integer(field, value) : null,
                FREEZE_DAYS.equals(key) ? integer(field, value) : null,
                FREEZE_NOTICE.equals(key) ? integer(field, value) : null,
                FREEZE_FEE.equals(key) ? decimal(field, value) : null,
                GRACE.equals(key) ? integer(field, value) : null,
                SUSPENSION.equals(key) ? integer(field, value) : null,
                RENEWAL_EXPIRY.equals(key) ? integer(field, value) : null,
                QUOTE_VALIDITY.equals(key) ? integer(field, value) : null));
    }

    private static String text(Object value) {
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }
        return value == null ? "" : value.toString();
    }

    private static int integer(Field field, String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("« " + field.label() + " » doit être un nombre entier.");
        }
    }

    private static BigDecimal decimal(Field field, String value) {
        try {
            return new BigDecimal(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("« " + field.label() + " » doit être un nombre.");
        }
    }

    /** Vide, il n'y a pas de montant fixe · la politique le lit comme un montant negatif. */
    private static BigDecimal fixedFee(Field field, String value) {
        return StringUtils.hasText(value) ? decimal(field, value) : BigDecimal.ONE.negate();
    }

    private static <E extends Enum<E>> E choice(Class<E> type, Field field, String value) {
        try {
            return Enum.valueOf(type, value == null ? "" : value.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("« " + field.label() + " » doit valoir " + String.join(", ", field.options()) + ".");
        }
    }
}

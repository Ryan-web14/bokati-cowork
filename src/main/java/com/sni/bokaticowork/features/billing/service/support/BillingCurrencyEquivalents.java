package com.sni.bokaticowork.features.billing.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.service.implementation.ExchangeRateService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Montants d'une facture en devises etrangeres (USD, EUR par defaut), a titre indicatif.
 *
 * <p>Les taux sont ceux de la comptabilite (taux du jour recuperes ou saisis ; l'euro a sa parite
 * fixe). A l'emission (scellement), les equivalents sont figes sur la piece : le montant affiche ne
 * change plus avec les taux. Avant l'emission, ils sont calcules au taux du jour et marques
 * indicatifs. Devises : reglage {@code billing.invoice.equivalent-currencies}.</p>
 */
@Slf4j
@Component
public class BillingCurrencyEquivalents {

    public static final String CURRENCIES_KEY = "billing.invoice.equivalent-currencies";

    /** Un equivalent : taux (unites de la devise de la piece pour 1 unite etrangere), total et reste du. */
    public record Equivalent(String currency, BigDecimal rate, BigDecimal totalAmount, BigDecimal balanceDue,
                             LocalDate rateDate, boolean frozen) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private ExchangeRateService rates;
    private PlatformSettings settings;

    @Autowired(required = false)
    void setOptional(@Lazy ExchangeRateService rates, PlatformSettings settings) {
        this.rates = rates;
        this.settings = settings;
    }

    public List<String> currencies() {
        String value = settings == null ? "USD,EUR" : settings.text(CURRENCIES_KEY, "USD,EUR");
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).map(code -> code.toUpperCase(Locale.ROOT))
                .filter(code -> code.matches("[A-Z]{3}")).distinct().toList();
    }

    /** Equivalents au taux d'une date ; une devise sans taux est omise. */
    public List<Equivalent> compute(BigDecimal total, BigDecimal balanceDue, String currency, LocalDate date, boolean frozen) {
        List<Equivalent> result = new ArrayList<>();
        if (rates == null || total == null) {
            return result;
        }
        String base = StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "XAF";
        LocalDate day = date == null ? LocalDate.now() : date;
        for (String target : currencies()) {
            if (target.equals(base)) {
                continue;
            }
            try {
                BigDecimal amount = rates.convert(total, base, target, day).setScale(2, RoundingMode.HALF_UP);
                BigDecimal due = balanceDue == null ? null : rates.convert(balanceDue, base, target, day).setScale(2, RoundingMode.HALF_UP);
                BigDecimal unit = rates.convert(BigDecimal.ONE, target, base, day).setScale(4, RoundingMode.HALF_UP);
                result.add(new Equivalent(target, unit, amount, due, day, frozen));
            } catch (RuntimeException ex) {
                log.debug("Pas de taux {} au {} : equivalent omis", target, day);
            }
        }
        return result;
    }

    /** Fige les equivalents sur la piece a son emission (taux de la date d'emission). */
    public void freeze(BillingDocument document) {
        try {
            LocalDate day = document.getIssueDate() == null ? LocalDate.now() : document.getIssueDate();
            List<Equivalent> equivalents = compute(document.getTotalAmount(), null, document.getCurrency(), day, true);
            document.setCurrencyEquivalentsJson(equivalents.isEmpty() ? null : objectMapper.writeValueAsString(equivalents));
        } catch (Exception ex) {
            log.warn("Equivalents en devises de {} non figes : {}", document.getDocumentNumber(), ex.getMessage());
        }
    }

    /** Equivalents d'une piece : figes s'ils existent (reste du recalcule au meme taux), sinon au taux du jour. */
    public List<Equivalent> forDocument(BillingDocument document) {
        if (StringUtils.hasText(document.getCurrencyEquivalentsJson())) {
            try {
                List<Equivalent> frozen = objectMapper.readValue(document.getCurrencyEquivalentsJson(), new TypeReference<>() {
                });
                BigDecimal due = document.getBalanceDue();
                return frozen.stream().map(eq -> new Equivalent(eq.currency(), eq.rate(), eq.totalAmount(),
                        due == null || eq.rate() == null || eq.rate().signum() == 0 ? null
                                : due.divide(eq.rate(), 2, RoundingMode.HALF_UP), eq.rateDate(), true)).toList();
            } catch (Exception ex) {
                log.warn("Equivalents figes de {} illisibles : {}", document.getDocumentNumber(), ex.getMessage());
            }
        }
        return compute(document.getTotalAmount(), document.getBalanceDue(), document.getCurrency(), LocalDate.now(), false);
    }
}

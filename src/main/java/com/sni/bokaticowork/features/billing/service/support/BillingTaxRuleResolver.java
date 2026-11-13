package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.model.TaxRule;
import com.sni.bokaticowork.features.billing.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BillingTaxRuleResolver {

    private static final String DEFAULT_COUNTRY_CODE = "CG";
    private static final String VAT = "VAT";
    private static final String ADDITIONAL_CENT = "ADDITIONAL_CENT";

    private final TaxRuleRepository taxRuleRepository;

    public TaxProfile defaultTaxProfile() {
        List<TaxRule> rules = taxRuleRepository.findActiveRules(DEFAULT_COUNTRY_CODE, LocalDate.now());
        BigDecimal vatRate = rules.stream()
                .filter(rule -> VAT.equalsIgnoreCase(rule.getTaxType()))
                .findFirst()
                .map(TaxRule::getRate)
                .orElse(BigDecimal.ZERO);
        BigDecimal additionalCentRate = rules.stream()
                .filter(rule -> ADDITIONAL_CENT.equalsIgnoreCase(rule.getTaxType()))
                .findFirst()
                .map(TaxRule::getRate)
                .orElse(BigDecimal.ZERO);
        return new TaxProfile(vatRate, additionalCentRate);
    }

    /**
     * Lot C1 : version d'une regle applicable a une date, par code de base (celui de l'article)
     * ou par code de version. Vide si le code est inconnu ou sans version en vigueur.
     */
    public java.util.Optional<TaxRule> ruleFor(String code, LocalDate date) {
        if (code == null || code.isBlank()) {
            return java.util.Optional.empty();
        }
        String wanted = code.trim();
        List<TaxRule> versions = taxRuleRepository.findVersions(wanted);
        if (versions.isEmpty()) {
            versions = taxRuleRepository.findByCode(wanted).map(rule -> taxRuleRepository.findVersions(rule.getBaseCode()))
                    .orElse(List.of());
        }
        return versions.stream()
                .filter(rule -> rule.validOn(date))
                .max(java.util.Comparator.comparing(rule -> rule.getValidFrom() == null ? LocalDate.MIN : rule.getValidFrom()));
    }

    /** Exoneree ou hors champ : ni TVA ni centimes additionnels. */
    public static boolean zeroRated(TaxRule rule) {
        return rule != null && (rule.getTreatment() == com.sni.bokaticowork.features.billing.enums.TaxTreatment.EXEMPT
                || rule.getTreatment() == com.sni.bokaticowork.features.billing.enums.TaxTreatment.OUT_OF_SCOPE);
    }

    public record TaxProfile(BigDecimal vatRate, BigDecimal additionalCentRate) {
    }
}
